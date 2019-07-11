package com.github.sun.foundation.spring.foo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @Author LinSH
 * @Date: 8:19 PM 2019-07-06
 */
@Path("/v1/api")
@EnableEurekaClient
@SpringBootApplication
@ComponentScan({"com.github.sun"})
public class FooServer {
  public static void main(String[] args) {
    new SpringApplicationBuilder(FooServer.class).run(args);
  }

  @GET
  @Path("/foo")
  public String foo() {
    return "Hello, bar";
  }
}
