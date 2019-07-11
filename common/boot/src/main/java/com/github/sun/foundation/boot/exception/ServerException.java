package com.github.sun.foundation.boot.exception;

public class ServerException extends ResponsiveException {
  public ServerException(String message) {
    super(message, Kind.SERVER_ERROR);
  }
}
