package com.github.sun.foundation.boot.exception;

public class TimeoutException extends ResponsiveException {
  public TimeoutException(String message) {
    super(message, Kind.TIMEOUT);
  }
}
