package com.github.sun.foundation.spring.rest;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.github.sun"})
public class RestServer {
  public static void main(String[] args) {
    new SpringApplicationBuilder(RestServer.class).run(args);
  }
}
