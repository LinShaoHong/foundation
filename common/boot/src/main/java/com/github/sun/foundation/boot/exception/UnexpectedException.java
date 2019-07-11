package com.github.sun.foundation.boot.exception;

public class UnexpectedException extends ResponsiveException {
  public UnexpectedException(String message) {
    super(message, Kind.UNEXPECTED);
  }

  public UnexpectedException(String message, Throwable ex) {
    super(message, Kind.UNEXPECTED, ex);
  }

  public UnexpectedException(Throwable ex) {
    super("", Kind.UNEXPECTED, ex);
  }

  @Override
  public String getMessage() {
    String message = super.getMessage();
    Throwable cause = getCause();
    if (cause != null) {
      message = message + "\nCause by " + stackTraceOf(cause);
    }
    return message;
  }
}
