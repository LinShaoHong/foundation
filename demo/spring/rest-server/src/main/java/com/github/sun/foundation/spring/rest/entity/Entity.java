package com.github.sun.foundation.spring.rest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.modelling.JsonHandler;
import com.github.sun.foundation.modelling.Converter;
import com.github.sun.foundation.modelling.NamingStrategy;
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
  @Converter(JsonHandler.ListJsonNodeHandler.class)
  private List<JsonNode> logs;
  @Converter(JsonHandler.JsonNodeHandler.class)
  private JsonNode form;
  @Converter(JsonHandler.JsonNodeHandler.class)
  private JsonNode log;
  private Map<String, Object> map;
  @Converter(JsonHandler.ArrayJsonNodeHandler.class)
  private JsonNode[] arr;
  private Type type;
}
