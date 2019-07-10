package com.github.sun.foundation.boot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author LinSH
 * @Date: 12:44 PM 2019-07-10
 */
public class JSON {
  private static final ObjectMapper mapper = new ObjectMapper();

  public ObjectMapper getMapper() {
    return mapper;
  }

  /**
   * 序列化
   */
  public static String serialize(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static byte[] serializeAsByte(Object object) {
    try {
      return mapper.writeValueAsBytes(object);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static JsonNode asJsonNode(Object obj) {
    return mapper.valueToTree(obj);
  }

  public static JsonNode asJsonNode(String json) {
    try {
      return mapper.readTree(json);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * 反序列化
   */
  public static <T> T deserialize(String json, Class<T> clazz) {
    try {
      return mapper.readValue(json, clazz);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T> T deserialize(byte[] bytes, Class<T> clazz) {
    try {
      return mapper.readValue(bytes, clazz);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T> T deserialize(Object object, Class<T> clazz) {
    try {
      return mapper.convertValue(object, clazz);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T> List<T> deserializeAsList(String json, Class<T> clazz) {
    JavaType type = mapper.getTypeFactory().constructParametrizedType(List.class, List.class, clazz);
    try {
      return mapper.readValue(json, type);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T> List<T> deserializeAsList(Object object, Class<T> clazz) {
    JavaType type = mapper.getTypeFactory().constructParametrizedType(List.class, List.class, clazz);
    return mapper.convertValue(object, type);
  }

  public static <T> Set<T> deserializeAsSet(String json, Class<T> clazz) {
    JavaType type = mapper.getTypeFactory().constructParametrizedType(Set.class, Set.class, clazz);
    try {
      return mapper.readValue(json, type);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T> Set<T> deserializeAsSet(Object object, Class<T> clazz) {
    JavaType type = mapper.getTypeFactory().constructParametrizedType(Set.class, Set.class, clazz);
    return mapper.convertValue(object, type);
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] deserializeAsArray(String json, Class<T> clazz) {
    try {
      Class<T[]> arrayClass = (Class<T[]>) Class.forName("[L" + clazz.getName() + ";");
      return mapper.readValue(json, arrayClass);
    } catch (IOException | ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] deserializeAsArray(Object object, Class<T> clazz) {
    try {
      Class<T[]> arrayClass = (Class<T[]>) Class.forName("[L" + clazz.getName() + ";");
      return mapper.convertValue(object, arrayClass);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <A, B> Map<A, B> deserializeAsMap(String json, Class<B> valueClass, Function<String, A> keyProvider) {
    if (json == null) {
      return Collections.emptyMap();
    }
    try {
      Map<String, B> value = mapper.readValue(json, new TypeReference<Map<String, B>>() {
      });
      return value.entrySet().stream()
        .collect(Collectors.toMap(e -> keyProvider.apply(e.getKey()), e -> deserialize(e.getValue(), valueClass)));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <B> Map<String, B> deserializeAsMap(String json, Class<B> valueClass) {
    return deserializeAsMap(json, valueClass, v -> v);
  }
}
