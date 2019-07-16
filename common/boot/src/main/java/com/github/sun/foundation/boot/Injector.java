package com.github.sun.foundation.boot;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Injector {
  private static ApplicationContext inner;
  private static DefaultListableBeanFactory registry;

  private Injector() {
  }

  public static void install(ApplicationContext context) {
    if (inner == null) {
      inner = context;
      registry = (DefaultListableBeanFactory) inner.getAutowireCapableBeanFactory();
    }
  }

  public static <T> T getInstance(Class<T> clazz) {
    return inner.getBean(clazz);
  }

  public static Object getInstance(String name) {
    return inner.getBean(name);
  }

  public static List<Object> annotationOf(Class<? extends Annotation> annotationClass) {
    return new ArrayList<>(inner.getBeansWithAnnotation(annotationClass).values());
  }

  public static <T> List<T> interfaceOf(Class<T> interfaceClass) {
    return new ArrayList<>(inner.getBeansOfType(interfaceClass).values());
  }

  public static <T> List<T> superClassOf(Class<T> superClass) {
    return interfaceOf(superClass);
  }

  public static <T> T interfaceOf(String name, Class<T> interfaceClass) {
    Map<String, T> beans = inner.getBeansOfType(interfaceClass);
    T bean = beans.get(name);
    if (bean == null) {
      throw new IllegalArgumentException("Missing implementation with name '" + name + "' by interface/super-class of '" + interfaceClass.getName() + "'");
    }
    return bean;
  }

  public static <T> T superClassOf(String name, Class<T> superClass) {
    return interfaceOf(name, superClass);
  }

  public static void inject(String name, Object singleton) {
    registry.registerSingleton(name, singleton);
  }

  public static void inject(Object singleton) {
    inject(name(singleton.getClass()), singleton);
  }

  public static void inject(String name, Class<?> beanClass, Map<String, Object> properties) {
    GenericBeanDefinition definition = new GenericBeanDefinition();
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(beanClass);
    properties.forEach(builder::addPropertyValue);
    registry.registerBeanDefinition(name, definition);
  }

  public static void inject(Class<?> beanClass, Map<String, Object> properties) {
    inject(name(beanClass), beanClass, properties);
  }

  public static void inject(Class<?> beanClass) {
    inject(beanClass, Collections.emptyMap());
  }

  private static String name(Class<?> beanClass) {
    String name = beanClass.getSimpleName();
    if (Character.isUpperCase(name.charAt(0))) {
      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    return name;
  }
}
