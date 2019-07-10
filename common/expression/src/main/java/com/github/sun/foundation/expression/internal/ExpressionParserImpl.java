package com.github.sun.foundation.expression.internal;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.expression.ExpressionBuilder;
import com.github.sun.foundation.expression.ExpressionParser;
import com.github.sun.foundation.expression.parser.ExpressionGrammarLexer;
import com.github.sun.foundation.expression.parser.ExpressionGrammarParser;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

/**
 * @Author LinSH
 * @Date: 7:04 PM 2019-02-28
 */
public class ExpressionParserImpl implements ExpressionParser {
  private final ExpressionBuilder builder = new ExpressionBuilder();

  @Override
  public Expression parse(InputStream in) {
    try {
      return parse(CharStreams.fromStream(in));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public Expression parse(Reader reader) {
    try {
      return parse(CharStreams.fromReader(reader));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public Expression parse(String in) {
    try {
      CharStream stream = CharStreams.fromString(in);
      return getParser(stream).start().value;
    } catch (RecognitionException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public List<Expression> parseTuple(String tuple) {
    try {
      CharStream stream = CharStreams.fromString(tuple);
      return getParser(stream).params().value;
    } catch (RecognitionException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Expression parse(CharStream charStream) {
    try {
      return getParser(charStream).start().value;
    } catch (RecognitionException ex) {
      throw new RuntimeException(ex);
    }
  }

  private CodeParser getParser(CharStream charStream) {
    ExpressionGrammarLexer lexer = new ExpressionGrammarLexer(charStream);
    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
    CodeParser parser = new CodeParser(tokenStream);
    parser.setExpressionBuilder(builder);
    return parser;
  }

  static class CodeParser extends ExpressionGrammarParser {
    CodeParser(TokenStream input) {
      super(input);
    }

    void setExpressionBuilder(ExpressionBuilder builder) {
      this.builder = builder;
    }
  }
}