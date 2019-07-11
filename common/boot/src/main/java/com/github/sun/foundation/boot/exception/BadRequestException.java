package com.github.sun.foundation.boot.exception;

public class BadRequestException extends ResponsiveException {
  public BadRequestException(String message) {
    super(message, Kind.BAD_REQUEST);
  }
}
