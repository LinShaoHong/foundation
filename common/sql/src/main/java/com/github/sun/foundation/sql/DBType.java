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

    private final static ThreadLocal<DBType> context = new ThreadLocal<>();

    public static void set(DBType dbType) {
        context.set(dbType);
    }

    public static DBType get() {
        DBType dbType = context.get();
        if (dbType == null) {
            //      throw new IllegalStateException("Can not find DBType from context.");
            return DBType.MYSQL;
        }
        return dbType;
    }

    public static void remove() {
        context.remove();
    }
}
