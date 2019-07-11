package com.github.sun.foundation.boot.exception;

public class UnAuthorizedException extends ResponsiveException {
  public UnAuthorizedException(String message) {
    super(message, Kind.UNAUTHORIZED);
  }
}
