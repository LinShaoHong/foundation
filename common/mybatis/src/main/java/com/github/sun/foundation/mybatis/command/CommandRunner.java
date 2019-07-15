package com.github.sun.foundation.mybatis.command;

import com.github.sun.foundation.sql.SqlBuilder;

import java.util.Collections;

public interface CommandRunner<V> {
  SqlBuilder.Template insert(Iterable<V> values);

  default SqlBuilder.Template insert(V value) {
    return insert(Collections.singletonList(value));
  }

  SqlBuilder.Template update(Iterable<V> values);

  default SqlBuilder.Template update(V value) {
    return update(Collections.singletonList(value));
  }

  SqlBuilder.Template delete(Iterable<V> values);

  default SqlBuilder.Template delete(V value) {
    return delete(Collections.singletonList(value));
  }
}
