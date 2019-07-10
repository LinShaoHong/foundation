package com.github.sun.foundation.sql.spi;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.Model;
import com.github.sun.foundation.sql.SqlBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.function.BiConsumer;

/**
 * @Author LinSH
 * @Date: 10:35 AM 2019-03-01
 */
public class SqlTemplate implements SqlBuilder.Template {
  private List<Expression.Parameter> parameters;
  private final List<Expression> expressions;

  public SqlTemplate(List<Expression> expressions) {
    this.expressions = expressions;
  }

  @Override
  public String parameterizedSQL() {
    return buildSQL((sb, param) -> append(sb, param.name()),
      (sb, template) -> append(sb, template.parameterizedSQL()));
  }

  @Override
  public String placeholderSQL() {
    return buildSQL((sb, param) -> append(sb, "?"),
      (sb, template) -> append(sb, template.placeholderSQL()));
  }

  @Override
  public String literalSQL() {
    return buildSQL((sb, param) -> reformatLiteral(sb, param.value()),
      (sb, template) -> append(sb, template.literalSQL()));
  }

  private void append(Appendable sb, String s) {
    try {
      sb.append(s);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private String buildSQL(BiConsumer<Appendable, Expression.Parameter> paramFunc,
                          BiConsumer<Appendable, SqlBuilder.Template> templateFunc) {
    StringBuilder sb = new StringBuilder();
    expressions.forEach(e -> e.visit(new SqlBuilder.AbstractVisitor<Void>() {
      @Override
      public Void onLiteral(Expression.Literal expr) {
        sb.append(expr.value());
        return null;
      }

      @Override
      public Void onParameter(Expression.Parameter expr) {
        paramFunc.accept(sb, expr);
        return null;
      }

      @Override
      public Void onTemplateExpression(SqlBuilder.TemplateExpression expr) {
        templateFunc.accept(sb, expr.template());
        return null;
      }
    }));
    return sb.toString();
  }

  @Override
  public List<Expression.Parameter> parameters() {
    if (parameters == null) {
      parameters = new ArrayList<>();
      expressions.forEach(e -> e.visit(new SqlBuilder.AbstractVisitor<Void>() {
        @Override
        public Void onParameter(Expression.Parameter expr) {
          String name = expr.name();
          // support mybatis
          int i = name.indexOf(", typeHandler=");
          if (i > 0) {
            name = name.substring(2, i);
          } else {
            name = name.substring(2, name.length() - 1);
          }
          parameters.add(Expression.Parameter.of(name, expr.value()));
          return null;
        }

        @Override
        public Void onTemplateExpression(SqlBuilder.TemplateExpression expr) {
          parameters.addAll(expr.template().parameters());
          return null;
        }
      }));
    }
    return parameters;
  }

  private void reformatLiteral(Appendable sb, Object value) {
    try {
      if (value instanceof Integer || value instanceof Long) {
        sb.append(value.toString());
      } else if (value instanceof BigDecimal) {
        sb.append(((BigDecimal) value).toPlainString());
      } else if (value instanceof String) {
        sb.append("'");
        sb.append(value.toString());
        sb.append("'");
      } else if (value instanceof Boolean) {
        sb.append(value.toString());
      } else if (value instanceof Date) {
        // 日期当作long处理
        sb.append(String.valueOf(((Date) value).getTime()));
      } else if (value instanceof Duration) {
        sb.append(String.valueOf(((Duration) value).toMillis()));
      } else {
        sb.append(value == null ? "NULL" : value.toString());
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Builder newBuilder(AliasCounter counter) {
    return new Builder(counter);
  }

  public static class Builder implements Appendable {
    private final AliasCounter counter;
    private final List<Expression> expressions = new Vector<>();
    private StringBuilder sb;
    private boolean appendable = true;

    public Builder(AliasCounter counter) {
      this.counter = counter;
    }

    public Builder append(Model.Property property, Object value) {
      if (appendable && sb != null) {
        expressions.add(Expression.literal(sb.toString()));
        if (value instanceof Expression) {
          expressions.add((Expression) value);
        } else {
          StringBuilder alias = new StringBuilder();
          alias.append("#{$").append(counter.next());
          Class<?> typeHandler = property == null ? null : property.typeHandler();
          if (typeHandler != null && !property.isJsonPath()) {
            alias.append(", typeHandler=").append(typeHandler.getName());
          }
          alias.append("}");
          expressions.add(Expression.Parameter.of(alias.toString(), value));
        }
        sb = null;
      }
      return this;
    }

    public void setAppendable(boolean appendable) {
      this.appendable = appendable;
    }

    @Override
    public Builder append(CharSequence value) {
      if (appendable) {
        if (sb == null) {
          sb = new StringBuilder();
        }
        sb.append(value);
      }
      return this;
    }

    @Override
    public Builder append(CharSequence value, int start, int end) {
      if (appendable) {
        if (sb == null) {
          sb = new StringBuilder();
        }
        sb.append(value, start, end);
      }
      return this;
    }

    @Override
    public Builder append(char c) {
      if (appendable) {
        if (sb == null) {
          sb = new StringBuilder();
        }
        sb.append(c);
      }
      return this;
    }

    public SqlBuilder.Template build() {
      if (sb != null) {
        expressions.add(Expression.literal(sb.toString()));
      }
      return new SqlTemplate(expressions);
    }
  }

  interface AliasCounter {
    int next();
  }
}
