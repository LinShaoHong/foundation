package com.github.sun.foundation.expression;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class ExpressionBuilder {
  public Expression number(String value) {
    if (value == null) {
      throw new IllegalArgumentException();
    }
    try {
      BigDecimal decimal = new BigDecimal(value);
      return Expression.Literal.of(decimal);
    } catch (NumberFormatException ex) {
      throw new RuntimeException(value + " is not a number");
    }
  }

  public Expression string(String value) {
    String v = value;
    if (v == null || !v.startsWith("\"") || !v.endsWith("\"")) {
      throw new IllegalArgumentException();
    }
    v = v.substring(1, v.length() - 1);
    return Expression.Literal.of(v);
  }

  public String unquote(String value) {
    String v = value;
    if (v == null || !v.startsWith("\"") || !v.endsWith("\"")) {
      throw new IllegalArgumentException();
    }
    v = v.substring(1, v.length() - 1);
    return v;
  }

  public Expression id(String value) {
    return Expression.id(value);
  }

  public Expression bool(boolean v) {
    return Expression.Literal.of(v);
  }

  public Expression eq(Expression a, Expression b) {
    return a.eq(b);
  }

  public Expression gt(Expression a, Expression b) {
    return a.gt(b);
  }

  public Expression ge(Expression a, Expression b) {
    return a.ge(b);
  }

  public Expression lt(Expression a, Expression b) {
    return a.lt(b);
  }

  public Expression le(Expression a, Expression b) {
    return a.le(b);
  }

  public Expression ne(Expression a, Expression b) {
    return a.ne(b);
  }

  public Expression and(Expression a, Expression b) {
    return a.and(b);
  }

  public Expression or(Expression a, Expression b) {
    return a.or(b);
  }

  public Expression not(Expression expression) {
    return expression.not();
  }

  public Expression plus(Expression a, Expression b) {
    return a.plus(b);
  }

  public Expression sub(Expression a, Expression b) {
    return a.sub(b);
  }

  public Expression mul(Expression a, Expression b) {
    return a.mul(b);
  }

  public Expression div(Expression a, Expression b) {
    return a.div(b);
  }

  public Expression mod(Expression a, Expression b) {
    return a.mod(b);
  }

  public Expression call(Expression func, List<Expression> params) {
    return func.call(params);
  }

  public Expression call(Expression func) {
    return call(func, Collections.emptyList());
  }

  public Expression member(Expression obj, String member) {
    return obj.member(member);
  }
}

