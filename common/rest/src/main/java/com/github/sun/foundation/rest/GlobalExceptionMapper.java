package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.utility.Throws;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;

@Slf4j
@Provider
public class GlobalExceptionMapper extends AbstractResource implements ExtendedExceptionMapper<Throwable> {
  @Override
  public boolean isMappable(Throwable exception) {
    return !(exception instanceof WebApplicationException);
  }

  @Override
  public javax.ws.rs.core.Response toResponse(Throwable ex) {
    log.error("Error:", ex);
    return javax.ws.rs.core.Response.serverError()
      .entity(responseOf(500, Throws.stackTraceOf(ex)))
      .type("application/json;charset=utf8")
      .build();
  }
}
