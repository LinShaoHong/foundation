package com.github.sun.foundation.mybatis.interceptor.utility;

import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Scanner;
import com.github.sun.foundation.boot.utility.Cache;
import com.github.sun.foundation.boot.utility.Packages;
import com.github.sun.foundation.boot.utility.PrependStringBuilder;
import com.github.sun.foundation.modelling.Converter;
import com.github.sun.foundation.sql.DBType;
import org.apache.ibatis.javassist.*;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author LinSH
 * @Date: 3:17 下午 2019-12-03
 */
@SuppressWarnings("unchecked")
public class TypeHandlerParser implements Converter.Parser, Lifecycle {
  private static final Cache<Class<? extends Converter.Handler>, Class<?>> cache = new Cache<>();
  private static final String PACKAGE = Packages.group(TypeHandlerParser.class) + ".foundation.mybatis.handlers";

  @Override
  public Class<?> parse(Class<? extends Converter.Handler> handlerClass) {
    return cache.get(handlerClass, () -> {
      try {
        ClassPool pool = ClassPool.getDefault();
        pool.importPackage(TypeHandlerParser.class.getName());
        pool.importPackage(CallableStatement.class.getName());
        pool.importPackage(PreparedStatement.class.getName());
        pool.importPackage(ResultSet.class.getName());
        pool.importPackage(SQLException.class.getName());
        pool.importPackage(JdbcType.class.getName());

        CtClass cc = pool.makeClass(PACKAGE + "." + getSimpleName(handlerClass));
        cc.setSuperclass(pool.get(BaseTypeHandler.class.getName()));
        String handlerName = "\"" + handlerClass.getName() + "\"";
        cc.addMethod(CtMethod.make("public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {" +
          "TypeHandlerParser.Adapter.setNonNullParameter(ps, i, parameter, jdbcType, " + handlerName + ");" +
          "}", cc));
        cc.addMethod(CtMethod.make("public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {" +
          "return TypeHandlerParser.Adapter.getNullableResult(rs, columnName, " + handlerName + ");" +
          "}", cc));
        cc.addMethod(CtMethod.make("public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {" +
          "return TypeHandlerParser.Adapter.getNullableResult(rs, columnIndex, " + handlerName + ");" +
          "}", cc));
        cc.addMethod(CtMethod.make("public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {" +
          "return TypeHandlerParser.Adapter.getNullableResult(cs, columnIndex, " + handlerName + ");" +
          "}", cc));
        return cc.toClass();
      } catch (CannotCompileException | NotFoundException ex) {
        throw new RuntimeException(ex);
      }
    });
  }

  private String getSimpleName(Class<?> clazz) {
    PrependStringBuilder sb = new PrependStringBuilder();
    while (true) {
      sb.prepend(clazz.getSimpleName());
      clazz = clazz.getEnclosingClass();
      if (clazz == null) {
        break;
      }
      sb.prepend(".");
    }
    return sb.toString();
  }

  @SuppressWarnings("Duplicates")
  public static class Adapter {
    private static final Cache<String, Converter.Handler> cache = new Cache<>();

    public static void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType, String handlerClassName) throws SQLException {
      switch (DBType.get()) {
        case PG:
          PGobject value = new PGobject();
          value.setType("jsonb");
          Object obj = getHandler(handlerClassName).serialize(parameter);
          value.setValue(obj == null ? null : obj.toString());
          ps.setObject(i, value);
          break;
        case MYSQL:
          ps.setObject(i, getHandler(handlerClassName).serialize(parameter));
          break;
        default:
          throw new IllegalArgumentException("Unknown db type: '" + DBType.get() + "'");
      }
    }

    public static Object getNullableResult(ResultSet rs, String columnName, String handlerClassName) throws SQLException {
      switch (DBType.get()) {
        case PG:
          PGobject value = (PGobject) rs.getObject(columnName);
          return getHandler(handlerClassName).deserialize(value == null ? null : value.getValue());
        case MYSQL:
          Object obj = rs.getObject(columnName);
          return getHandler(handlerClassName).deserialize(obj);
        default:
          throw new IllegalArgumentException("Unknown db type: '" + DBType.get() + "'");
      }
    }

    public static Object getNullableResult(ResultSet rs, int columnIndex, String handlerClassName) throws SQLException {
      switch (DBType.get()) {
        case PG:
          PGobject value = (PGobject) rs.getObject(columnIndex);
          return getHandler(handlerClassName).deserialize(value == null ? null : value.getValue());
        case MYSQL:
          Object obj = rs.getObject(columnIndex);
          return getHandler(handlerClassName).deserialize(obj);
        default:
          throw new IllegalArgumentException("Unknown db type: '" + DBType.get() + "'");
      }
    }

    public static Object getNullableResult(CallableStatement cs, int columnIndex, String handlerClassName) throws SQLException {
      switch (DBType.get()) {
        case PG:
          PGobject value = (PGobject) cs.getObject(columnIndex);
          return getHandler(handlerClassName).deserialize(value == null ? null : value.getValue());
        case MYSQL:
          Object obj = cs.getObject(columnIndex);
          return getHandler(handlerClassName).deserialize(obj);
        default:
          throw new IllegalArgumentException("Unknown db type: '" + DBType.get() + "'");
      }
    }

    private static Converter.Handler getHandler(String handlerClassName) {
      return cache.get(handlerClassName, () -> {
        try {
          return (Converter.Handler) Class.forName(handlerClassName).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  @Override
  public void startup() {
    Scanner.getClassesWithInterface(Converter.Handler.class)
      .stream()
      .filter(Scanner.ClassTag::isImplementClass)
      .forEach(v -> parse(v.runtimeClass()));
  }

  @Override
  public void shutdown() {
  }
}
