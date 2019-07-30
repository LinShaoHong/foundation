package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@UtilityClass
public class Throws {
  public String stackTraceOf(Throwable ex) {
    try (StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw)) {
      ex.printStackTrace(pw);
      return sw.toString();
    } catch (IOException ex2) {
      return ex.getMessage();
    }
  }
}
