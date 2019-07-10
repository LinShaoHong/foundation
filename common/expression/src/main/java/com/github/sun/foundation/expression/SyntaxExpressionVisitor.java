package com.github.sun.foundation.expression;

/**
 * @Author LinSH
 * @Date: 上午9:29 2019-02-28
 */
public class SyntaxExpressionVisitor<T> implements Expression.Visitor<T> {
  @Override
  public T onUnaryExpression(Expression.UnaryExpression expr) {
    throw new UnsupportedOperationException("syntax error");
  }

  @Override
  public T onBinaryExpression(Expression.BinaryExpression expr) {
    throw new UnsupportedOperationException("syntax error");
  }

  @Override
  public T onMemberAccessExpression(Expression.MemberAccessExpression expr) {
    throw new UnsupportedOperationException("syntax error");
  }

  @Override
  public T onCallExpression(Expression.CallExpression expr) {
    throw new UnsupportedOperationException("syntax error");
  }

  @Override
  public T onCaseExpression(Expression.CaseExpression expr) {
    throw new UnsupportedOperationException("syntax error");
  }

  @Override
  public T onJsonPathExpression(Expression.JsonPathExpression expr) {
    throw new UnsupportedOperationException("syntax error");
  }

  @Override
  public T onIdentifier(Expression.Identifier expr) {
    throw new UnsupportedOperationException("syntax error");
  }

  @Override
  public T onLiteral(Expression.Literal expr) {
    throw new UnsupportedOperationException("syntax error");
  }

  @Override
  public T onParameter(Expression.Parameter expr) {
    throw new UnsupportedOperationException("syntax error");
  }

  @Override
  public T onEmpty(Expression expr) {
    throw new UnsupportedOperationException("syntax error");
  }
}
