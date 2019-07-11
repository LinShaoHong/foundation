package com.github.sun.foundation.boot.utility;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author LinSH
 * @Date: 1:18 PM 2019-07-10
 */
public interface Scanner {
  List<ClassTag<?>>

  getClassesWithAnnotation(Class<? extends Annotation> annotationClass);

  <T> List<ClassTag<? extends T>> getClassesWithInterface(Class<T> interfaceClass);

  <T> List<ClassTag<? extends T>> getSubclassesOf(Class<T> superClass);

  List<Class<?>> getAllInterfaces();

  interface ClassTag<T> {
    Class<T> runtimeClass();

    T getInstance();

    default boolean isImplementClass() {
      Class<T> c = runtimeClass();
      return !Modifier.isAbstract(c.getModifiers())
        && !Modifier.isInterface(c.getModifiers());
    }
  }

  Map<Set<String>, Scanner> cache = new HashMap<>();

  static Scanner create(String... basePackages) {
    Set<String> key = Arrays.stream(basePackages)
      .collect(Collectors.toSet());
    return cache.computeIfAbsent(key, set -> {
      ScanResult scanResult = new ClassGraph()
        .enableAllInfo()
        .whitelistPackages(basePackages)
        .scan();
      return new ScannerImpl(scanResult);
    });
  }

  class ScannerImpl implements Scanner {
    private final ScanResult scanResult;

    public ScannerImpl(ScanResult scanResult) {
      this.scanResult = scanResult;
    }

    @Override
    public List<ClassTag<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
      return getClasses(scanResult.getClassesWithAnnotation(annotationClass.getName()).loadClasses());
    }

    @Override
    public <T> List<ClassTag<? extends T>> getClassesWithInterface(Class<T> interfaceClass) {
      return getClasses(scanResult.getClassesImplementing(interfaceClass.getName()).loadClasses());
    }

    @Override
    public <T> List<ClassTag<? extends T>> getSubclassesOf(Class<T> superClass) {
      return getClasses(scanResult.getSubclasses(superClass.getName()).loadClasses());
    }

    @Override
    public List<Class<?>> getAllInterfaces() {
      return scanResult.getAllInterfaces().stream()
        .map(ClassInfo::loadClass)
        .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <A> List<ClassTag<? extends A>> getClasses(List<Class<?>> classes) {
      return classes.stream()
        .map(c -> (ClassTag<? extends A>) new ClassTagImpl(c))
        .collect(Collectors.toList());
    }
  }

  class ClassTagImpl<T> implements ClassTag<T> {
    private final Class<T> clazz;

    private ClassTagImpl(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Override
    public Class<T> runtimeClass() {
      return clazz;
    }

    @Override
    public T getInstance() {
      if (!isImplementClass()) {
        throw new RuntimeException(clazz.getName() + " is not a implementation class");
      }
      try {
        return clazz.newInstance();
      } catch (InstantiationException | IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
