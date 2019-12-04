package com.github.sun.foundation.modelling;

import com.github.sun.foundation.boot.utility.Cache;
import com.github.sun.foundation.boot.utility.Strings;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

  default boolean hasProperty() {
    return !properties().isEmpty();
  }

  List<Property> properties();

  List<Property> primaryProperties();

  List<Property> transientProperties();

  List<Property> persistenceProperties();

  default String column(String field) {
    Property property = persistenceProperties().stream()
      .filter(p -> p.name().equals(field))
      .findFirst()
      .orElse(null);
    String column = property == null ? null : property.column();
    if (column == null) {
      NamingStrategy a = rawClass() == null ? null : rawClass().getAnnotation(NamingStrategy.class);
      boolean camelCaseToUnderscore = a != null && a.value().startsWith("camelCaseTo");
      column = camelCaseToUnderscore ? Strings.camelCaseToUnderScore(field, a.value().contains("Lower")) : field;
    }
    return column;
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

    Model model();

    Field field();

    String column();

    boolean isJsonPath();

    void isJsonPath(boolean isJsonPath);

    default boolean hasJsonPath() {
      return true;
    }

    String columnPrefix();

    Class<? extends Converter.Handler> typeHandler();

    boolean hasAnnotation(Class<? extends Annotation> annotationClass);

    Object getValue(Object obj);
  }

  Cache<Class<?>, Model> cache = new Cache<>();

  static Model from(Class<?> entityClass) {
    return cache.get(entityClass, () -> new ModelImpl(entityClass));
  }

  class ModelImpl implements Model {
    private final Class<?> entityClass;

    private String tableName;
    private List<Property> properties;
    private List<Property> primaryProperties;
    private List<Property> transientProperties;
    private List<Property> persistenceProperties;

    protected ModelImpl(Class<?> entityClass) {
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
    private final Field field;

    private Class<? extends Converter.Handler> typeHandler;
    private String columnPrefix;
    private boolean isJsonPath;
    private Model model;
    private String column;

    protected PropertyImpl(Field field) {
      this.field = field;
    }

    @Override
    public Field field() {
      return field;
    }

    @Override
    public String column() {
      if (column == null && field != null) {
        Column c = field().getAnnotation(Column.class);
        if (c != null) {
          column = c.name();
        }
      }
      return column;
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
    public Model model() {
      if (field != null && model == null) {
        Class<?> actualType = resultType(field.getGenericType());
        if (actualType != null) {
          model = from(actualType);
        }
      }
      return model;
    }

    @Override
    public Class<? extends Converter.Handler> typeHandler() {
      if (field != null && typeHandler == null) {
        Converter converter = field.getAnnotation(Converter.class);
        if (converter != null) {
          typeHandler = converter.value();
        }
      }
      return typeHandler;
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
      return field.isAnnotationPresent(annotationClass);
    }

    @Override
    public Object getValue(Object obj) {
      if (field != null) {
        boolean accessible = field.isAccessible();
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
      return null;
    }

    @Override
    public String columnPrefix() {
      if (field != null && columnPrefix == null) {
        ColumnPrefix a = field.getAnnotation(ColumnPrefix.class);
        columnPrefix = a == null ? null : a.value();
      }
      return columnPrefix;
    }
  }

  static Class<?> resultType(Type type) {
    if (type instanceof ParameterizedType) {
      Class<?> rawType = (Class<?>) ((ParameterizedType) type).getRawType();
      if (rawType.isAssignableFrom(Map.class)) {
        return Map.class;
      }
      if (Iterable.class.isAssignableFrom(rawType)) {
        Type t = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (t instanceof ParameterizedType) {
          return resultType(((ParameterizedType) t).getRawType());
        }
        return resultType(t);
      }
    } else if (type instanceof Class) {
      Class<?> c = (Class<?>) type;
      if (c.isArray()) { // array
        return c.getComponentType();
      }
      return c;
    }
    return null;
  }
}
