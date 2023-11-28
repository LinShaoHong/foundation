package com.github.sun.foundation.boot.utility;

import com.github.sun.foundation.boot.Injector;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Reflections {
  private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();
  private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

  @SuppressWarnings("Duplicates")
  public static Object getValue(Object obj, Field field) {
    synchronized (field.getType()) {
      boolean accessible = field.canAccess(obj);
      try {
        if (!accessible) {
          field.setAccessible(true);
        }
        return field.get(obj);
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      } finally {
        if (!accessible) {
          field.setAccessible(false);
        }
      }
    }
  }

  public static Object getValue(Object obj, String field) {
    Field f = getField(obj.getClass(), field);
    return f == null ? null : getValue(obj, f);
  }

  public static Field getField(Class<?> clazz, String name) {
    String key = clazz.getName() + ":" + name;
    if (FIELDS.containsKey(key)) {
      return FIELDS.get(key);
    }
    Class<?> _clazz = clazz;
    Field field = null;
    while (_clazz != Object.class) {
      try {
        field = _clazz.getDeclaredField(name);
        break;
      } catch (NoSuchFieldException ex) {
        _clazz = _clazz.getSuperclass();
      }
    }
    if (field != null) {
      FIELDS.put(key, field);
    }
    return field;
  }

  @SuppressWarnings("Duplicates")
  public static void setValue(Object obj, Field field, Object value) {
    synchronized (field.getType()) {
      boolean accessible = field.canAccess(obj);
      try {
        if (!accessible) {
          field.setAccessible(true);
        }
        if (value != null) {
          field.set(obj, value);
        }
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      } finally {
        if (!accessible) {
          field.setAccessible(false);
        }
      }
    }
  }

  public static void setValue(Object obj, String field, Object value) {
    Field f = getField(obj.getClass(), field);
    if (f != null) {
      setValue(obj, f, value);
    }
  }

  public static Method getMethod(Class<?> clazz, String name) {
    String key = clazz.getName() + ":" + name;
    if (METHODS.containsKey(key)) {
      return METHODS.get(key);
    }
    Method method = Arrays.stream(clazz.getMethods())
      .filter(m -> Objects.equals(m.getName(), name)).findFirst().orElse(null);
    METHODS.put(key, method);
    return method;
  }

  public static Object invoke(Object obj, Method method, Object... args) {
    try {
      return method.invoke(obj, args);
    } catch (IllegalAccessException | InvocationTargetException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Object invoke(Object obj, String method, Object... args) {
    Method m = getMethod(obj.getClass(), method);
    return invoke(obj, m, args);
  }

  public static Object newInstance(Class<?> clazz) {
    try {
      return Injector.getInstance(clazz);
    } catch (NoSuchBeanDefinitionException e) {
      try {
        return clazz.getDeclaredConstructor().newInstance();
      } catch (InstantiationException |
               IllegalAccessException |
               InvocationTargetException |
               NoSuchMethodException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}