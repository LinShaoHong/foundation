package com.github.sun.foundation.boot;

import com.github.sun.foundation.boot.json.ObjectMapperConfigurator;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Packages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Bootstrap {
  private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
  private static Bootstrap instance;

  private final Class<?> appClass;
  private final ApplicationContext context;

  private Bootstrap(Class<?> appClass, ApplicationContext context) {
    this.appClass = appClass;
    this.context = context;
  }

  public static Bootstrap build(Class<?> appClass, ApplicationContext context) {
    if (instance == null) {
      instance = new Bootstrap(appClass, context);
    }
    return instance;
  }

  public void startup() {
    // install injector
    Injector.install(context);
    // install scanner
    Scanner.install(loadBasePackages(appClass).toArray(new String[0]));
    // init JSON
    configObjectMapper();
  }

  private void configObjectMapper() {
    Scanner.getClassesWithInterface(ObjectMapperConfigurator.class)
      .forEach(v -> v.getInstance().config(JSON.getMapper()));
  }

  private <A> Set<String> loadBasePackages(Class<A> appClass) {
    Set<String> basePackages = new SetHelper();
    basePackages.add(Packages.group(Bootstrap.class) + ".foundation");
    Consumer<ComponentScan> func = a -> {
      if (a == null || a.value().length == 0) {
        basePackages.add(appClass.getPackage().getName());
      } else {
        basePackages.addAll(Arrays.asList(a.value()));
      }
    };
    ComponentScan a = appClass.getAnnotation(ComponentScan.class);
    func.accept(a);
    ComponentScans as = appClass.getAnnotation(ComponentScans.class);
    if (as != null) {
      for (ComponentScan scan : as.value()) {
        func.accept(scan);
      }
    }
    log.info("Scan component from following base packages:{}",
      Iterators.mkString(basePackages, "\n- ", "\n- ", "\n"));
    return basePackages;
  }

  private static class SetHelper extends HashSet<String> {
    @Override
    public boolean addAll(Collection<? extends String> c) {
      return c.stream().anyMatch(this::add);
    }

    @Override
    public boolean add(String s) {
      boolean b = super.stream().anyMatch(s::startsWith);
      if (!b) {
        super.add(s);
      }
      return b;
    }
  }
}
