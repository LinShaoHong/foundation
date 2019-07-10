package com.github.sun.foundation.sql;

/**
 * @Author LinSH
 * @Date: 12:58 PM 2019-07-10
 */
public enum DBType {
  MYSQL, PG;

  public static DBType from(String jdbcUrl) {
    if (jdbcUrl.startsWith("jdbc:mysql:")
      || jdbcUrl.startsWith("jdbc:log4jdbc:mysql:")) {
      return DBType.MYSQL;
    } else if (jdbcUrl.startsWith("jdbc:postgresql:")
      || jdbcUrl.startsWith("jdbc:log4jdbc:postgresql:")) {
      return DBType.PG;
    }
    throw new UnsupportedOperationException("Server do not support the JDBCDatasource with url: " + jdbcUrl);
  }
}
