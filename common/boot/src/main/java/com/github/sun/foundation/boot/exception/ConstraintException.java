package com.github.sun.foundation.boot.exception;

public class ConstraintException extends ResponsiveException {
  public ConstraintException(String message) {
    super(message, Kind.ANTI_CONSTRAINT);
  }

  public ConstraintException(String message, Throwable cause) {
    super(message, Kind.ANTI_CONSTRAINT, cause);
  }
}
