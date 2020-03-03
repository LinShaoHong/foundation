package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.Bootstrap;
import com.github.sun.foundation.boot.Scanner;
import com.github.sun.foundation.boot.utility.Configurators;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Info;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ApplicationPath("/api")
public abstract class JerseyApplication<A> extends ResourceConfig {
  @SuppressWarnings("unchecked")
  public JerseyApplication(ApplicationContext context) {
    Class<A> appClass = (Class<A>) getClass().getGenericSuperclass();
    Bootstrap bootstrap = Bootstrap.build(appClass, context);
    bootstrap.startup();
    Runtime.getRuntime().addShutdownHook(new Thread(bootstrap::shutdown));
    List<Class<?>> providers = Scanner.getClassesWithAnnotation(Provider.class)
      .stream()
      .map(Scanner.ClassTag::runtimeClass)
      .collect(Collectors.toList());
    List<Class<?>> resources = Scanner.getClassesWithAnnotation(Path.class)
      .stream()
      .map(Scanner.ClassTag::runtimeClass)
      .collect(Collectors.toList());
    log.info("Register jersey with following providers:\n{}\nand following resources:\n{}\n",
      Configurators.renderClasses(providers),
      Configurators.renderClasses(resources));
    HashSet<Class<?>> set = new HashSet<>();
    set.addAll(providers);
    set.addAll(resources);
    // swagger
    set.add(ApiListingResource.class);
    set.add(SwaggerSerializers.class);
    // multipart
    set.add(MultiPartFeature.class);
    // sse
    set.add(SseFeature.class);
    registerClasses(set);
    // inject context provider
    Scanner.getClassesWithInterface(RequestScopeContextResolver.BinderProvider.class)
      .stream()
      .filter(Scanner.ClassTag::isImplementClass)
      .forEach(provider -> register(provider.getInstance().binder()));
  }

  @Value("${spring.application.name}")
  private String appName;

  @Bean
  public Docket docket() {
    BeanConfig swagger = new BeanConfig();
    swagger.setBasePath("/api");
    swagger.setInfo(new Info().title(appName + " API"));
    SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, swagger);
    return new Docket(DocumentationType.SWAGGER_2).select().build();
  }

  @Bean
  @Primary
  public SwaggerResourcesProvider swaggerResource() {
    return () -> {
      SwaggerResource resource = new SwaggerResource();
      resource.setLocation("/api/swagger.json");
      resource.setSwaggerVersion("2.0");
      resource.setName("Jersey");
      return Collections.singletonList(resource);
    };
  }
}
