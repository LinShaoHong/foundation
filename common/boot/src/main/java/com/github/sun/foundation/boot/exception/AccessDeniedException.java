package com.github.sun.foundation.boot.exception;

public class AccessDeniedException extends ResponsiveException {
  public AccessDeniedException(String message) {
    super(message, Kind.ACCESS_DENIED);
  }
}
