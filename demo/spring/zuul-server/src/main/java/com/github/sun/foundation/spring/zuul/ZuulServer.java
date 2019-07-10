package com.github.sun.foundation.spring.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * @Author LinSH
 * @Date: 8:49 PM 2019-07-06
 */
@EnableZuulProxy
@EnableEurekaClient
@SpringBootApplication
public class ZuulServer {
  public static void main(String[] args) {
    SpringApplication.run(ZuulServer.class, args);
  }
}
