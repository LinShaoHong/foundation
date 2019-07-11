package com.github.sun.foundation.sql;

import com.github.sun.foundation.boot.utility.SqlFormatter;
import com.github.sun.foundation.boot.utility.Tuple;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.expression.ExpressionParser;
import org.immutables.value.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author LinSH
 * @Date: 7:24 PM 2019-02-28
 */
public interface SqlBuilder {
  void clear();

  JoinAble from(Class<?> entityClass, String tableAlias);

  default JoinAble from(Class<?> entityClass) {
    return from(entityClass, null);
  }

  JoinAble from(String table, String tableAlias);

  default JoinAble from(String table) {
    return from(table, null);
  }

  JoinAble from(Expression subQueryExpression, String subAlias);

  JoinAble from(From from);

  void putFroms(Map<String, From> froms);

  default void putFroms(List<From> froms) {
    Map<String, From> map = new HashMap<>();
    froms.stream().filter(f -> f.alias != null).forEach(f -> map.put(f.alias, f));
    putFroms(map);
  }

  default void putFroms(From... froms) {
    putFroms(Arrays.asList(froms));
  }

  abstract class From {
    public final String alias;

    public From(String alias) {
      this.alias = alias;
    }

    public abstract Class<?> entityClass();

    public abstract String tableName();

    public abstract Expression subQueryExpression();
  }

  class TypedFrom extends From {
    public TypedFrom(Class<?> entityClass, String alias) {
      super(alias);
      this.entityClass = entityClass;
    }

    public final Class<?> entityClass;

    @Override
    public Class<?> entityClass() {
      return entityClass;
    }

    @Override
    public String tableName() {
      return null;
    }

    @Override
    public Expression subQueryExpression() {
      return null;
    }
  }

  class DimsFrom extends From {
    public DimsFrom(String tableName, String alias) {
      super(alias);
      this.tableName = tableName;
    }

    public final String tableName;

    @Override
    public Class<?> entityClass() {
      return null;
    }

    @Override
    public String tableName() {
      return tableName;
    }

    @Override
    public Expression subQueryExpression() {
      return null;
    }
  }

  class SubFrom extends From {
    public SubFrom(Expression subQueryExpression, String alias) {
      super(alias);
      this.subQueryExpression = subQueryExpression;
    }

    public final Expression subQueryExpression;

    @Override
    public Class<?> entityClass() {
      return null;
    }

    @Override
    public String tableName() {
      return null;
    }

    @Override
    public Expression subQueryExpression() {
      return subQueryExpression;
    }
  }

  default From fromOf(Class<?> entityClass, String alias) {
    TypedFrom from = new TypedFrom(entityClass, alias);
    putFroms(from);
    return from;
  }

  default From fromOf(Class<?> entityClass) {
    return fromOf(entityClass, null);
  }

  default From fromOf(String tableName, String alias) {
    DimsFrom from = new DimsFrom(tableName, alias);
    putFroms(from);
    return from;
  }

  default From fromOf(String tableName) {
    return fromOf(tableName, null);
  }

  default From fromOf(Expression subQueryExpression, String alias) {
    SubFrom from = new SubFrom(subQueryExpression, alias);
    putFroms(from);
    return from;
  }

  default From fromOf(Expression subQueryExpression) {
    return fromOf(subQueryExpression, null);
  }

  interface JoinAble extends FilterAble {
    List<Join> joins();

    JoinAble join(Class<?> joinedClass, String alias, JoinMode mode, Expression on);

    <T> JoinAble join(Iterable<T> values, Function<T, Join> func);

    default <T> JoinAble join(Iterable<Join> joins) {
      return join(joins, j -> j);
    }

    default JoinAble leftJoin(Class<?> joinedClass, String alias, Expression on) {
      return join(joinedClass, alias, JoinMode.LEFT, on);
    }

    default JoinAble leftJoin(Class<?> joinedClass, String alias) {
      return leftJoin(joinedClass, alias, null);
    }

    default JoinAble innerJoin(Class<?> joinedClass, String alias, Expression on) {
      return join(joinedClass, alias, JoinMode.INNER, on);
    }

    default JoinAble innerJoin(Class<?> joinedClass, String alias) {
      return innerJoin(joinedClass, alias, null);
    }

    default JoinAble rightJoin(Class<?> joinedClass, String alias, Expression on) {
      return join(joinedClass, alias, JoinMode.RIGHT, on);
    }

    default JoinAble rightJoin(Class<?> joinedClass, String alias) {
      return rightJoin(joinedClass, alias, null);
    }

    JoinAble join(String joinedTable, String alias, JoinMode mode, Expression on);

    default JoinAble leftJoin(String joinedTable, String alias, Expression on) {
      return join(joinedTable, alias, JoinMode.LEFT, on);
    }

    default JoinAble leftJoin(String joinedTable, String alias) {
      return leftJoin(joinedTable, alias, null);
    }

    default JoinAble innerJoin(String joinedTable, String alias, Expression on) {
      return join(joinedTable, alias, JoinMode.INNER, on);
    }

    default JoinAble innerJoin(String joinedTable, String alias) {
      return innerJoin(joinedTable, alias, null);
    }

    default JoinAble rightJoin(String joinedTable, String alias, Expression on) {
      return join(joinedTable, alias, JoinMode.RIGHT, on);
    }

    default JoinAble rightJoin(String joinedTable, String alias) {
      return rightJoin(joinedTable, alias, null);
    }

    JoinAble join(Expression subQueryExpression, String subAlias, JoinMode mode, Expression on);

    default JoinAble leftJoin(Expression subQueryExpression, String subAlias, Expression on) {
      return join(subQueryExpression, subAlias, JoinMode.LEFT, on);
    }

    default JoinAble leftJoin(Expression subQueryExpression, String subAlias) {
      return leftJoin(subQueryExpression, subAlias, null);
    }

    default JoinAble innerJoin(Expression subQueryExpression, String subAlias, Expression on) {
      return join(subQueryExpression, subAlias, JoinMode.INNER, on);
    }

    default JoinAble innerJoin(Expression subQueryExpression, String subAlias) {
      return innerJoin(subQueryExpression, subAlias, null);
    }

    default JoinAble rightJoin(Expression subQueryExpression, String subAlias, Expression on) {
      return join(subQueryExpression, subAlias, JoinMode.RIGHT, on);
    }

    default JoinAble rightJoin(Expression sub, String subAlias) {
      return rightJoin(sub, subAlias, null);
    }
  }

  void putJoins(Map<String, Join> joins);

  default void putJoins(List<Join> joins) {
    Map<String, Join> map = new HashMap<>();
    joins.forEach(j -> map.put(j.alias, j));
    putJoins(map);
  }

  default void putJoins(Join... joins) {
    putJoins(Arrays.asList(joins));
  }

  abstract class Join {
    public final String alias;
    public final JoinMode mode;
    public final Expression on;

    public Join(String alias, JoinMode mode, Expression on) {
      this.alias = alias;
      this.mode = mode;
      this.on = on;
    }

    public abstract Class<?> joinedClass();

    public abstract String joinedTable();

    public abstract Expression subQueryExpression();
  }

  class TypedJoin extends Join {
    public TypedJoin(Class<?> joinedClass, String alias, JoinMode mode, Expression on) {
      super(alias, mode, on);
      this.joinedClass = joinedClass;
    }

    public final Class<?> joinedClass;

    @Override
    public Class<?> joinedClass() {
      return joinedClass;
    }

    @Override
    public String joinedTable() {
      return null;
    }

    @Override
    public Expression subQueryExpression() {
      return null;
    }
  }

  default Join joinOf(Class<?> joinedClass, String alias, JoinMode mode, Expression on) {
    TypedJoin join = new TypedJoin(joinedClass, alias, mode, on);
    putJoins(join);
    return join;
  }

  default Join leftJoinOf(Class<?> joinedClass, String alias, Expression on) {
    return joinOf(joinedClass, alias, JoinMode.LEFT, on);
  }

  default Join leftJoinOf(Class<?> joinedClass, String alias) {
    return leftJoinOf(joinedClass, alias, null);
  }

  default Join innerJoinOf(Class<?> joinedClass, String alias, Expression on) {
    return joinOf(joinedClass, alias, JoinMode.INNER, on);
  }

  default Join innerJoinOf(Class<?> joinedClass, String alias) {
    return innerJoinOf(joinedClass, alias, null);
  }

  default Join rightJoinOf(Class<?> joinedClass, String alias, Expression on) {
    return joinOf(joinedClass, alias, JoinMode.RIGHT, on);
  }

  default Join rightJoinOf(Class<?> joinedClass, String alias) {
    return rightJoinOf(joinedClass, alias, null);
  }

  default Join joinOf(String joinedTable, String alias, JoinMode mode, Expression on) {
    DimsJoin join = new DimsJoin(joinedTable, alias, mode, on);
    putJoins(join);
    return join;
  }

  default Join leftJoinOf(String joinedTable, String alias, Expression on) {
    return joinOf(joinedTable, alias, JoinMode.LEFT, on);
  }

  default Join leftJoinOf(String joinedTable, String alias) {
    return leftJoinOf(joinedTable, alias, null);
  }

  default Join innerJoinOf(String joinedTable, String alias, Expression on) {
    return joinOf(joinedTable, alias, JoinMode.INNER, on);
  }

  default Join innerJoinOf(String joinedTable, String alias) {
    return innerJoinOf(joinedTable, alias, null);
  }

  default Join rightJoinOf(String joinedTable, String alias, Expression on) {
    return joinOf(joinedTable, alias, JoinMode.RIGHT, on);
  }

  default Join rightJoinOf(String joinedTable, String alias) {
    return rightJoinOf(joinedTable, alias, null);
  }

  default Join joinOf(Expression subQueryExpression, String subAlias, JoinMode mode, Expression on) {
    SubJoin join = new SubJoin(subQueryExpression, subAlias, mode, on);
    putJoins(join);
    return join;
  }

  default Join leftJoinOf(Expression subQueryExpression, String subAlias, Expression on) {
    return joinOf(subQueryExpression, subAlias, JoinMode.LEFT, on);
  }

  default Join leftJoinOf(Expression subQueryExpression, String subAlias) {
    return leftJoinOf(subQueryExpression, subAlias, null);
  }

  default Join innerJoinOf(Expression subQueryExpression, String subAlias, Expression on) {
    return joinOf(subQueryExpression, subAlias, JoinMode.INNER, on);
  }

  default Join innerJoinOf(Expression subQueryExpression, String subAlias) {
    return innerJoinOf(subQueryExpression, subAlias, null);
  }

  default Join rightJoinOf(Expression subQueryExpression, String subAlias, Expression on) {
    return joinOf(subQueryExpression, subAlias, JoinMode.RIGHT, on);
  }

  default Join rightJoinOf(Expression subQueryExpression, String subAlias) {
    return rightJoinOf(subQueryExpression, subAlias, null);
  }

  class DimsJoin extends Join {
    public DimsJoin(String joinedTable, String alias, JoinMode mode, Expression on) {
      super(alias, mode, on);
      this.joinedTable = joinedTable;
    }

    public final String joinedTable;

    @Override
    public Class<?> joinedClass() {
      return null;
    }

    @Override
    public String joinedTable() {
      return joinedTable;
    }

    @Override
    public Expression subQueryExpression() {
      return null;
    }
  }

  class SubJoin extends Join {
    public SubJoin(Expression subQueryExpression, String alias, JoinMode mode, Expression on) {
      super(alias, mode, on);
      this.subQueryExpression = subQueryExpression;
    }

    public final Expression subQueryExpression;

    @Override
    public Class<?> joinedClass() {
      return null;
    }

    @Override
    public String joinedTable() {
      return null;
    }

    @Override
    public Expression subQueryExpression() {
      return subQueryExpression;
    }
  }

  interface FilterAble extends ModifyAble {
    UpdateAble insert();

    TemplateBuilder insert(Expression subQueryExpression);

    FilterAble where(Expression condition);

    <T> FilterAble where(Iterable<T> values, Function<T, Expression> func);
  }

  interface GroupAble extends OrderAble {
    Group groupBy();

    GroupAble groupBy(List<Expression> expressions);

    <T> GroupAble groupBy(Iterable<T> values, Function<T, Expression> func);

    default GroupAble groupBy(Expression... expressions) {
      return groupBy(Arrays.asList(expressions));
    }

    default GroupAble groupBy(String... fields) {
      return groupBy(Arrays.stream(fields)
        .map(Expression::path)
        .collect(Collectors.toList()));
    }

    GroupAble having(Expression booleanExpr);
  }

  class Group {
    public final List<Expression> groups;
    public final Expression having;

    public Group(List<Expression> groups, Expression having) {
      this.groups = groups;
      this.having = having;
    }
  }

  interface OrderAble extends LimitAble {
    List<Order> orders();

    OrderAble orderBy(OrderMode mode, Expression expr);

    <T> OrderAble orderBy(Iterable<T> values, Function<T, Order> func);

    default OrderAble asc(Expression expr) {
      return orderBy(OrderMode.ASC, expr);
    }

    default OrderAble desc(Expression expr) {
      return orderBy(OrderMode.DESC, expr);
    }

    default OrderAble asc(String field) {
      return asc(Expression.path(field));
    }

    default OrderAble desc(String field) {
      return desc(Expression.path(field));
    }
  }

  class Order {
    public final OrderMode mode;
    public final Expression expression;

    public Order(OrderMode mode, Expression expression) {
      this.mode = mode;
      this.expression = expression;
    }
  }

  default Order orderOf(OrderMode mode, Expression expression) {
    return new Order(mode, expression);
  }

  default Order ascOf(Expression expression) {
    return orderOf(OrderMode.ASC, expression);
  }

  default Order descOf(Expression expression) {
    return orderOf(OrderMode.DESC, expression);
  }

  interface LimitAble extends SelectAble {
    SelectAble limit(Limit limit);

    default SelectAble limit(int start, int count) {
      return limit(new Limit(Expression.literal(start), Expression.literal(count)));
    }

    default SelectAble limit(int count) {
      return limit(0, count);
    }
  }

  class Limit {
    public final Expression start;
    public final Expression count;

    public Limit(Expression start, Expression count) {
      this.start = start;
      this.count = count;
    }
  }

  interface SelectAble extends DistinctAble {
    List<Select> selects();

    SelectAble select(Expression expr, String alias);

    <T> SelectAble select(Iterable<T> values, Function<T, Select> func);

    SelectAble select(List<Expression> expressions);

    default SelectAble select(Expression... expressions) {
      return select(Arrays.asList(expressions));
    }

    default SelectAble select(String... fields) {
      List<Expression> expressions = Stream.of(fields)
        .map(Expression::path)
        .collect(Collectors.toList());
      return select(expressions);
    }
  }

  class Select {
    public final Expression expr;
    public final String alias;

    public Select(Expression expr, String alias) {
      this.expr = expr;
      this.alias = alias;
    }
  }

  default Select selectOf(Expression expr, String alias) {
    return new Select(expr, alias);
  }

  default Select selectOf(Expression expr) {
    return selectOf(expr, null);
  }

  interface DistinctAble extends QueryAble {
    QueryAble distinct();
  }

  interface QueryAble extends TemplateBuilder {
    TemplateBuilder count();

    TemplateBuilder forUpdate();
  }

  interface ModifyAble extends GroupAble {
    UpdateAble update();

    TemplateBuilder delete(Iterable<String> tables);

    default TemplateBuilder delete(String... tables) {
      return delete(Arrays.asList(tables));
    }

    default TemplateBuilder delete() {
      return delete(Collections.emptyList());
    }
  }

  interface UpdateAble extends TemplateBuilder {
    UpdateAble ending();

    UpdateAble set(String field, Expression value);

    <T> UpdateAble set(Iterable<T> values, Function<T, Tuple.Tuple2<String, Object>> func);

    default UpdateAble set(String field, Object value) {
      return set(field, Expression.literal(value));
    }
  }

  interface TemplateBuilder {
    Template template();

    Expression subQuery();
  }

  interface Template {
    /**
     * 格式化的字面量SQL
     */
    default String pretty() {
      return SqlFormatter.format(literalSQL());
    }

    /**
     * 字面量SQL
     */
    String literalSQL();

    /**
     * 带有占位符"?"的SQL
     */
    String placeholderSQL();

    /**
     * 带有参数化的SQL
     */
    String parameterizedSQL();

    /**
     * 参数列表
     */
    List<Expression.Parameter> parameters();

    default Map<String, Object> parametersAsMap() {
      Map<String, Object> args = new LinkedHashMap<>();
      parameters().forEach(v -> args.put(v.name(), v.value()));
      return args;
    }
  }

  @Value.Immutable
  @Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
  interface TemplateExpression extends Expression {
    @Value.Parameter
    Template template();

    @Override
    default int priority() {
      return 20;
    }

    @Override
    default <T> T visit(Visitor<T> visitor) {
      if (visitor instanceof SqlBuilder.AbstractVisitor) {
        return ((SqlBuilder.AbstractVisitor<T>) visitor).onTemplateExpression(this);
      }
      return null;
    }

    static TemplateExpression of(SqlBuilder.Template template) {
      return ImmutableTemplateExpression.of(template);
    }
  }

  abstract class AbstractVisitor<T> extends Expression.AbstractVisitor<T> {
    public T onTemplateExpression(TemplateExpression expr) {
      return null;
    }
  }

  interface StatelessSqlBuilder extends SqlBuilder, JoinAble, UpdateAble {
  }

  interface Factory {
    SqlBuilder create();
  }

  default Expression parse(String expr) {
    ExpressionParser parser = ExpressionParser.newParser();
    return parser.parse(expr);
  }

  default Expression id(String name) {
    return Expression.id(name);
  }

  default Expression value(Object v) {
    return Expression.literal(v);
  }

  default Expression param() {
    return Expression.Parameter.of("?", null);
  }

  // sql function
  default Expression field(String name) {
    return Expression.path(name);
  }

  default Expression exists(Expression expr) {
    return id("EXISTS").call(expr);
  }

  default Expression notExists(Expression expr) {
    return id("NOT EXISTS").call(expr);
  }

  default Expression rowNumber() {
    return id("ROW_NUMBER").call();
  }

  // case被占用，首字母大写
  default Expression.CaseAble Case(Expression expression) {
    return new Expression.CaseAbleImpl(expression);
  }

  default Expression.CaseAble Case() {
    return Case(Expression.EMPTY);
  }

  default Expression coalesce(Expression... expressions) {
    return id("COALESCE").call(expressions);
  }

  default Expression coalesce(Object... values) {
    return id("COALESCE").call(values);
  }
}
