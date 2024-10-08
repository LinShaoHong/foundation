package com.github.sun.foundation.boot.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class JSON {
    private final ObjectMapper mapper = new ObjectMapper();

    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * 序列化
     */
    public String serialize(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte[] serializeAsByte(Object object) {
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public JsonNode asJsonNode(Object obj) {
        return mapper.valueToTree(obj);
    }

    public JsonNode asJsonNode(String json) {
        try {
            return mapper.readTree(json);
        } catch (IOException ex) {
            throw new RuntimeException("Json Format Invalid:\n" + json, ex);
        }
    }

    /**
     * 反序列化
     */
    public <T> T deserialize(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try {
            return mapper.readValue(bytes, clazz);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T> T deserialize(Object object, Class<T> clazz) {
        try {
            return mapper.convertValue(object, clazz);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T> List<T> deserializeAsList(String json, Class<T> clazz) {
        JavaType type = mapper.getTypeFactory().constructParametricType(List.class, clazz);
        try {
            return mapper.readValue(json, type);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T> List<T> deserializeAsList(Object object, Class<T> clazz) {
        JavaType type = mapper.getTypeFactory().constructParametricType(List.class, clazz);
        return mapper.convertValue(object, type);
    }

    public <T> Set<T> deserializeAsSet(String json, Class<T> clazz) {
        JavaType type = mapper.getTypeFactory().constructParametricType(Set.class, clazz);
        try {
            return mapper.readValue(json, type);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T> Set<T> deserializeAsSet(Object object, Class<T> clazz) {
        JavaType type = mapper.getTypeFactory().constructParametricType(Set.class, clazz);
        return mapper.convertValue(object, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] deserializeAsArray(String json, Class<T> clazz) {
        try {
            Class<T[]> arrayClass = (Class<T[]>) Class.forName("[L" + clazz.getName() + ";");
            return mapper.readValue(json, arrayClass);
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] deserializeAsArray(Object object, Class<T> clazz) {
        try {
            Class<T[]> arrayClass = (Class<T[]>) Class.forName("[L" + clazz.getName() + ";");
            return mapper.convertValue(object, arrayClass);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public <A, B> Map<A, B> deserializeAsMap(String json, Class<B> valueClass, Function<String, A> keyProvider) {
        if (json == null) {
            return Collections.emptyMap();
        }
        try {
            Map<String, B> value = mapper.readValue(json, new TypeReference<Map<String, B>>() {
            });
            return value.entrySet().stream()
                    .collect(Collectors.toMap(e -> keyProvider.apply(e.getKey()), e -> deserialize(e.getValue(), valueClass)));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public <B> Map<String, B> deserializeAsMap(String json, Class<B> valueClass) {
        return deserializeAsMap(json, valueClass, v -> v);
    }

    public <B> Map<String, B> deserializeAsMap(JsonNode node, Class<B> valueClass) {
        return deserializeAsMap(node.toString(), valueClass, v -> v);
    }

    public Valuer newValuer(String json, String... path) {
        return newValuer(asJsonNode(json), path);
    }

    public Valuer newValuer(JsonNode node, String... path) {
        return Valuer.of(node, path);
    }

    public static class Valuer {
        private final JsonNode node;
        private final Path path;

        private Valuer(JsonNode node, Path path) {
            this.node = node;
            this.path = path;
        }

        public static Valuer of(JsonNode node, String... path) {
            return new Valuer(node, pathOf(path));
        }

        public JsonNode raw() {
            return node;
        }

        public boolean hasValue() {
            return node != null && !node.isMissingNode() && !node.isNull();
        }

        public Valuer get(String field) {
            JsonNode n = node.get(field);
            if (n == null) {
                n = MissingNode.getInstance();
            }
            return new Valuer(n, new Path(field, path));
        }

        public String asText(String defaultValue) {
            return hasValue() ? node.textValue() : defaultValue;
        }

        public String asText() {
            if (!node.isTextual()) {
                throw error("Expected String but found: " + node.getNodeType().name(), path);
            }
            return node.textValue();
        }

        public int asInt(int defaultValue) {
            return hasValue() ? node.intValue() : defaultValue;
        }

        public int asInt() {
            if (!node.isInt()) {
                throw error("Expected Integer but found: " + node.getNodeType().name(), path);
            }
            return node.intValue();
        }

        public long asLong(long defaultValue) {
            return hasValue() ? node.longValue() : defaultValue;
        }

        public long asLong() {
            if (!node.isLong()) {
                throw error("Expected Long but found: " + node.getNodeType().name(), path);
            }
            return node.longValue();
        }

        public double asDouble(double defaultValue) {
            return hasValue() ? node.doubleValue() : defaultValue;
        }

        public double asDouble() {
            if (!node.isLong()) {
                throw error("Expected Long but found: " + node.getNodeType().name(), path);
            }
            return node.doubleValue();
        }

        public boolean asBoolean(boolean defaultValue) {
            return hasValue() ? node.booleanValue() : defaultValue;
        }

        public boolean asBoolean() {
            if (!node.isBoolean()) {
                throw error("Expected Boolean but found: " + node.getNodeType().name(), path);
            }
            return node.booleanValue();
        }

        public Iterable<Valuer> asArray() {
            if (!hasValue()) {
                return Collections.emptyList();
            }
            if (!node.isArray()) {
                throw error("Expected Array but found: " + node.getNodeType().name(), path);
            }
            Iterator<JsonNode> it = node.iterator();
            return () -> new Iterator<Valuer>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Valuer next() {
                    return new Valuer(it.next(), new Path(String.valueOf(++i), path));
                }
            };
        }

        public <T> T as(Class<T> c) {
            if (!node.isObject()) {
                throw error("Expected Object but found: " + node.getNodeType(), path);
            }
            return deserialize(node, c);
        }
    }

    private static IllegalArgumentException error(String message, Path path) {
        if (path == null) {
            return new IllegalArgumentException(message);
        }
        // print path
        PrependStringBuilder sb = new PrependStringBuilder();
        Path p = path;
        while (p.parent != null) {
            sb.prepend(p.name).prepend(".");
            p = p.parent;
        }
        sb.prepend(p.name);
        // print message
        sb.append(": ").append(message);
        return new IllegalArgumentException(sb.toString());
    }

    private static Path pathOf(String... path) {
        Path p = null;
        if (path != null) {
            for (String n : path) {
                p = new Path(n, p);
            }
        }
        return p;
    }

    private static class Path {
        public final String name;
        public final Path parent;

        private Path(String name, Path parent) {
            this.name = name;
            this.parent = parent;
        }

        public Path sub(String name) {
            return new Path(name, this);
        }
    }
}
