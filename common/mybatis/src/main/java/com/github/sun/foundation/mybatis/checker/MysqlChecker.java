package com.github.sun.foundation.mybatis.checker;

import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Scanner;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.PrependStringBuilder;
import com.github.sun.foundation.boot.utility.Tuple;
import com.github.sun.foundation.modelling.Converter;
import com.github.sun.foundation.modelling.Model;
import com.github.sun.foundation.mybatis.SqlSessionMeta;
import com.github.sun.foundation.mybatis.interceptor.ResultMapInterceptor;
import com.github.sun.foundation.mybatis.query.IndexParser;
import com.github.sun.foundation.sql.ConnectionManager;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.foundation.sql.factory.SqlBuilderFactory;
import com.github.sun.foundation.sql.spi.AbstractSqlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;

import javax.persistence.Column;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.JDBCType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MysqlChecker implements Lifecycle {
  private final SqlBuilder.Factory factory = SqlBuilderFactory.mysql();

  private Map<String, String> tableCollations;
  private Map<String, Map<String, List<String>>> tableIndexes;
  private Map<String, Map<String, Field>> tableFields;

  private String check(List<TableDef> tables, ConnectionManager connManager) {
    StringBuilder info = new StringBuilder();
    tables.forEach(def -> check(info, def, connManager));
    return info.toString();
  }

  private void check(StringBuilder info, TableDef def, ConnectionManager connectionManager) {
    connectionManager.withContext(() -> {
      if (checkTable(info, def, connectionManager)) {
        checkTableFields(info, def, connectionManager);
        // 检查内索引
        checkIndexes(info, def, connectionManager);
      }
    });
  }

  private boolean checkTable(StringBuilder info, TableDef def, ConnectionManager connectionManager) {
    if (tableCollations == null) {
      // collect table collations
      tableCollations = new HashMap<>();
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from("INFORMATION_SCHEMA.TABLES").where(sb.field("TABLE_SCHEMA").eq(def.schema)).select(sb.field("TABLE_NAME")).select(sb.field("TABLE_COLLATION")).template();
      connectionManager.fetch(template, rs -> {
        while (rs.next()) {
          tableCollations.put(rs.getString(1), rs.getString(2));
        }
      });
    }
    String collation = tableCollations.get(def.model.tableName());
    if (collation == null) {
      String sql = buildDDL(def);
      info.append(String.format("\n-- 缺少数据表: %s 请执行SQL语句以修正该问题:\n%s;\n", def.model.tableName(), sql));
      return false;
    }
    return true;
  }

  private void checkTableFields(StringBuilder info, TableDef def, ConnectionManager connectionManager) {
    if (tableFields == null) {
      tableFields = new HashMap<>();
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from("INFORMATION_SCHEMA.COLUMNS").where(sb.field("TABLE_SCHEMA").eq(def.schema)).select(sb.field("TABLE_NAME")).select(sb.field("COLUMN_NAME")).select(sb.field("IS_NULLABLE")).select(sb.field("DATA_TYPE")).select(sb.field("COLUMN_TYPE")).select(sb.field("COLUMN_DEFAULT")).select(sb.field("GENERATION_EXPRESSION")).template();
      connectionManager.fetch(template, rs -> {
        while (rs.next()) {
          String tableName = rs.getString("TABLE_NAME");
          Map<String, Field> cols = tableFields.computeIfAbsent(tableName, x -> new HashMap<>());
          String col = rs.getString("COLUMN_NAME");
          cols.put(col, new Field(rs.getString("DATA_TYPE"), rs.getString("COLUMN_TYPE"), !"NO".equals(rs.getString("IS_NULLABLE")), rs.getString("COLUMN_DEFAULT") != null, !rs.getString("GENERATION_EXPRESSION").isEmpty()));
        }
      });
    }
    String table = def.model.tableName();
    Map<String, Field> fields = tableFields.get(table);
    Set<String> existColumns = fields.keySet();
    existColumns.removeAll(Arrays.asList(def.model.column("createTime"), def.model.column("updateTime")));
    Set<String> columns = def.model.persistenceProperties().stream().map(Model.Property::column).collect(Collectors.toSet());
    Set<String> missing = columns.stream().filter(v -> !existColumns.contains(v)).collect(Collectors.toSet());
    Set<String> redundant = existColumns.stream().filter(v -> !columns.contains(v)).collect(Collectors.toSet());
    if (!missing.isEmpty()) {
      info.append(String.format("\n-- 数据表'%s'缺少字段: (%s)\n", table, Iterators.mkString(missing, ", ")));
    }
    if (!redundant.isEmpty()) {
      String sql = Iterators.mkString(redundant, "\n", c -> "ALTER TABLE " + table + " DROP COLUMN " + c + ";");
      info.append(String.format("\n-- 数据表'%s'存在多余字段 (%s) 请执行以下语句修改该问题:\n%s\n", table, Iterators.mkString(redundant, ", "), sql));
    }
    existColumns.removeAll(missing);
    existColumns.removeAll(redundant);
    existColumns.forEach(c -> {
      Field field = fields.get(c);
      Model.Property property = def.model.persistenceProperties().stream().filter(p -> p.column().equalsIgnoreCase(c)).findFirst().orElseThrow(IllegalStateException::new);
      JDBCType jdbcType;
      int length = 0;
      switch (property.kind()) {
        case NUMBER:
          Type javaType = property.javaType();
          if (javaType == int.class || javaType == Integer.class) {
            jdbcType = JDBCType.INTEGER;
          } else if (javaType == long.class || javaType == Long.class) {
            jdbcType = JDBCType.BIGINT;
          } else if (javaType == float.class || javaType == double.class || javaType == Double.class || javaType == Float.class) {
            jdbcType = JDBCType.DOUBLE;
          } else if (javaType == BigDecimal.class) {
            jdbcType = JDBCType.DECIMAL;
          } else if (javaType == BigInteger.class) {
            jdbcType = JDBCType.DECIMAL;
          } else {
            throw new IllegalStateException("unsupported java type: " + def.model.name() + "." + property.name());
          }
          break;
        case BOOLEAN:
          jdbcType = JDBCType.BOOLEAN;
          break;
        case TEXT:
          Column a = property.field().getAnnotation(Column.class);
          length = a == null ? 255 : a.length();
          jdbcType = JDBCType.VARCHAR;
          break;
        case ENUM:
          length = 10;
          jdbcType = JDBCType.VARCHAR;
          break;
        case DATE:
          jdbcType = JDBCType.TIMESTAMP;
          break;
        case OBJECT:
        case ARRAY:
          if (!property.hasAnnotation(Converter.class)) {
            throw new IllegalStateException("missing @Converter: " + def.model.name() + "." + property.name() + "." + property.kind());
          }
          jdbcType = JDBCType.JAVA_OBJECT;
          break;
        default:
          throw new IllegalStateException("unsupported column type: " + def.model.name() + "." + property.name() + "." + property.kind());
      }
      Tuple.Tuple2<String, List<String>> types = preciseTypes(jdbcType, length);
      if (types._2.stream().noneMatch(v -> v.equalsIgnoreCase(field.type))) {
        if (types._1.equalsIgnoreCase("JSON")) {
          String sql = "ALTER TABLE " + table + " CHANGE COLUMN " + c + " " + c + " JSON;";
          info.append(String.format("\n-- 字段'%s.%s'类型不正确,期望%s,实际是%s 请执行以下语句修复该问题:\n%s\n", table, c, types._1.toLowerCase(), field.type, sql));
        } else {
          info.append(String.format("\n-- 字段'%s.%s'类型不正确,期望%s,实际是%s\n", table, c, types._1.toLowerCase(), field.type));
        }
      }
      boolean notNull = property.hasAnnotation(NotNull.class) || property.hasAnnotation(NotEmpty.class);
      if (notNull && field.nullable && !field.defaultValue && !field.generated) {
        String sql = "ALTER TABLE " + table + " modify " + c + " " + field.columnType + " NOT NULL;";
        info.append(String.format("\n-- 字段'%s.%s'，不能允许为NULL 请执行以下sql修复该问题:\n%s\n", table, c, sql));
      }
    });
  }

  private Tuple.Tuple2<String, List<String>> preciseTypes(JDBCType type, int length) {
    switch (type) {
      case TIMESTAMP:
      case BOOLEAN:
      case BIGINT:
      case JAVA_OBJECT:
        String tp = type(type, length);
        return Tuple.of(tp, Collections.singletonList(tp));
      case INTEGER:
        return Tuple.of(type(JDBCType.INTEGER, length), Arrays.asList(type(JDBCType.SMALLINT, length), type(JDBCType.TINYINT, length), type(JDBCType.INTEGER, length)));
      case DOUBLE:
      case DECIMAL:
        tp = type(JDBCType.DECIMAL, length);
        return Tuple.of(tp, Collections.singletonList(tp));
      case VARCHAR:
        return Tuple.of(type(JDBCType.VARCHAR, length), Arrays.asList(type(JDBCType.CHAR, length), type(JDBCType.VARCHAR, length), type(JDBCType.NCHAR, length), type(JDBCType.NVARCHAR, length), type(JDBCType.LONGVARCHAR, length), type(JDBCType.LONGNVARCHAR, length)));
      default:
        throw new IllegalStateException("unsupported jdbc type: " + type.getName());
    }
  }

  private String type(JDBCType type, int length) {
    switch (type) {
      case BLOB:
      case CLOB:
      case TINYINT:
      case SMALLINT:
      case BIGINT:
      case DECIMAL:
      case BIT:
        return type.getName();
      case INTEGER:
        return "INT";
      case CHAR:
      case VARCHAR:
      case NCHAR:
      case NVARCHAR:
        if (length < AbstractSqlBuilder.JDBC_TEXT_LENGTH) {
          return type.getName();
        }
        // make it as TEXT/LONGTEXT if too long
      case LONGVARCHAR:
      case LONGNVARCHAR:
        if (length <= AbstractSqlBuilder.JDBC_TEXT_LENGTH) {
          return "TEXT";
        } else {
          return "LONGTEXT";
        }
      case DATE:
      case TIMESTAMP:
        return "DATETIME";
      case BOOLEAN:
        return "TINYINT";
      case JAVA_OBJECT:
        return "JSON";
      default:
        throw new IllegalArgumentException();
    }
  }

  private void checkIndexes(StringBuilder info, TableDef def, ConnectionManager connectionManager) {
    if (tableIndexes == null) {
      tableIndexes = new HashMap<>();
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from("INFORMATION_SCHEMA.STATISTICS").where(sb.field("TABLE_SCHEMA").eq(def.schema)).asc("TABLE_NAME").asc("INDEX_NAME").asc("SEQ_IN_INDEX").select(sb.field("TABLE_NAME")).select(sb.field("INDEX_NAME")).select(sb.field("COLUMN_NAME")).template();
      connectionManager.fetch(template, rs -> {
        while (rs.next()) {
          String tableName = rs.getString("TABLE_NAME");
          String idx = rs.getString("INDEX_NAME");
          String col = rs.getString("COLUMN_NAME");
          Map<String, List<String>> indexMap = tableIndexes.computeIfAbsent(tableName, x -> new HashMap<>());
          List<String> cols = indexMap.computeIfAbsent(idx, x -> new ArrayList<>());
          cols.add(col);
        }
      });
    }
    String table = def.model.tableName();
    Map<String, List<String>> indexMap = tableIndexes.get(table);
    def.indexes.forEach(index -> {
      if (new HashSet<>(index.keys).containsAll(def.primaryKey())) {
        return;
      }
      List<String> actual = indexMap == null ? null : indexMap.get(index.name);
      if (actual == null && !def.indexes.isEmpty()) {
        String sql = "ALTER TABLE " + table + " ADD INDEX " + index.name + " (" + Iterators.mkString(index.keys, ", ") + ");";
        info.append(String.format("\n-- 数据表'%s'缺少索引 '%s' 请执行以下语句修正该问题:\n%s\n", table, index.name, sql));
      } else {
        if (!index.keys.equals(actual)) {
          info.append(String.format("\n-- 索引'%s.%s'字段不正确,期望%s,实际是%s\n", table, index.name, Iterators.mkString(index.keys, "(", ", ", ")"), Iterators.mkString(actual, "(", ", ", ")")));
        }
      }
    });
  }

  @Override
  public void startup() {
    new Thread(() -> {
      PrependStringBuilder sb = new PrependStringBuilder();
      while (SqlSessionMeta.collector.isEmpty()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          throw new RuntimeException(ex);
        }
      }
      SqlSessionMeta.collector.values().forEach(meta -> {
        Collection<Class<?>> mappers = Scanner.getClassesWithAnnotation(Mapper.class).stream().map(Scanner.ClassTag::runtimeClass).collect(Collectors.toList());
        List<TableDef> tables = mappers.stream().map(mapperClass -> {
          Class<?> entityClass = ResultMapInterceptor.findEntityClass(mapperClass);
          if (entityClass != null) {
            Model model = Model.from(entityClass);
            String name = model.tableName();
            if (name != null) {
              List<TableDef.Index> indices = Stream.of(mapperClass.getDeclaredMethods()).map(m -> {
                List<String> keys = IndexParser.parseKeyName(m.getName());
                if (!keys.isEmpty()) {
                  StringBuilder s = new StringBuilder(model.tableName()).append("_idx");
                  keys.forEach(k -> s.append("_").append(k));
                  return new TableDef.Index(s.toString(), keys);
                }
                return null;
              }).filter(Objects::nonNull).collect(Collectors.toList());
              Set<String> idxSet = new HashSet<>();
              indices.forEach(idx -> idxSet.add(idx.name));
              indices.removeIf(idx -> idxSet.stream().anyMatch(v -> v.length() > idx.name.length() && v.startsWith(idx.name)));
              return new TableDef(meta.database(), model, indices.stream().distinct().collect(Collectors.toList()));
            }
          }
          return null;
        }).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        ConnectionManager connectionManager = ConnectionManager.build(meta.dataSource());
        if (!tables.isEmpty()) {
          sb.append(check(tables, connectionManager));
        }
      });
      if (sb.length() > 0) {
        sb.prepend("\n\n--------------------------------------------- 数据表检查 -------------------------------------------\n");
        sb.append("\n--------------------------------------------------------------------------------------------------------");
        log.info("表结构存在错误请先修复" + sb);
      }
    }).start();
  }

  @Override
  public void shutdown() {
  }

  private String buildDDL(TableDef def) {
    SqlBuilder.StatelessSqlBuilder sb = factory.createStatelessBuilder();
    sb.createTable(def.model.tableName()).primaryKey(def.primaryKey());
    Set<String> keys = new HashSet<>(def.primaryKey());
    for (TableDef.Index index : def.indexes) {
      keys.addAll(index.keys);
      if (index.keys.size() > 1) {
        sb.index(index.name, index.keys);
      }
    }
    Set<String> columns = new HashSet<>();
    def.model.persistenceProperties().forEach(p -> {
      columns.add(p.column());
      boolean notNull = keys.contains(p.column()) || p.hasAnnotation(NotNull.class) || p.hasAnnotation(NotEmpty.class);
      switch (p.kind()) {
        case NUMBER:
          Type javaType = p.javaType();
          if (javaType == null) {
            throw new IllegalStateException("missing java type: " + def.model.name() + "." + p.name());
          }
          if (javaType == int.class || javaType == Integer.class) {
            sb.column(p.column(), JDBCType.INTEGER, 0, 0, notNull);
          } else if (javaType == long.class || javaType == Long.class) {
            sb.column(p.column(), JDBCType.BIGINT, 0, 0, notNull);
          } else if (javaType == float.class || javaType == double.class || javaType == Double.class || javaType == Float.class) {
            sb.column(p.column(), JDBCType.DOUBLE, 0, 0, notNull);
          } else if (javaType == BigDecimal.class) {
            Column c = p.field().getAnnotation(Column.class);
            int scale = c != null ? c.scale() : 2;
            sb.column(p.column(), JDBCType.DECIMAL, 18, scale, notNull);
          } else if (javaType == BigInteger.class) {
            sb.column(p.column(), JDBCType.DECIMAL, 32, 0, notNull);
          } else {
            throw new IllegalStateException("unsupported java type: " + def.model.name() + "." + p.name());
          }
          break;
        case BOOLEAN:
          sb.column(p.column(), JDBCType.BOOLEAN, 0, 0, true);
          break;
        case TEXT:
          Column c = p.field().getAnnotation(Column.class);
          boolean varchar = c != null && c.length() <= 255;
          int length = c != null ? c.length() : 255;
          sb.column(p.column(), varchar ? JDBCType.VARCHAR : JDBCType.NVARCHAR, length, 0, notNull);
          break;
        case ENUM:
          sb.column(p.column(), JDBCType.VARCHAR, 20, 0, true);
          break;
        case DATE:
          sb.column(p.column(), JDBCType.TIMESTAMP, 0, 0, notNull);
          break;
        case OBJECT:
        case ARRAY:
          sb.column(p.column(), JDBCType.JAVA_OBJECT, 0, 0, notNull);
          break;
        default:
          throw new IllegalStateException("unsupported column type: " + def.model.name() + "." + p.name() + "." + p.kind());
      }
    });
    if (!columns.contains(def.model.column("createTime"))) {
      sb.column(def.model.column("createTime"), JDBCType.TIMESTAMP, true, "DEFAULT CURRENT_TIMESTAMP");
    }
    if (!columns.contains(def.model.column("updateTime"))) {
      sb.column(def.model.column("updateTime"), JDBCType.TIMESTAMP, true, "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
    }

    return sb.template().parameterizedSQL();
  }

  private static class Field {
    private final String type;
    private final String columnType;
    private final boolean nullable;
    private final boolean defaultValue;
    private final boolean generated;

    private Field(String type, String columnType, boolean nullable, boolean defaultValue, boolean generated) {
      this.type = type;
      this.columnType = columnType;
      this.nullable = nullable;
      this.defaultValue = defaultValue;
      this.generated = generated;
    }
  }
}
