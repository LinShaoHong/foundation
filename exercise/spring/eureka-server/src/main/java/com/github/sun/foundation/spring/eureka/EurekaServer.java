package com.github.sun.foundation.spring.eureka;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * @Author LinSH
 * @Date: 6:56 PM 2019-07-06
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaServer {
  public static void main(String[] args) {
    new SpringApplicationBuilder(EurekaServer.class).run(args);
  }
}
