package com.github.sun.foundation.spring.bar;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @Author LinSH
 * @Date: 8:16 PM 2019-07-06
 */
@Path("/v1/api")
@EnableEurekaClient
@SpringBootApplication
@ComponentScan({"com.github.sun"})
public class BarServer {
  public static void main(String[] args) {
    new SpringApplicationBuilder(BarServer.class).run(args);
  }

  @GET
  @Path("/bar")
  public String bar() {
    return "Hello, foo";
  }
}
