package com.github.sun.foundation.sql;

import com.github.sun.foundation.expression.Expression;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @Author LinSH
 * @Date: 12:04 PM 2019-07-10
 */
public class ConnectionManager {
  private Supplier<Connection> connProvider;
  private final ThreadLocal<Context> contextHolder = new ThreadLocal<>();

  private ConnectionManager(DataSource dataSource) {
    this.connProvider = () -> {
      try {
        return dataSource.getConnection();
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    };
  }

  private ConnectionManager(Connection connection) {
    this.connProvider = () -> connection;
  }

  private ConnectionManager(String url, String username, String password) {
    this.connProvider = () -> {
      try {
        return DriverManager.getConnection(url, username, password);
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    };
  }

  public static ConnectionManager build(DataSource dataSource) {
    return new ConnectionManager(dataSource);
  }

  public static ConnectionManager build(Connection connection) {
    return new ConnectionManager(connection);
  }

  public static ConnectionManager build(String url, String username, String password) {
    return new ConnectionManager(url, username, password);
  }

  /**
   * 在当前线程重新创建一个可执行SQL的上下文，若执行失败且开启了事务，则该上下文内的操作都将被回滚。
   * 执行完成之后，将之前的上下文返回给当前线程
   */
  public <T> T withContext(Callable<T> func) {
    Exception ex = null;
    Context parent = contextHolder.get();
    Context context = new Context();
    contextHolder.set(context);
    try {
      return func.call();
    } catch (Exception e) {
      ex = e;
      throw new RuntimeException(ex);
    } finally {
      contextHolder.set(parent);
      try (Connection c = context.connection) {
        if (c != null) {
          try {
            if (!c.getAutoCommit()) {
              if (ex == null) {
                c.commit();
              } else {
                c.rollback();
              }
            }
          } finally {
            c.close();
          }
        }
      } catch (SQLException e) {
        if (ex == null) {
          ex = e;
        } else {
          ex.addSuppressed(e);
        }
        throw new RuntimeException(ex);
      }
    }
  }

  public void withContext(Action action) {
    withContext(() -> {
      action.run();
      return null;
    });
  }

  public interface Action {
    void run();
  }

  /**
   * 分配一个连接供jdbc使用，为了保证事务，该方法必须在特定的上下文中调用
   */
  public <T> T withConnection(boolean write, Function<Connection, T> func) {
    Context context = contextHolder.get();
    if (context == null) {
      throw new IllegalStateException("Require context");
    }
    try {
      Connection conn = context.connection;
      if (conn == null) {
        conn = connProvider.get();
        context.connection = conn;
      }
      if (write) {
        conn.setAutoCommit(false);
      }
      return func.apply(conn);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public interface ResultSetProcessor<A> {
    A apply(ResultSet rs) throws SQLException;
  }


  public interface ResultSetConsumer {
    void apply(ResultSet rs) throws SQLException;
  }

  public <T> T fetch(SqlBuilder.Template template, int fetchSize, ResultSetProcessor<T> func) {
    boolean write = fetchSize > 0;
    return withConnection(write, conn -> {
      try (PreparedStatement ps = conn.prepareStatement(template.placeholderSQL())) {
        ps.setFetchSize(fetchSize);
        List<Expression.Parameter> params = template.parameters();
        for (int i = 1; i <= params.size(); i++) {
          ps.setObject(i, params.get(i - 1).value());
        }
        ResultSet rs = ps.executeQuery();
        return func.apply(rs);
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    });
  }

  public <T> T fetch(SqlBuilder.Template template, ResultSetProcessor<T> func) {
    return fetch(template, 0, func);
  }

  public void fetch(SqlBuilder.Template template, int fetchSize, ResultSetConsumer func) {
    fetch(template, fetchSize, rs -> {
      func.apply(rs);
      return null;
    });
  }

  public void fetch(SqlBuilder.Template template, ResultSetConsumer func) {
    fetch(template, 0, func);
  }

  public List<Map<String, Object>> fetch(SqlBuilder.Template template) {
    return fetch(template, rs -> {
      List<Map<String, Object>> result = new ArrayList<>();
      try {
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        while (rs.next()) {
          Map<String, Object> map = new LinkedHashMap<>();
          for (int i = 0; i < count; i++) {
            String name = meta.getColumnName(i + 1);
            Object value = rs.getObject(name);
            map.put(name, value);
          }
          result.add(map);
        }
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
      return result;
    });
  }

  public Map<String, Object> fetchOne(SqlBuilder.Template template) {
    List<Map<String, Object>> mapList = fetch(template);
    return mapList.isEmpty() ? null : mapList.get(0);
  }

  public void execute(SqlBuilder.Template template) {
    withConnection(true, conn -> {
      try (PreparedStatement ps = conn.prepareStatement(template.placeholderSQL())) {
        List<Expression.Parameter> params = template.parameters();
        for (int i = 1; i <= params.size(); i++) {
          ps.setObject(i, params.get(i - 1).value());
        }
        return ps.execute();
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    });
  }

  public void executeBatch(Iterable<SqlBuilder.Template> templates) {
    withConnection(true, conn -> {
      try (Statement statement = conn.createStatement()) {
        for (SqlBuilder.Template template : templates) {
          statement.addBatch(template.literalSQL());
        }
        return statement.executeBatch();
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    });
  }

  public void executeBatch(SqlBuilder.Template template, JDBCBatchConsumer func) {
    withConnection(true, conn -> {
      try (PreparedStatement ps = conn.prepareStatement(template.placeholderSQL())) {
        Batch batch = new Batch(ps);
        func.apply(batch);
        return ps.executeBatch();
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    });
  }

  public interface JDBCBatchConsumer {
    void apply(Batch batch) throws SQLException;
  }

  public static class Batch {
    private final PreparedStatement ps;

    private Batch(PreparedStatement ps) {
      this.ps = ps;
    }

    private void push(Iterable<Object> values) {
      try {
        int i = 1;
        for (Object value : values) {
          ps.setObject(i++, value);
        }
        ps.addBatch();
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    }

    public void push(Object... values) {
      push(Arrays.asList(values));
    }
  }

  private static class Context {
    private Connection connection;
  }
}
