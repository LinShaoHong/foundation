package com.github.sun.foundation.expression;


import com.github.sun.foundation.expression.internal.ExpressionParserImpl;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

public interface ExpressionParser {
  Expression parse(InputStream in);

  Expression parse(Reader reader);

  Expression parse(String in);

  List<Expression> parseTuple(String tuple);

  static ExpressionParser newParser() {
    return new ExpressionParserImpl();
  }
}
