package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class Iterators {
  public <A, B> Iterable<B> map(Iterable<A> values, Function<A, B> func) {
    Iterator<A> it = values.iterator();
    return () -> new Iterator<B>() {
      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public B next() {
        A v = it.next();
        return func.apply(v);
      }
    };
  }

  public <T> String mkString(Iterable<T> values, CharSequence delimiter) {
    return mkString(values, delimiter, String::valueOf);
  }

  public <T> String mkString(Iterable<T> values, CharSequence delimiter, Function<T, String> func) {
    return mkString(values, "", delimiter, "", func);
  }

  public <T> String mkString(Iterable<T> values, CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
    return mkString(values, prefix, delimiter, suffix, String::valueOf);
  }

  public <T> String mkString(Iterable<T> values, CharSequence prefix, CharSequence delimiter, CharSequence suffix, Function<T, String> func) {
    StringBuilder sb = new StringBuilder();
    Iterator<T> it = values.iterator();
    if (it.hasNext()) {
      sb.append(prefix);
      sb.append(func.apply(it.next()));
      while (it.hasNext()) {
        sb.append(delimiter);
        sb.append(func.apply(it.next()));
      }
      sb.append(suffix);
    }
    return sb.toString();
  }

  public <T> Stream<T> toStream(Iterable<T> values) {
    return StreamSupport.stream(values.spliterator(), false);
  }

  public <T> List<T> asList(Iterable<T> values) {
    return toStream(values).collect(Collectors.toList());
  }

  public <T> Collection<T> asCollection(Iterable<T> values) {
    if (values instanceof Collection) {
      return (Collection<T>) values;
    }
    return asList(values);
  }

  public List<Integer> slice(int count) {
    if (count <= 0) {
      throw new IndexOutOfBoundsException();
    }
    return slice(0, count);
  }

  public List<Integer> slice(int start, int end) {
    List<Integer> list = new ArrayList<>();
    for (int i = start; i < end; i++) {
      list.add(i);
    }
    return list;
  }
}
