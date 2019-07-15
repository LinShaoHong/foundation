package com.github.sun.foundation.mybatis.config;

public class DruidSettings {
  public final String url;
  public final String username;
  public final String password;
  public final String filters;
  public final Integer maxActive;
  public final Integer initialSize;
  public final Integer minIdle;
  public final Integer maxWait;
  public final Integer timeBetweenEvictionRunsMillis;
  public final Integer minEvictableIdleTimeMillis;
  public final String validationQuery;
  public final Boolean testWhileIdle;
  public final Boolean testOnReturn;
  public final Boolean testOnBorrow;
  public final Integer helperThreadSize;

  public DruidSettings(String url, String username, String password, String filters, Integer maxActive, Integer initialSize, Integer minIdle, Integer maxWait, Integer timeBetweenEvictionRunsMillis, Integer minEvictableIdleTimeMillis, String validationQuery, Boolean testWhileIdle, Boolean testOnReturn, Boolean testOnBorrow, Integer helperThreadSize) {
    this.url = url;
    this.username = username;
    this.password = password;
    this.filters = filters;
    this.maxActive = maxActive;
    this.initialSize = initialSize;
    this.minIdle = minIdle;
    this.maxWait = maxWait;
    this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    this.validationQuery = validationQuery;
    this.testWhileIdle = testWhileIdle;
    this.testOnReturn = testOnReturn;
    this.testOnBorrow = testOnBorrow;
    this.helperThreadSize = helperThreadSize;
  }
}
