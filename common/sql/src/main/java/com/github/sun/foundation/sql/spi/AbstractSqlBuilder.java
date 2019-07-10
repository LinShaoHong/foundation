package com.github.sun.foundation.sql.spi;


import com.github.sun.foundation.boot.Tuple;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.JoinMode;
import com.github.sun.foundation.sql.Model;
import com.github.sun.foundation.sql.OrderMode;
import com.github.sun.foundation.sql.SqlBuilder;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author LinSH
 * @Date: 下午10:37 2019-02-28
 */
public abstract class AbstractSqlBuilder implements SqlBuilder.StatelessSqlBuilder, Cloneable {
  private Stack<AbstractSqlBuilder> delegates;
  private Map<String, Model> subModelCache = new HashMap<>();
  private final static Map<Class<?>, Model> modelCache = new HashMap<>();
  private final static Map<String, Model> dimsModelCache = new HashMap<>();

  protected Counter counter;
  protected Context context;
  private Map<Expression, List<Select>> subSelects;

  protected Model mainModel;
  protected From from;
  protected String subAlias;
  protected List<Join> joins;
  protected List<Expression> conditions;
  protected List<Expression> groups;
  protected Expression having;
  protected List<String> deletes;
  protected List<Order> orders;
  protected Limit limit;
  protected List<Select> selects;
  protected boolean distinct;
  protected boolean count;
  protected boolean forUpdate;
  protected boolean insert;
  protected Expression subQueryExpressionForInsertion;
  protected boolean delete;
  protected List<Map<String, Expression>> updateSets;
  protected Template template;

  @Override
  public void clear() {
    subModelCache.clear();
    this.delegates = null;
    this.counter = null;
    this.subSelects = null;
    this.context = null;
    clean();
  }

  private void clean() {
    this.mainModel = null;
    this.from = null;
    this.subAlias = null;
    this.joins = null;
    this.conditions = null;
    this.groups = null;
    this.having = null;
    this.deletes = null;
    this.orders = null;
    this.limit = null;
    this.selects = null;
    this.distinct = false;
    this.count = false;
    this.forUpdate = false;
    this.insert = false;
    this.subQueryExpressionForInsertion = null;
    this.delete = false;
    this.updateSets = null;
    this.template = null;
  }

  private void push() {
    if (counter == null) {
      counter = new Counter();
    }
    if (delegates == null) {
      delegates = new Stack<>();
    }
    delegates.push(this.clone());
    clean();
  }

  private void pop() {
    if (!delegates.isEmpty()) {
      AbstractSqlBuilder delegated = delegates.pop();
      this.mainModel = delegated.mainModel;
      this.from = delegated.from;
      this.subAlias = delegated.subAlias;
      this.joins = delegated.joins;
      this.conditions = delegated.conditions;
      this.groups = delegated.groups;
      this.having = delegated.having;
      this.deletes = delegated.deletes;
      this.orders = delegated.orders;
      this.limit = delegated.limit;
      this.selects = delegated.selects;
      this.distinct = delegated.distinct;
      this.count = delegated.count;
      this.forUpdate = delegated.forUpdate;
      this.insert = delegated.insert;
      this.subQueryExpressionForInsertion = delegated.subQueryExpressionForInsertion;
      this.delete = delegated.delete;
      this.updateSets = delegated.updateSets;
      this.template = delegated.template;
    }
  }

  protected Model model(Class<?> entityClass) {
    return modelCache.computeIfAbsent(entityClass, Model::from);
  }

  private Model model(String tableName) {
    return dimsModelCache.computeIfAbsent(tableName, table -> {
      if (table != null) {
        return new Model.ModelImpl(null) {
          @Override
          public String tableName() {
            return table;
          }

          @Override
          public Property findProperty(String name) {
            return new PropertyImpl(null) {
              @Override
              public String name() {
                return name;
              }

              @Override
              public boolean hasJsonPath() {
                return false;
              }
            };
          }
        };
      }
      return null;
    });
  }

  private Model model(List<Select> selects, String subAlias) {
    return subModelCache.computeIfAbsent(subAlias, a -> new Model.ModelImpl(null) {
      @Override
      public List<Property> persistenceProperties() {
        if (selects != null) {
          return selects.stream().map(select -> new PropertyImpl(null) {
            @Override
            public String name() {
              return select.alias;
            }

            @Override
            public boolean hasJsonPath() {
              return false;
            }
          }).collect(Collectors.toList());
        }
        return Collections.emptyList();
      }

      @Override
      public Property findProperty(String name) {
        if (selects == null) {
          name = "*";
        }
        return super.findProperty(name);
      }
    });
  }

  protected Model model(From from) {
    if (from.entityClass() != null) {
      return model(from.entityClass());
    } else if (from.tableName() != null) {
      return model(from.tableName());
    } else {
      return model(subSelects.get(from.subQueryExpression()), from.alias);
    }
  }

  protected Model model(Join join) {
    if (join.joinedClass() != null) {
      return model(join.joinedClass());
    } else if (join.joinedTable() != null) {
      return model(join.joinedTable());
    } else {
      return model(subSelects.get(join.subQueryExpression()), join.alias);
    }
  }

  protected class Counter {
    private int count = 1;

    public int next() {
      return count++;
    }
  }

  private static class Context {
    private Map<String, From> allFroms = new HashMap<>();
    private Map<String, Join> allJoins = new HashMap<>();
  }

  private void putFrom(From from) {
    if (context == null) {
      context = new Context();
    }
    context.allFroms.put(from.alias, from);
  }

  private void putJoin(Join join) {
    if (context == null) {
      context = new Context();
    }
    context.allJoins.put(join.alias, join);
  }

  public Model byAliasFromContext(String alias) {
    if (context == null) {
      context = new Context();
    }
    From from = context.allFroms.get(alias);
    if (from != null) {
      return model(from);
    }
    Join join = context.allJoins.get(alias);
    return join == null ? null : model(join);
  }

  @Override
  public JoinAble from(Class<?> entityClass, String tableAlias) {
    push();
    if (this.from == null) {
      from = new TypedFrom(entityClass, tableAlias);
    }
    this.mainModel = model(entityClass);
    if (mainModel.tableName() == null) {
      throw new IllegalArgumentException(mainModel.name() + " not specify @Table");
    }
    if (tableAlias != null) {
      putFrom(from);
    }
    return this;
  }

  @Override
  public JoinAble from(String table, String tableAlias) {
    if (table == null) {
      throw new IllegalArgumentException("tableName is null");
    }
    push();
    if (this.from == null) {
      from = new DimsFrom(table, tableAlias);
    }
    this.mainModel = model(table);
    if (tableAlias != null) {
      putFrom(from);
    }
    return this;
  }

  @Override
  public JoinAble from(Expression subQueryExpression, String subAlias) {
    if (subAlias == null) {
      throw new IllegalArgumentException("subQuery at from-clause must specify a alias");
    }
    push();
    if (this.from == null) {
      this.from = new SubFrom(subQueryExpression, subAlias);
    }
    this.subAlias = subAlias;
    List<Select> selects = subSelects.get(subQueryExpression);
    if (selects != null && !selects.isEmpty()) {
      this.mainModel = model(selects, subAlias);
      putFrom(from);
    }
    return this;
  }

  @Override
  public void putFroms(Map<String, From> froms) {
    if (context == null) {
      context = new Context();
    }
    context.allFroms.putAll(froms);
  }

  @Override
  public JoinAble from(From from) {
    push();
    this.from = from;
    this.mainModel = model(from);
    if (from.alias != null) {
      putFrom(from);
    }
    return this;
  }

  @Override
  public JoinAble join(Class<?> joinedClass, String alias, JoinMode model, Expression on) {
    if (joinedClass == null) return this;
    if (joins == null) {
      joins = new ArrayList<>();
    }
    if (alias == null) {
      throw new IllegalArgumentException("join alias cannot be null");
    }
    joins.add(joinOf(joinedClass, alias, model, on));
    putJoin(joinOf(joinedClass, alias, model, on));
    return this;
  }

  @Override
  public <T> JoinAble join(Iterable<T> values, Function<T, Join> func) {
    if (values == null) return this;
    if (joins == null) {
      joins = new ArrayList<>();
    }
    for (T value : values) {
      Join join = func.apply(value);
      if (join != null) {
        if (join.alias == null) {
          throw new IllegalArgumentException("join alias cannot be null");
        }
        joins.add(join);
        putJoin(func.apply(value));
      }
    }
    return this;
  }

  @Override
  public JoinAble join(String joinedTable, String alias, JoinMode mode, Expression on) {
    if (joinedTable == null) return this;
    if (joins == null) {
      joins = new ArrayList<>();
    }
    if (alias == null) {
      throw new IllegalArgumentException("join alias cannot be null");
    }
    joins.add(new DimsJoin(joinedTable, alias, mode, on));
    putJoin(new DimsJoin(joinedTable, alias, mode, on));
    return this;
  }

  @Override
  public JoinAble join(Expression subQueryExpression, String alias, JoinMode model, Expression on) {
    if (subQueryExpression == null) return this;
    if (joins == null) {
      joins = new ArrayList<>();
    }
    if (alias == null) {
      throw new IllegalArgumentException("join alias cannot be null");
    }
    joins.add(new SubJoin(subQueryExpression, alias, model, on));
    putJoin(new SubJoin(subQueryExpression, alias, model, on));
    return this;
  }

  @Override
  public List<Join> joins() {
    return joins == null ? Collections.emptyList() : Collections.unmodifiableList(joins);
  }

  @Override
  public void putJoins(Map<String, Join> joins) {
    if (context == null) {
      context = new Context();
    }
    context.allJoins.putAll(joins);
  }

  @Override
  public UpdateAble insert() {
    this.insert = true;
    return this;
  }

  @Override
  public TemplateBuilder insert(Expression subQueryExpression) {
    this.insert = true;
    this.subQueryExpressionForInsertion = subQueryExpression;
    return this;
  }

  @Override
  public FilterAble where(Expression condition) {
    if (conditions == null) {
      conditions = new ArrayList<>();
    }
    if (condition != null && condition != Expression.EMPTY) {
      conditions.add(condition);
    }
    return this;
  }

  @Override
  public <T> FilterAble where(Iterable<T> values, Function<T, Expression> func) {
    if (conditions == null) {
      conditions = new ArrayList<>();
    }
    for (T value : values) {
      Expression condition = func.apply(value);
      if (condition != null && condition != Expression.EMPTY) {
        conditions.add(condition);
      }
    }
    return this;
  }

  @Override
  public GroupAble groupBy(List<Expression> expressions) {
    if (groups == null) {
      groups = new ArrayList<>();
    }
    groups.addAll(expressions);
    return this;
  }

  @Override
  public <T> GroupAble groupBy(Iterable<T> values, Function<T, Expression> func) {
    if (values == null) return this;
    if (groups == null) {
      groups = new ArrayList<>();
    }
    for (T value : values) {
      Expression e = func.apply(value);
      if (e != null) {
        groups.add(e);
      }
    }
    return this;
  }

  @Override
  public Group groupBy() {
    return groups == null ? null : new Group(groups, having);
  }

  @Override
  public GroupAble having(Expression booleanExpr) {
    this.having = booleanExpr;
    return this;
  }

  @Override
  public OrderAble orderBy(OrderMode model, Expression expr) {
    if (orders == null) {
      orders = new ArrayList<>();
    }
    orders.add(orderOf(model, expr));
    return this;
  }

  @Override
  public <T> OrderAble orderBy(Iterable<T> values, Function<T, Order> func) {
    if (values == null) return this;
    if (orders == null) {
      orders = new ArrayList<>();
    }
    for (T value : values) {
      Order order = func.apply(value);
      if (order != null) {
        orders.add(func.apply(value));
      }
    }
    return this;
  }

  @Override
  public List<Order> orders() {
    return orders == null ? Collections.emptyList() : Collections.unmodifiableList(orders);
  }

  @Override
  public SelectAble limit(Limit limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public SelectAble select(Expression expr, String alias) {
    if (selects == null) {
      selects = new ArrayList<>();
    }
    selects.add(new Select(expr, alias));
    return this;
  }

  @Override
  public <T> SelectAble select(Iterable<T> values, Function<T, Select> func) {
    if (values == null) return this;
    if (selects == null) {
      selects = new ArrayList<>();
    }
    for (T value : values) {
      Select select = func.apply(value);
      selects.add(select);
    }
    return this;
  }

  @Override
  public SelectAble select(List<Expression> expressions) {
    if (selects == null) {
      selects = new ArrayList<>();
    }
    selects.addAll(expressions.stream()
      .map(e -> new Select(e, null))
      .collect(Collectors.toList()));
    return this;
  }

  @Override
  public List<Select> selects() {
    return selects == null ? new ArrayList<>() : selects;
  }

  @Override
  public QueryAble distinct() {
    this.distinct = true;
    return this;
  }

  @Override
  public TemplateBuilder count() {
    this.count = true;
    return this;
  }

  @Override
  public TemplateBuilder forUpdate() {
    this.forUpdate = true;
    return this;
  }

  @Override
  public UpdateAble update() {
    if (updateSets == null) {
      updateSets = new ArrayList<>();
    }
    return this;
  }

  @Override
  public TemplateBuilder delete(Iterable<String> tables) {
    this.delete = true;
    if (deletes == null) {
      deletes = new ArrayList<>();
    }
    deletes.clear();
    tables.forEach(deletes::add);
    return this;
  }

  @Override
  public UpdateAble ending() {
    if (updateSets == null) {
      updateSets = new ArrayList<>();
    }
    updateSets.add(new LinkedHashMap<>());
    return this;
  }

  @Override
  public UpdateAble set(String field, Expression value) {
    if (updateSets == null) {
      updateSets = new ArrayList<>();
    }
    Map<String, Expression> sets;
    if (updateSets.isEmpty()) {
      sets = new LinkedHashMap<>();
      updateSets.add(sets);
    } else {
      sets = updateSets.get(updateSets.size() - 1);
    }
    sets.put(field, value);
    return this;
  }

  @Override
  public <T> UpdateAble set(Iterable<T> values, Function<T, Tuple.Tuple2<String, Object>> func) {
    if (updateSets == null) {
      updateSets = new ArrayList<>();
    }
    Map<String, Expression> sets;
    if (updateSets.isEmpty()) {
      sets = new LinkedHashMap<>();
      updateSets.add(sets);
    } else {
      sets = updateSets.get(updateSets.size() - 1);
    }
    for (T value : values) {
      Tuple.Tuple2<String, Object> tp = func.apply(value);
      Expression expr;
      if (tp._2 instanceof Expression) {
        expr = (Expression) tp._2;
      } else {
        expr = Expression.literal(tp._2);
      }
      sets.put(tp._1, expr);
    }
    return this;
  }

  @Override
  public Template template() {
    if (template == null) {
      Expression condition = null;
      if (conditions != null && !conditions.isEmpty()) {
        condition = conditions.get(0);
        for (int i = 1; i < conditions.size(); i++) {
          condition = condition.and(conditions.get(i));
        }
      }
      if (delete) {
        template = buildDeleteTemplate(from, deletes, joins, condition);
      } else if (insert && (subQueryExpressionForInsertion != null || (updateSets != null && !updateSets.isEmpty()))) {
        if (updateSets != null) {
          updateSets.removeIf(Map::isEmpty);
        }
        template = buildInsertTemplate(from, subQueryExpressionForInsertion, updateSets);
      } else if (updateSets != null && !updateSets.isEmpty()) {
        updateSets.removeIf(Map::isEmpty);
        template = buildUpdateTemplate(from, joins, condition, updateSets);
      } else {
        template = buildQueryTemplate(from, joins, condition, selects, groupBy(), orders, limit, distinct, count, forUpdate);
      }
    }
    return template;
  }

  @Override
  public Expression subQuery() {
    TemplateExpression expression = TemplateExpression.of(template());
    if (subSelects == null) {
      subSelects = new HashMap<>();
    }
    if (this.selects != null) {
      ArrayList<Select> arr = new ArrayList<>(this.selects);
      subSelects.put(expression, arr);
    }
    pop();
    return expression;
  }

  @Override
  protected AbstractSqlBuilder clone() {
    try {
      return (AbstractSqlBuilder) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new RuntimeException(ex);
    }
  }

  protected abstract Template buildInsertTemplate(From from, Expression subQueryExpressionForInsertion, List<Map<String, Expression>> updateSets);

  protected abstract Template buildDeleteTemplate(From from, List<String> deletes, List<Join> joins, Expression condition);

  protected abstract Template buildUpdateTemplate(From from, List<Join> joins, Expression condition, List<Map<String, Expression>> updateSets);

  protected abstract Template buildQueryTemplate(From from, List<Join> joins, Expression condition, List<Select> selects, Group group, List<Order> orders, Limit limit, boolean distinct, boolean count, boolean forUpdate);
}
