package com.github.sun.foundation.boot;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author LinSH
 * @Date: 3:46 PM 2019-03-02
 */
public class TypeInfo {
  /**
   * 构造一个参数化的类型
   *
   * @param rawType 范型声明类型
   * @param args    范型参数列表
   * @return 参数化的类型
   */
  public static ParameterizedType makeGenericType(Class<?> rawType, Type... args) {
    return new ParameterizedType() {
      @Override
      public Type[] getActualTypeArguments() {
        return args;
      }

      @Override
      public Type getRawType() {
        return rawType;
      }

      @Override
      public Type getOwnerType() {
        return rawType.getEnclosingClass();
      }

      @Override
      public int hashCode() {
        return Objects.hashCode(getRawType()) ^ Objects.hashCode(getOwnerType()) ^ Arrays.deepHashCode(getActualTypeArguments());
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof ParameterizedType)) {
          return false;
        }
        ParameterizedType pt = (ParameterizedType) obj;
        if (rawType != pt.getRawType()) {
          return false;
        }
        return Arrays.deepEquals(args, pt.getActualTypeArguments());
      }
    };
  }

  /**
   * 对于基类baseClass,获取其继承(实现)类型type所使用的范型参数
   *
   * @param type      类型
   * @param baseClass 基类
   * @return 基类的范型参数
   */
  public static List<Type> getTypeParameters(Type type, Class<?> baseClass) {
    return new TypeParametersFinder().find(type, baseClass);
  }

  private static class TypeParametersFinder {
    private Class<?> c;
    private ParameterizedType pt;
    private Map<String, Type> typeArguments = new HashMap<>();

    private void parse(Type type) {
      if (type instanceof ParameterizedType) {
        pt = (ParameterizedType) type;
        c = (Class<?>) pt.getRawType();
      } else if (type instanceof Class) {
        pt = null;
        c = (Class<?>) type;
      } else {
        throw new IllegalArgumentException();
      }
    }

    private void updateArgs() {
      if (pt == null) {
        typeArguments.clear();
      } else {
        TypeVariable<?>[] variables = c.getTypeParameters();
        Type[] args = pt.getActualTypeArguments();
        for (int i = 0; i < variables.length; i++) {
          typeArguments.put(variables[i].getName(), args[i]);
        }
      }
    }

    private List<Type> getResult() {
      return Arrays.stream(c.getTypeParameters())
        .map(v -> typeArguments.get(v.getName()))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    }

    private List<Type> findInSuperClasses(Type type, Class<?> baseClass) {
      Type t = type;
      for (; ; ) {
        parse(t);
        updateArgs();
        if (c.getSuperclass() == null || !baseClass.isAssignableFrom(c.getSuperclass())) {
          break;
        }
        t = c.getGenericSuperclass();
      }
      if (c == baseClass) {
        return getResult();
      }
      return null;
    }

    private List<Type> findInSuperInterfaces(Class<?> baseClass) {
      for (; ; ) {
        boolean found = false;
        for (Type t : c.getGenericInterfaces()) {
          parse(t);
          if (baseClass.isAssignableFrom(c)) {
            found = true;
            updateArgs();
            break;
          }
        }
        if (!found) {
          return null;
        }
        if (c == baseClass) {
          return getResult();
        }
      }
    }

    private List<Type> find(Type type, Class<?> baseClass) {
      List<Type> types = findInSuperClasses(type, baseClass);
      if (types == null) {
        types = findInSuperInterfaces(baseClass);
      }
      return types;
    }
  }
}
