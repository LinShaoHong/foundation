package com.github.sun.foundation.sql.factory;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.foundation.sql.spi.BasicSqlBuilder;
import com.github.sun.foundation.sql.spi.SqlTemplate;

/**
 * @Author LinSH
 * @Date: 2:40 PM 2019-03-04
 */
public class SqlBuilderFactory {
  public static SqlBuilder.Factory mysql() {
    return Mysql::new;
  }

  private static class Mysql extends BasicSqlBuilder {
    @Override
    protected String escapeName(String name) {
      checkName(name);
      return "`" + name + "`";
    }

    @Override
    protected void buildSubJoinOnClause(SqlTemplate.Builder sb, Expression subQueryExpression) {
      sb.append(" (");
      sb.append(null, subQueryExpression);
      sb.append(")");
    }

    @Override
    protected void buildJoinOnClause(SqlTemplate.Builder sb, Expression on) {
      if (on != null) {
        sb.append(" ON ");
        on.visit(newVisitor(sb));
      }
    }

    @Override
    protected void buildLimitClause(SqlTemplate.Builder sb, SqlBuilder.Limit limit) {
      if (limit == null) return;
      Visitor visitor = newVisitor(sb);
      sb.append(" LIMIT ");
      limit.start.visit(visitor);
      sb.append(", ");
      limit.count.visit(visitor);
    }
  }

  public static SqlBuilder.Factory postgres() {
    return Postgres::new;
  }

  private static class Postgres extends BasicSqlBuilder {
    @Override
    protected String escapeName(String name) {
      checkName(name);
      return "\"" + name + "\"";
    }

    @Override
    protected void buildForUpdateClause(SqlTemplate.Builder sb, boolean forUpdate) {
      if (forUpdate) {
        throw new UnsupportedOperationException("POSTGRES NOT SUPPORT FOR-UPDATE");
      }
    }
  }
}
