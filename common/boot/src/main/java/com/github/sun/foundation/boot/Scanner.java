package com.github.sun.foundation.boot;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class Scanner {
  private static ScanResult scanResult;

  private Scanner() {
  }

  public static void install(String... basePackages) {
    if (scanResult == null) {
      scanResult = new ClassGraph()
        .enableAllInfo()
        .whitelistPackages(basePackages)
        .scan();
    }
  }

  public static List<ClassTag<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
    return getClasses(scanResult.getClassesWithAnnotation(annotationClass.getName()).loadClasses());
  }

  public static <T> List<ClassTag<? extends T>> getClassesWithInterface(Class<T> interfaceClass) {
    return getClasses(scanResult.getClassesImplementing(interfaceClass.getName()).loadClasses());
  }

  public static <T> List<ClassTag<? extends T>> getSubclassesOf(Class<T> superClass) {
    return getClasses(scanResult.getSubclasses(superClass.getName()).loadClasses());
  }

  public List<Class<?>> getAllInterfaces() {
    return scanResult.getAllInterfaces().stream()
      .map(ClassInfo::loadClass)
      .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private static <A> List<ClassTag<? extends A>> getClasses(List<Class<?>> classes) {
    return classes.stream()
      .map(c -> (ClassTag<? extends A>) new ClassTagImpl(c))
      .collect(Collectors.toList());
  }

  public interface ClassTag<T> {
    Class<T> runtimeClass();

    T getInstance();

    default boolean isImplementClass() {
      Class<T> c = runtimeClass();
      return !Modifier.isAbstract(c.getModifiers())
        && !Modifier.isInterface(c.getModifiers());
    }
  }

  private static class ClassTagImpl<T> implements ClassTag<T> {
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
        return clazz.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
