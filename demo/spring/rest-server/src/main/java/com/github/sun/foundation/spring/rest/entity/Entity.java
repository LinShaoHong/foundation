package com.github.sun.foundation.spring.rest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.mybatis.handler.JsonHandler;
import com.github.sun.foundation.sql.Handler;
import com.github.sun.foundation.sql.NamingStrategy;
import lombok.Data;

import javax.persistence.Table;
import java.util.List;
import java.util.Map;

@Data
@Table(name = "demo_test")
@NamingStrategy(NamingStrategy.CAMEL_CASE_TO_UPPER_UNDERSCORE)
public class Entity {
  public enum Type {
    A, B
  }

  private String id;
  private String name;
  private String address;
  @Handler(JsonHandler.ListJsonNodeHandler.class)
  private List<JsonNode> logs;
  @Handler(JsonHandler.JsonNodeHandler.class)
  private JsonNode form;
  @Handler(JsonHandler.JsonNodeHandler.class)
  private JsonNode log;
  private Map<String, Object> map;
  @Handler(JsonHandler.ArrayJsonNodeHandler.class)
  private JsonNode[] arr;
  private Type type;
}
