package com.github.sun.foundation.spring.rest;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @Author LinSH
 * @Date: 12:17 PM 2019-07-11
 */
@Path("/v1/api")
@SpringBootApplication
@ComponentScan({"com.github.sun"})
public class RestServer {
  public static void main(String[] args) {
    new SpringApplicationBuilder(RestServer.class).run(args);
  }

  @GET
  @Path("/hello")
  public String foo() {
    return "Hello, World";
  }
}
