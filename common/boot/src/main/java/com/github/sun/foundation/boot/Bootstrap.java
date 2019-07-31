package com.github.sun.foundation.boot;

import com.github.sun.foundation.boot.json.ObjectMapperConfigurator;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Packages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class Bootstrap {
  private static Bootstrap instance;

  private final Class<?> appClass;
  private final ApplicationContext context;

  private List<Lifecycle> services;

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
    try {
      // install injector
      Injector.install(context);
      // install scanner
      Scanner.install(loadBasePackages(appClass).toArray(new String[0]));
      // init JSON
      configObjectMapper();
      // load injections
      loadInjectProviders();
      // start services
      startServices();
    } catch (Throwable ex) {
      log.error("Error occurs during startup", ex);
      cleanup();
    }
  }

  public void shutdown() {
    try {
      if (services != null) {
        Collections.reverse(services);
        services.forEach(Lifecycle::shutdown);
      }
    } catch (Throwable ex) {
      log.error("Error occurs during shutdown", ex);
      cleanup();
    }
  }

  private void cleanup() {
    this.services = null;
    log.info("Terminated.");
    System.exit(1);
  }

  private void loadInjectProviders() {
    List<InjectionProvider> providers = Scanner.getClassesWithInterface(InjectionProvider.class)
      .stream()
      .map(Scanner.ClassTag::getInstance)
      .collect(Collectors.toList());
    if (!providers.isEmpty()) {
      Set<String> set = providers.stream()
        .map(v -> v.getClass().getName())
        .collect(Collectors.toSet());
      providers.forEach(p -> p.config(new InjectionProvider.BinderImpl()));
      log.info("Inject by following providers:{}",
        Iterators.mkString(set, "\n- ", "\n- ", "\n"));
    }
  }

  private void startServices() {
    services = Injector.interfaceOf(Lifecycle.class);
    services.addAll(Scanner.getClassesWithInterface(Lifecycle.class)
      .stream()
      .filter(s -> services.stream().noneMatch(v -> v.getClass() == s.runtimeClass()))
      .map(Scanner.ClassTag::getInstance)
      .collect(Collectors.toList()));
    services.sort((s1, s2) -> {
      Order o1 = s1.getClass().getAnnotation(Order.class);
      Order o2 = s2.getClass().getAnnotation(Order.class);
      int i1 = o1 == null ? Order.DEFAULT : o1.value();
      int i2 = o2 == null ? Order.DEFAULT : o2.value();
      return i1 - i2;
    });
    services.forEach(Lifecycle::startup);
  }

  private void configObjectMapper() {
    Scanner.getClassesWithInterface(ObjectMapperConfigurator.class)
      .forEach(v -> v.getInstance().config(JSON.getMapper()));
  }

  private <A> Set<String> loadBasePackages(Class<A> appClass) {
    SetHelper basePackages = new SetHelper();
    basePackages.add(appClass.getPackage().getName());
    basePackages.add(Packages.group(Bootstrap.class) + ".foundation");
    Consumer<ComponentScan> func = a -> {
      if (a != null && a.value().length > 0) {
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
      c.forEach(this::add);
      return true;
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
