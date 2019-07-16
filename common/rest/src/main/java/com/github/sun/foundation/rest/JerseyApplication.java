package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.Bootstrap;
import com.github.sun.foundation.boot.Scanner;
import com.github.sun.foundation.boot.utility.Configurators;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public abstract class JerseyApplication<A> extends ResourceConfig {
  private static final Logger log = LoggerFactory.getLogger(JerseyApplication.class);

  @SuppressWarnings("unchecked")
  public JerseyApplication(ApplicationContext context) {
    Class<A> appClass = (Class<A>) getClass().getGenericSuperclass();
    Bootstrap bootstrap = Bootstrap.build(appClass, context);
    bootstrap.startup();
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
    registerClasses(set);
  }
}
