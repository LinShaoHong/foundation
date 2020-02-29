package com.github.sun.foundation.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public interface SqlSessionMeta {
  String database();

  String basePackage();

  String configLocation();

  DataSource dataSource();

  SqlSessionFactory sqlSessionFactory();

  static SqlSessionMeta build(String configLocation, String database, String basePackage, DataSource dataSource, SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionMeta() {
      @Override
      public String database() {
        return database;
      }

      @Override
      public String basePackage() {
        return basePackage;
      }

      @Override
      public String configLocation() {
        return configLocation;
      }

      @Override
      public DataSource dataSource() {
        return dataSource;
      }

      @Override
      public SqlSessionFactory sqlSessionFactory() {
        return sqlSessionFactory;
      }
    };
  }

  Map<String, SqlSessionMeta> collector = new HashMap<>();
}
