package com.github.sun.foundation.sql.spi;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.expression.Expression.CallExpression;
import com.github.sun.foundation.expression.Expression.Literal;
import com.github.sun.foundation.expression.Expression.MemberAccessExpression;
import com.github.sun.foundation.sql.SqlBuilder;

import java.util.List;

public abstract class AbstractSqlVisitor<T> extends SqlBuilder.AbstractVisitor<T> {
  protected abstract T onIsNullExpression(Expression value);

  protected abstract T onIsNotNullExpression(Expression value);

  protected abstract T onOverExpression(Expression value, String sql);

  protected abstract T onDistinctExpression(Expression value);

  protected abstract T onCastExpression(Expression value, String dataType);

  protected abstract T onBetweenExpression(Expression value, Expression start, Expression end);

  protected abstract T onInExpression(Expression value, List<Expression> parameters, boolean isIn);

  protected abstract T onFuncExpression(Expression func, List<Expression> parameters);

  protected abstract T onLikeExpression(Expression value, Expression parameter, LikeModel model);

  enum LikeModel {
    STARTS_WITH, CONTAINS, ENDS_WITH
  }

  @Override
  public T onCallExpression(CallExpression expr) {
    Expression function = expr.getFunction();
    List<Expression> parameters = expr.getParameters();
    if (function instanceof MemberAccessExpression) {
      MemberAccessExpression ma = (MemberAccessExpression) function;
      switch (ma.getMember()) {
        case "in":
          return onInExpression(ma.getObject(), parameters, true);
        case "notIn":
          return onInExpression(ma.getObject(), parameters, false);
        case "cast":
          if (parameters.size() != 1) {
            throw new IllegalArgumentException("cast function dose not has only one parameter");
          }
          if (!(parameters.get(0) instanceof Literal)) {
            throw new IllegalArgumentException("parameter of cast function must be a Literal");
          }
          return onCastExpression(ma.getObject(), ((Literal) parameters.get(0)).getValue().toString());
        case "over":
          if (parameters.size() != 1) {
            throw new IllegalArgumentException("over function dose not has only one parameter");
          }
          if (!(parameters.get(0) instanceof Expression.Identifier)) {
            throw new IllegalArgumentException("parameter of over function must be a Identifier");
          }
          return onOverExpression(ma.getObject(), ((Expression.Identifier) parameters.get(0)).getName());
        case "between":
          if (parameters.size() != 2) {
            throw new IllegalArgumentException("between function dose not has only two parameter");
          }
          return onBetweenExpression(ma.getObject(), parameters.get(0), parameters.get(1));
        case "isNull":
          if (!parameters.isEmpty()) {
            throw new IllegalArgumentException("isNull function has more than zero parameter");
          }
          return onIsNullExpression(ma.getObject());
        case "isNotNull":
          if (!parameters.isEmpty()) {
            throw new IllegalArgumentException("isNotNull function has more than zero parameter");
          }
          return onIsNotNullExpression(ma.getObject());
        case "distinct":
          if (!parameters.isEmpty()) {
            throw new IllegalArgumentException("distinct function has more than zero parameter");
          }
          return onDistinctExpression(ma.getObject());
        case "contains":
          return onLikeExpression(ma.getObject(), parameters.get(0), LikeModel.CONTAINS);
        case "endsWith":
          return onLikeExpression(ma.getObject(), parameters.get(0), LikeModel.ENDS_WITH);
        case "startsWith":
          return onLikeExpression(ma.getObject(), parameters.get(0), LikeModel.STARTS_WITH);
      }
    }
    return onFuncExpression(function, parameters);
  }
}
