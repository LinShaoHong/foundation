package com.github.sun.foundation.expression;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @Author LinSH
 * @Date: 7:18 PM 2019-02-28
 */
public class ExpressionTest {
  @Test
  public void testFunc() {
    ExpressionParser parser = ExpressionParser.newParser();
    Expression expression = parser.parse("form.name.in(\"hello\",\"world\") && nickName.isNull()");
    Assert.assertEquals(Expression.path("form.name").in("hello", "world").and(Expression.path("nickName").isNull()), expression);
  }

  @Test
  public void testCmp() {
    ExpressionParser parser = ExpressionParser.newParser();
    Expression expression = parser.parse("name == \"lin\" && age == 25 && active == true");
    Assert.assertEquals(Expression.id("name").eq("lin").and(Expression.id("age").eq(new BigDecimal(25))).and(Expression.id("active").eq(true)), expression);
    expression = parser.parse("name == \"lin\" && (age == 25 || active == true) && time<1000");
    Assert.assertEquals(Expression.id("name").eq("lin").and(Expression.id("age").eq(new BigDecimal(25)).or(Expression.id("active").eq(true))).and(Expression.id("time").lt(new BigDecimal(1000))), expression);
  }

  @Test
  public void testTuple() {
    ExpressionParser parser = ExpressionParser.newParser();
    List<Expression> tuple = parser.parseTuple("desc(name),asc(age)");
    assertEquals(tuple.get(0), Expression.id("desc").call(Expression.id("name")));
    assertEquals(tuple.get(1), Expression.id("asc").call(Expression.id("age")));
  }
}
