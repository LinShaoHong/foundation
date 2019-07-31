package com.github.sun.foundation.rest;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
@Provider
public class GlobalExceptionMapper implements ExtendedExceptionMapper<Throwable> {
  @Override
  public boolean isMappable(Throwable exception) {
    return !(exception instanceof WebApplicationException);
  }

  @Override
  public Response toResponse(Throwable exception) {
    log.warn("", exception);
    try (StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw)
    ) {
      exception.printStackTrace(pw);
      return Response.serverError().entity(sw.toString())
        .type("text/plain;charset=UTF-8")
        .build();
    } catch (IOException ex) {
      log.error("异常处理错误", ex);
      return Response.serverError().build();
    }
  }
}
