package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.exception.ResponsiveException;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Slf4j
@Provider
public class ResponsiveExceptionMapper implements ExceptionMapper<ResponsiveException> {
  private Helper helper = new Helper();

  @Override
  public javax.ws.rs.core.Response toResponse(ResponsiveException exception) {
    return helper.toResponse(exception);
  }

  private static class Helper extends AbstractResource {
    private javax.ws.rs.core.Response toResponse(ResponsiveException exception) {
      if (exception.getKind() == ResponsiveException.Kind.UNEXPECTED) {
        log.error("Unexpected exception: ", exception);
      }
      int code = exception.getCode();
      javax.ws.rs.core.Response.Status status = javax.ws.rs.core.Response.Status.fromStatusCode(code);
      status = status == null ? javax.ws.rs.core.Response.Status.OK : status;
      return javax.ws.rs.core.Response.status(status)
        .entity(responseOf(code, exception.getMessage()))
        .type("application/json;charset=utf8")
        .build();
    }
  }
}
