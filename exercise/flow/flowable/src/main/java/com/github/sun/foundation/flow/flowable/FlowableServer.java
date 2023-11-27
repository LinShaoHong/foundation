package com.github.sun.foundation.flow.flowable;

import com.github.sun.foundation.rest.JerseyApplication;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class FlowableServer extends JerseyApplication {
  public FlowableServer(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(FlowableServer.class).run(args);
  }

  @Configuration
  public static class Config implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {
    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
      engineConfiguration.setActivityFontName("宋体");
      engineConfiguration.setLabelFontName("宋体");
      engineConfiguration.setAnnotationFontName("宋体");
    }
  }
}
