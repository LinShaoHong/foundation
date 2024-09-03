package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.exception.BadRequestException;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Provider
public class SetParameterConverter implements ParamConverterProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if ((rawType == Set.class || rawType == List.class)
                && genericType instanceof ParameterizedType) {
            boolean set = rawType == Set.class;
            Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            ParamConverter<?> converter = null;
            if (type == String.class) {
                converter = set ? new SetStringConverter() : new ListStringConverter();
            } else if (type == Integer.class) {
                converter = set ? new SetIntConverter() : new ListIntConverter();
            } else if (type == Long.class) {
                converter = set ? new SetLongConverter() : new ListLongConverter();
            }
            return (ParamConverter<T>) converter;
        }
        return null;
    }

    private static abstract class SetConverter<T> implements ParamConverter<Set<T>> {
        protected abstract Set<T> parse(Set<String> set);

        @Override
        public Set<T> fromString(String value) {
            if (value == null) {
                throw new BadRequestException("缺少参数");
            }
            if (value.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> set = Stream.of(value.split(",")).collect(Collectors.toSet());
            return parse(set);
        }

        @Override
        public String toString(Set<T> value) {
            return value.stream().map(Object::toString).collect(Collectors.joining(","));
        }
    }

    private static abstract class ListConverter<T> implements ParamConverter<List<T>> {
        protected abstract List<T> parse(List<String> set);

        @Override
        public List<T> fromString(String value) {
            if (value == null) {
                throw new BadRequestException("缺少参数");
            }
            if (value.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> set = Stream.of(value.split(",")).collect(Collectors.toList());
            return parse(set);
        }

        @Override
        public String toString(List<T> value) {
            return value.stream().map(Object::toString).collect(Collectors.joining(","));
        }
    }

    private static class SetStringConverter extends SetConverter<String> {
        @Override
        protected Set<String> parse(Set<String> set) {
            return set;
        }
    }

    private static class SetIntConverter extends SetConverter<Integer> {
        @Override
        protected Set<Integer> parse(Set<String> set) {
            return set.stream().map(Integer::valueOf).collect(Collectors.toSet());
        }
    }

    private static class SetLongConverter extends SetConverter<Long> {
        @Override
        protected Set<Long> parse(Set<String> set) {
            return set.stream().map(Long::valueOf).collect(Collectors.toSet());
        }
    }

    private static class ListStringConverter extends ListConverter<String> {
        @Override
        protected List<String> parse(List<String> set) {
            return set;
        }
    }

    private static class ListIntConverter extends ListConverter<Integer> {
        @Override
        protected List<Integer> parse(List<String> set) {
            return set.stream().map(Integer::valueOf).collect(Collectors.toList());
        }
    }

    private static class ListLongConverter extends ListConverter<Long> {
        @Override
        protected List<Long> parse(List<String> set) {
            return set.stream().map(Long::valueOf).collect(Collectors.toList());
        }
    }
}
