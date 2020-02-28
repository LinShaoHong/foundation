package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.utility.Iterators;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper extends AbstractResource implements ExceptionMapper<ConstraintViolationException> {
  @Override
  public javax.ws.rs.core.Response toResponse(ConstraintViolationException exception) {
    Set<String> messages = exception.getConstraintViolations()
      .stream()
      .map(ConstraintViolation::getMessageTemplate)
      .collect(Collectors.toSet());
    String errorMsg = Iterators.mkString(messages, "Bad Request: ", "; ", "");
    return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST)
      .entity(responseOf(javax.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode(), errorMsg))
      .type("application/json;charset=utf8")
      .build();
  }
}
