package com.github.sun.foundation.spring.bar;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author LinSH
 * @Date: 8:16 PM 2019-07-06
 */
@RestController
@EnableEurekaClient
@SpringBootApplication
public class BarServer {
  public static void main(String[] args) {
    new SpringApplicationBuilder(BarServer.class).run(args);
  }

  @GetMapping("/v1/api/bar")
  public String bar() {
    return "Hello, foo";
  }
}
