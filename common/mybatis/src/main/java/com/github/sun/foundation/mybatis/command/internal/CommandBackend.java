package com.github.sun.foundation.mybatis.command.internal;

import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.Tuple;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.mybatis.command.CommandRunner;
import com.github.sun.foundation.sql.Model;
import com.github.sun.foundation.sql.SqlBuilder;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CommandBackend<V> implements CommandRunner<V> {
  private final SqlBuilder.Factory factory;

  public CommandBackend(SqlBuilder.Factory factory) {
    this.factory = factory;
  }

  @Override
  public SqlBuilder.Template insert(Iterable<V> values) {
    Collection<V> arr = Iterators.asCollection(values);
    if (arr.isEmpty()) {
      throw new IllegalArgumentException("values is empty");
    }
    Class<?> clazz = values.iterator().next().getClass();
    Model model = Model.from(clazz);
    SqlBuilder sb = factory.create();
    SqlBuilder.UpdateAble insertAble = sb.from(clazz).insert();
    arr.forEach(value -> {
      model.persistenceProperties().forEach(p -> insertAble.set(p.name(), p.getValue(value)));
      insertAble.ending();
    });
    return insertAble.template();
  }

  @Override
  public SqlBuilder.Template update(Iterable<V> values) {
    Collection<V> arr = Iterators.asCollection(values);
    if (arr.isEmpty()) {
      throw new IllegalArgumentException("values is empty");
    }
    V head = values.iterator().next();
    Class<?> clazz = head.getClass();
    Model model = Model.from(clazz);
    List<Model.Property> pks = model.primaryProperties();
    if (pks.isEmpty()) {
      throw new IllegalArgumentException(clazz.getName() + " missing primary keys");
    }
    List<Model.Property> updateProperties = model.persistenceProperties().stream()
      .filter(p -> pks.stream().noneMatch(pk -> pk.name().equals(p.name())))
      .collect(Collectors.toList());

    SqlBuilder sb = factory.create();
    return sb.from(clazz)
      .where(pks, pk -> {
        List<Object> pkValues = arr.stream()
          .map(pk::getValue)
          .distinct()
          .collect(Collectors.toList());
        return pkValues.size() > 1 ? sb.field(pk.name()).in(pkValues) : sb.field(pk.name()).eq(pkValues.get(0));
      })
      .update()
      .set(updateProperties, p -> {
        if (arr.size() == 1) {
          return Tuple.of(p.name(), p.getValue(head));
        } else {
          Expression.CaseAble caseAble = sb.Case();
          arr.forEach(value -> {
            Expression expr = null;
            for (Model.Property pk : pks) {
              Expression e = sb.field(pk.name()).eq(pk.getValue(value));
              expr = expr == null ? e : expr.and(e);
            }
            caseAble.when(expr).then(p.name(), p.getValue(value));
          });
          return Tuple.of(p.name(), caseAble.Else());
        }
      }).template();
  }

  @Override
  public SqlBuilder.Template delete(Iterable<V> values) {
    Collection<V> arr = Iterators.asCollection(values);
    if (arr.isEmpty()) {
      throw new IllegalArgumentException("values is empty");
    }
    Class<?> clazz = values.iterator().next().getClass();
    Model model = Model.from(clazz);
    List<Model.Property> pks = model.primaryProperties();
    if (pks.isEmpty()) {
      throw new IllegalArgumentException(clazz.getName() + " missing primary keys");
    }
    SqlBuilder sb = factory.create();
    return sb.from(clazz)
      .where(pks, pk -> {
        List<Object> pkValues = arr.stream()
          .map(pk::getValue)
          .distinct()
          .collect(Collectors.toList());
        return pkValues.size() > 1 ? sb.field(pk.name()).in(pkValues) : sb.field(pk.name()).eq(pkValues.get(0));
      }).delete().template();
  }
}
