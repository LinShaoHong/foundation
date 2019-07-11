package com.github.sun.foundation.boot.exception;

public class NotFoundException extends ResponsiveException {
  public NotFoundException(String message) {
    super(message, Kind.NOT_FOUND);
  }
}
