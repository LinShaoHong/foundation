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
            return new PropertyImpl(this, null) {
                @Override
                public String name() {
                    return name;
                }
            };
        }
        return properties().stream()
                .filter(Objects::nonNull)
                .filter(p -> p.name().equals(name))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(name() + " unknown '" + name + "'"));
    }

    enum Kind {
        /**
         * Numeric type
         */
        NUMBER,
        /**
         * String type
         */
        TEXT,
        /**
         * True or false
         */
        BOOLEAN,
        /**
         * Datetime
         */
        DATE,
        /**
         * Enumerable type
         */
        ENUM,
        /**
         * Inline object
         */
        OBJECT,
        /**
         * Array of elements
         */
        ARRAY
    }

    interface Property {
        String name();

        Model model();

        Field field();

        Kind kind();

        Type javaType();

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
                            .map(field -> new PropertyImpl(this, field))
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
                            .map(field -> new PropertyImpl(this, field))
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
        private final static Map<Type, Kind> cache = new HashMap<>();

        static {
            Arrays.asList(int.class, long.class, float.class, double.class, byte.class,
                    Integer.class, Long.class, Float.class, Double.class, Byte.class,
                    BigDecimal.class, BigInteger.class).forEach(c -> cache.put(c, Kind.NUMBER));
            cache.put(boolean.class, Kind.BOOLEAN);
            cache.put(Boolean.class, Kind.BOOLEAN);
            cache.put(String.class, Kind.TEXT);
            cache.put(Date.class, Kind.DATE);
        }

        private final Model parent;
        private final Field field;

        private Class<? extends Converter.Handler> typeHandler;
        private String columnPrefix;
        private boolean isJsonPath;
        private Model model;
        private String column;

        protected PropertyImpl(Model parent, Field field) {
            this.parent = parent;
            this.field = field;
        }

        @Override
        public Field field() {
            return field;
        }

        @Override
        public Kind kind() {
            return cache.computeIfAbsent(javaType(), javaType -> {
                Class<?> rawType = field().getType();
                if (rawType.isEnum()) {
                    return Kind.ENUM;
                }
                if (Map.class.isAssignableFrom(rawType)) {
                    return Kind.OBJECT;
                }
                if (Collection.class.isAssignableFrom(rawType)) {
                    return Kind.ARRAY;
                }
                if (Number.class.isAssignableFrom(rawType)) {
                    return Kind.NUMBER;
                }
                return Kind.OBJECT;
            });
        }

        @Override
        public Type javaType() {
            return field().getGenericType();
        }

        @Override
        public String column() {
            if (column == null && field != null) {
                Column c = field().getAnnotation(Column.class);
                if (c != null) {
                    column = c.name();
                } else {
                    Class<?> rawClass = parent == null ? null : parent.rawClass();
                    NamingStrategy a = rawClass == null ? null : rawClass.getAnnotation(NamingStrategy.class);
                    boolean camelCaseToUnderscore = a != null && a.value().startsWith("camelCaseTo");
                    column = camelCaseToUnderscore ? Strings.camelCaseToUnderScore(name(), a.value().contains("Lower")) : name();
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
