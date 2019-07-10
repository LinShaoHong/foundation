package com.github.sun.foundation.spring.foo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author LinSH
 * @Date: 8:19 PM 2019-07-06
 */
@RestController
@EnableEurekaClient
@SpringBootApplication
public class FooServer {
  public static void main(String[] args) {
    new SpringApplicationBuilder(FooServer.class).run(args);
  }

  @GetMapping("/v1/api/foo")
  public String foo() {
    return "Hello, bar";
  }
}
