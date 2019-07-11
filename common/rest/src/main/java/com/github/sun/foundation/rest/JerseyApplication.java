package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.utility.Configurators;
import com.github.sun.foundation.boot.utility.Packages;
import com.github.sun.foundation.boot.utility.Scanner;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author LinSH
 * @Date: 10:31 AM 2019-07-11
 */
@Configuration
public class JerseyApplication extends ResourceConfig {
  private static final Logger log = LoggerFactory.getLogger(JerseyApplication.class);

  public JerseyApplication() {
    Scanner scanner = Scanner.create(Packages.group(getClass()));
    List<Class<?>> providers = scanner.getClassesWithAnnotation(Provider.class)
      .stream()
      .map(Scanner.ClassTag::runtimeClass)
      .collect(Collectors.toList());
    List<Class<?>> resources = scanner.getClassesWithAnnotation(Path.class)
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
