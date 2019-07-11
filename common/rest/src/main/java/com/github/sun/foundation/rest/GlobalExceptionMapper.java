package com.github.sun.foundation.rest;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @Author LinSH
 * @Date: 10:33 AM 2019-07-11
 */
@Provider
public class GlobalExceptionMapper implements ExtendedExceptionMapper<Throwable> {
  private final Logger log = LoggerFactory.getLogger(GlobalExceptionMapper.class);

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
      return Response.serverError().entity(sw.toString()).type("text/plain;charset=UTF-8").build();
    } catch (IOException ex) {
      log.error("异常处理错误", ex);
      return Response.serverError().build();
    }
  }
}
