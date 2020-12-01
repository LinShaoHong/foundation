package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.exception.ResponsiveException;
import com.github.sun.foundation.boot.utility.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.api.MultiException;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;

@Slf4j
@Provider
public class ResponsiveExceptionMapper implements ExceptionMapper<ResponsiveException> {
  private final Helper helper = new Helper();

  @Override
  public javax.ws.rs.core.Response toResponse(ResponsiveException exception) {
    return helper.toResponse(exception);
  }

  @Provider
  public static class MultiExceptionMapper extends AbstractResource implements ExceptionMapper<MultiException> {
    private final Helper helper = new Helper();

    @Override
    public javax.ws.rs.core.Response toResponse(MultiException ex) {
      List<Throwable> errors = ex.getErrors();
      Throwable throwable = errors.stream().filter(v -> (v instanceof ResponsiveException)).findFirst().orElse(null);
      if (throwable != null) {
        return helper.toResponse((ResponsiveException) throwable);
      }
      javax.ws.rs.core.Response.Status status = javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
      String message = Iterators.mkString(errors, "\n", Throwable::getMessage);
      return javax.ws.rs.core.Response.status(status)
        .entity(responseOf(status.getStatusCode(), message))
        .type("application/json;charset=utf8")
        .build();
    }
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
