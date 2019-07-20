package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.exception.ResponsiveException;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Slf4j
@Provider
public class ResponsiveExceptionMapper implements ExceptionMapper<ResponsiveException> {
  @Override
  public Response toResponse(ResponsiveException exception) {
    if (exception.getKind() == ResponsiveException.Kind.UNEXPECTED) {
      log.error("Unexpected exception: ", exception);
    }
    return Response.status(getStatus(exception.getKind()))
      .entity(exception.getMessage())
      .type("text/plain; charset=UTF-8")
      .build();
  }

  private Response.Status getStatus(ResponsiveException.Kind kind) {
    switch (kind) {
      case UNAUTHORIZED:
        return Response.Status.UNAUTHORIZED;
      case ACCESS_DENIED:
        return Response.Status.FORBIDDEN;
      case ANTI_CONSTRAINT:
        return Response.Status.PRECONDITION_FAILED;
      case BAD_REQUEST:
        return Response.Status.BAD_REQUEST;
      case NOT_FOUND:
        return Response.Status.NOT_FOUND;
      case TIMEOUT:
        return Response.Status.REQUEST_TIMEOUT;
      case SERVER_ERROR:
        return Response.Status.SERVICE_UNAVAILABLE;
      case UNEXPECTED:
        return Response.Status.INTERNAL_SERVER_ERROR;
      default:
        throw new IllegalStateException();
    }
  }
}
