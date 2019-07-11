package com.github.sun.foundation.sql;

import com.github.sun.foundation.boot.utility.Strings;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author LinSH
 * @Date: 7:40 PM 2019-02-28
 */
public interface Model {
  default String name() {
    return rawClass().getName();
  }

  Class<?> rawClass();

  String tableName();

  List<Property> properties();

  List<Property> primaryProperties();

  List<Property> transientProperties();

  List<Property> persistenceProperties();

  default String column(String field) {
    return Strings.camelCaseToUnderScore(field);
  }

  default Property findProperty(String name) {
    if ("*".equals(name) || Strings.isInt(name)) {
      return new PropertyImpl(null) {
        @Override
        public String name() {
          return name;
        }
      };
    }
    return persistenceProperties().stream()
      .filter(p -> p.name().equals(name))
      .findAny()
      .orElseThrow(() -> new IllegalArgumentException(name() + " unknown '" + name + "'"));
  }

  interface Property {
    String name();

    Field field();

    boolean isJsonPath();

    void isJsonPath(boolean isJsonPath);

    default boolean hasJsonPath() {
      return true;
    }

    String selectAliasPrefix();

    Class<?> typeHandler();

    boolean hasAnnotation(Class<? extends Annotation> annotationClass);
  }

  static Model from(Class<?> entityClass) {
    return new ModelImpl(entityClass);
  }

  class ModelImpl implements Model {
    private final Class<?> entityClass;

    private String tableName;
    private List<Property> properties;
    private List<Property> primaryProperties;
    private List<Property> transientProperties;
    private List<Property> persistenceProperties;

    public ModelImpl(Class<?> entityClass) {
      this.entityClass = entityClass;
    }

    @Override
    public List<Property> primaryProperties() {
      if (primaryProperties == null) {
        primaryProperties = persistenceProperties().stream()
          .filter(p -> p.hasAnnotation(Id.class)).collect(Collectors.toList());
        primaryProperties = Collections.unmodifiableList(primaryProperties);
      }
      return primaryProperties;
    }

    @Override
    public List<Property> persistenceProperties() {
      if (persistenceProperties == null) {
        if (hasProperties()) {
          final int ignoreModifier = Modifier.FINAL | Modifier.STATIC |
            Modifier.VOLATILE | Modifier.TRANSIENT;
          persistenceProperties = Stream.of(entityClass.getDeclaredFields())
            .filter(f -> !f.getName().contains("$")
              && !f.isAnnotationPresent(Transient.class)
              && (f.getModifiers() & ignoreModifier) <= 0)
            .map(PropertyImpl::new)
            .collect(Collectors.toList());
          persistenceProperties = Collections.unmodifiableList(persistenceProperties);
        } else {
          persistenceProperties = Collections.emptyList();
        }
      }
      return persistenceProperties;
    }

    @Override
    public Class<?> rawClass() {
      return entityClass;
    }

    @Override
    public String tableName() {
      if (tableName == null) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null) {
          tableName = table.name();
        }
      }
      return tableName;
    }

    @Override
    public List<Property> properties() {
      if (properties == null) {
        properties = new ArrayList<>();
        properties.addAll(primaryProperties());
        properties.addAll(persistenceProperties());
        properties.addAll(transientProperties());
        properties = Collections.unmodifiableList(properties);
      }
      return properties;
    }

    @Override
    public List<Property> transientProperties() {
      if (transientProperties == null) {
        if (hasProperties()) {
          transientProperties = Stream.of(entityClass.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(Transient.class))
            .map(PropertyImpl::new)
            .collect(Collectors.toList());
          transientProperties = Collections.unmodifiableList(transientProperties);
        } else {
          transientProperties = Collections.emptyList();
        }
      }
      return transientProperties;
    }

    private boolean hasProperties() {
      if (entityClass == null) {
        return false;
      }
      List<Class<?>> nonPropertyTypes = Arrays.asList(Integer.class, Long.class, Float.class, Double.class,
        Short.class, BigInteger.class, BigDecimal.class, Boolean.class, Byte.class, Void.class, String.class,
        Map.class, Date.class, java.sql.Date.class, Time.class, Timestamp.class);
      return !nonPropertyTypes.contains(entityClass);
    }
  }

  class PropertyImpl implements Property {
    private Class<?> typeHandler;
    private String selectAliasPrefix;
    private boolean isJsonPath;
    private final Field field;

    public PropertyImpl(Field field) {
      this.field = field;
    }

    @Override
    public Field field() {
      return field;
    }

    @Override
    public boolean isJsonPath() {
      return isJsonPath;
    }

    @Override
    public void isJsonPath(boolean isJsonPath) {
      this.isJsonPath = isJsonPath;
    }

    @Override
    public String name() {
      return field.getName();
    }

    @Override
    public Class<?> typeHandler() {
      if (field != null && typeHandler == null) {
        Handler handler = field.getAnnotation(Handler.class);
        if (handler != null) {
          typeHandler = handler.value();
        }
      }
      return typeHandler;
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
      return field.isAnnotationPresent(annotationClass);
    }

    @Override
    public String selectAliasPrefix() {
      if (field != null && selectAliasPrefix == null) {
        SelectAliasPrefix a = field.getAnnotation(SelectAliasPrefix.class);
        selectAliasPrefix = a == null ? null : a.value();
      }
      return selectAliasPrefix;
    }
  }
}
