package com.github.sun.foundation.mybatis.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.DBType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class JsonHandler<T> extends BaseTypeHandler<T> {
  private final Class<T> clazz;

  public JsonHandler() {
    Type type = getClass().getGenericSuperclass();
    this.clazz = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
    switch (DBType.get()) {
      case PG:
        PGobject value = new PGobject();
        value.setType("jsonb");
        value.setValue(serialize(parameter));
        ps.setObject(i, value);
        break;
      case MYSQL:
        ps.setObject(i, serialize(parameter));
        break;
      default:
        throw new IllegalArgumentException("Unknown db type: '" + DBType.get() + "'");
    }
  }

  @Override
  @SuppressWarnings("Duplicates")
  public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
    switch (DBType.get()) {
      case PG:
        PGobject value = (PGobject) rs.getObject(columnName);
        return deserialize(value == null ? null : value.getValue());
      case MYSQL:
        Object obj = rs.getObject(columnName);
        return deserialize((String) obj);
      default:
        throw new IllegalArgumentException("Unknown db type: '" + DBType.get() + "'");
    }
  }

  @Override
  @SuppressWarnings("Duplicates")
  public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    switch (DBType.get()) {
      case PG:
        PGobject value = (PGobject) rs.getObject(columnIndex);
        return deserialize(value == null ? null : value.getValue());
      case MYSQL:
        Object obj = rs.getObject(columnIndex);
        return deserialize((String) obj);
      default:
        throw new IllegalArgumentException("Unknown db type: '" + DBType.get() + "'");
    }
  }

  @Override
  @SuppressWarnings("Duplicates")
  public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    switch (DBType.get()) {
      case PG:
        PGobject value = (PGobject) cs.getObject(columnIndex);
        return deserialize(value == null ? null : value.getValue());
      case MYSQL:
        Object obj = cs.getObject(columnIndex);
        return deserialize((String) obj);
      default:
        throw new IllegalArgumentException("Unknown db type: '" + DBType.get() + "'");
    }
  }

  public String serialize(T value) {
    return JSON.serialize(value);
  }

  public T deserialize(String value) {
    return value == null ? null : JSON.deserialize(value, clazz);
  }

  public abstract static class ListHandler<T> extends JsonHandler<List<T>> {
    private final Class<T> clazz;

    public ListHandler() {
      Type type = getClass().getGenericSuperclass();
      this.clazz = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    @Override
    public List<T> deserialize(String value) {
      return value == null ? Collections.emptyList() : JSON.deserializeAsList(value, clazz);
    }
  }

  public abstract static class SetHandler<T> extends JsonHandler<Set<T>> {
    private final Class<T> clazz;

    public SetHandler() {
      Type type = getClass().getGenericSuperclass();
      this.clazz = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    @Override
    public Set<T> deserialize(String value) {
      return value == null ? Collections.emptySet() : JSON.deserializeAsSet(value, clazz);
    }
  }

  public abstract static class ArrayHandler<M> extends JsonHandler<M[]> {
    private Class<M> clazz;

    public ArrayHandler() {
      Type type = getClass().getGenericSuperclass();
      this.clazz = (Class<M>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    @Override
    public M[] deserialize(String value) {
      return value == null ? null : JSON.deserializeAsArray(value, clazz);
    }
  }

  public abstract static class MapHandler<A, B> extends JsonHandler<Map<A, B>> {
    private final Class<B> valueClass;
    private final Function<String, A> keyProvider;

    public MapHandler(Function<String, A> keyProvider) {
      this.keyProvider = keyProvider;
      ParameterizedType pt = (ParameterizedType) getClass().getGenericSuperclass();
      this.valueClass = (Class<B>) pt.getActualTypeArguments()[1];
    }

    @Override
    public Map<A, B> deserialize(String value) {
      return JSON.deserializeAsMap(value, valueClass, keyProvider);
    }
  }

  /**
   * for com.fasterxml.jackson.databind.node.ObjectNode
   */
  public static final class ObjectNodeHandler extends JsonHandler<ObjectNode> {
  }

  /**
   * for List<com.fasterxml.jackson.databind.node.ObjectNode>
   */
  public static final class ListObjectNodeHandler extends ListHandler<ObjectNode> {
  }

  /**
   * for Set<com.fasterxml.jackson.databind.node.ObjectNode>
   */
  public static final class SetObjectNodeHandler extends SetHandler<ObjectNode> {
  }

  /**
   * for com.fasterxml.jackson.databind.node.ObjectNode[]
   */
  public static final class ArrayObjectNodeHandler extends ArrayHandler<ObjectNode> {
  }

  /**
   * for com.fasterxml.jackson.databind.JsonNode
   */
  public static final class JsonNodeHandler extends JsonHandler<JsonNode> {
  }

  /**
   * for List<com.fasterxml.jackson.databind.node.ObjectNode>
   */
  public static final class ListJsonNodeHandler extends ListHandler<JsonNode> {
  }

  /**
   * for Set<com.fasterxml.jackson.databind.node.ObjectNode>
   */
  public static final class SetJsonNodeHandler extends SetHandler<JsonNode> {
  }

  /**
   * for com.fasterxml.jackson.databind.node.ObjectNode[]
   */
  public static final class ArrayJsonNodeHandler extends ArrayHandler<JsonNode> {
  }

  /**
   * for java.util.Map<String,Object>
   */
  public static final class GenericMapHandler extends MapHandler<String, Object> {
    public GenericMapHandler() {
      super(v -> v);
    }
  }

  /**
   * for java.util.List<String>
   */
  public static final class ListStringHandler extends ListHandler<String> {
  }

  /**
   * for java.util.Set<String>
   */
  public static final class SetStringHandler extends SetHandler<String> {
  }

  /**
   * for String[]
   */
  public static final class ArrayStringHandler extends ArrayHandler<String> {
  }

  /**
   * for java.util.List<Integer>
   */
  public static final class ListIntHandler extends ListHandler<Integer> {
  }

  /**
   * for java.util.Set<Integer>
   */
  public static final class SetIntHandler extends SetHandler<Integer> {
  }

  /**
   * for Integer[]
   */
  public static final class ArrayIntHandler extends ArrayHandler<Integer> {
  }

  /**
   * for java.util.List<Long>
   */
  public static final class ListLongHandler extends ListHandler<Long> {
  }

  /**
   * for java.util.Set<Long>
   */
  public static final class SetLongHandler extends SetHandler<Long> {
  }

  /**
   * for Long[]
   */
  public static final class ArrayLongHandler extends ArrayHandler<Integer> {
  }

  /**
   * for java.util.List<Double>
   */
  public static final class ListDoubleHandler extends ListHandler<Double> {
  }

  /**
   * for java.util.Set<Double>
   */
  public static final class SetDoubleHandler extends SetHandler<Double> {
  }

  /**
   * for Double[]
   */
  public static final class ArrayDoubleHandler extends ArrayHandler<Integer> {
  }
}
