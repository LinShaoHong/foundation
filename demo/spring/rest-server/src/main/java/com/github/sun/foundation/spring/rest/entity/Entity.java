package com.github.sun.foundation.spring.rest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.mybatis.handler.JsonHandler;
import com.github.sun.foundation.sql.Handler;
import com.github.sun.foundation.sql.NamingStrategy;

import javax.persistence.Table;
import java.util.List;
import java.util.Map;

@Table(name = "TEST")
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

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public List<JsonNode> getLogs() {
    return logs;
  }

  public void setLogs(List<JsonNode> logs) {
    this.logs = logs;
  }

  public JsonNode getForm() {
    return form;
  }

  public void setForm(JsonNode form) {
    this.form = form;
  }

  public JsonNode getLog() {
    return log;
  }

  public void setLog(JsonNode log) {
    this.log = log;
  }

  public Map<String, Object> getMap() {
    return map;
  }

  public void setMap(Map<String, Object> map) {
    this.map = map;
  }

  public JsonNode[] getArr() {
    return arr;
  }

  public void setArr(JsonNode[] arr) {
    this.arr = arr;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }
}
