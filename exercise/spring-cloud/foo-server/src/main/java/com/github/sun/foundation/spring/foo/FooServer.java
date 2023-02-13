package com.github.sun.foundation.spring.foo;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ApplicationContext;

@EnableEurekaClient
@SpringBootApplication
public class FooServer extends JerseyApplication {
  public FooServer(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(FooServer.class).run(args);
  }
}
