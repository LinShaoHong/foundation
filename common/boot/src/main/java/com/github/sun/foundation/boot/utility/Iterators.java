package com.github.sun.foundation.boot.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @Author LinSH
 * @Date: 11:22 AM 2019-04-25
 */
public class Iterators {
  public static String mkString(Iterable<String> values, CharSequence delimiter) {
    return mkString(values, "", delimiter, "");
  }

  public static String mkString(Iterable<String> values, CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
    StringBuilder sb = new StringBuilder();
    sb.append(prefix);
    Iterator<String> it = values.iterator();
    sb.append(it.next());
    while (it.hasNext()) {
      sb.append(delimiter);
      sb.append(it.next());
    }
    sb.append(suffix);
    return sb.toString();
  }

  public static <T> Stream<T> toStream(Iterable<T> values) {
    return StreamSupport.stream(values.spliterator(), false);
  }

  public static <T> List<T> asList(Iterable<T> values) {
    return toStream(values).collect(Collectors.toList());
  }

  public static <T> Collection<T> asCollection(Iterable<T> values) {
    if (values instanceof Collection) {
      return (Collection<T>) values;
    }
    return asList(values);
  }

  public static List<Integer> slice(int count) {
    if (count <= 0) {
      throw new IndexOutOfBoundsException();
    }
    return slice(0, count);
  }

  public static List<Integer> slice(int start, int end) {
    List<Integer> list = new ArrayList<>();
    for (int i = start; i < end; i++) {
      list.add(i);
    }
    return list;
  }
}
