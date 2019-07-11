package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.utility.Iterators;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author LinSH
 * @Date: 10:36 AM 2019-07-11
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
  public Response toResponse(ConstraintViolationException exception) {
    Set<String> messages = exception.getConstraintViolations()
      .stream()
      .map(ConstraintViolation::getMessageTemplate)
      .collect(Collectors.toSet());
    String errorMsg = Iterators.mkString(messages, "Bad Request:\n- ", "\n- ", "");
    return Response.status(Response.Status.BAD_REQUEST)
      .entity(errorMsg)
      .build();
  }
}
