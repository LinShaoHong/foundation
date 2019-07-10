package com.github.sun.foundation.sql.spi;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.expression.Expression.*;
import com.github.sun.foundation.sql.Model;
import com.github.sun.foundation.boot.*;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * @Author LinSH
 * @Date: 10:35 AM 2019-03-01
 */
public abstract class BasicSqlBuilder extends AbstractSqlBuilder {
  private final static Pattern INVALID_CHARS = Pattern.compile("[\\t\\n\\r\\00\\x1a\\x08'\"`]");

  public BasicSqlBuilder() {
    clear();
  }

  @Override
  protected Template buildInsertTemplate(From from, Expression subQueryExpressionForInsertion, List<Map<String, Expression>> updateSets) {
    SqlTemplate.Builder sb = SqlTemplate.newBuilder(counter::next);
    sb.append("INSERT INTO ");
    buildTableClause(sb, from);
    Visitor visitor = newVisitor(sb);
    sb.append(" (");
    if (subQueryExpressionForInsertion != null) {
      sb.append(null, subQueryExpressionForInsertion);
      sb.append(")");
    } else {
      Map<String, Model.Property> props = new HashMap<>();
      buildWithSep(sb, ", ", updateSets.get(0).keySet(), (b, s) -> {
        field(s).visit(visitor);
        props.put(s, visitor.context.property);
      });
      sb.append(") VALUES ");
      buildWithSep(sb, ", ", updateSets, (builder, updateSet) -> {
        builder.append("(");
        buildWithSep(builder, ", ", updateSet.entrySet(), (b, e) -> {
          visitor.context.property = props.get(e.getKey());
          e.getValue().visit(visitor);
        });
        builder.append(")");
      });
    }
    return sb.build();
  }

  @Override
  protected Template buildDeleteTemplate(From from, List<String> deletes, List<Join> joins, Expression condition) {
    SqlTemplate.Builder sb = SqlTemplate.newBuilder(counter::next);
    buildDeleteClause(sb, from.alias, deletes, joins);
    buildFromClause(sb, from);
    buildJoinClause(sb, joins);
    buildWhereClause(sb, condition);
    return sb.build();
  }

  @Override
  protected Template buildUpdateTemplate(From from, List<Join> joins, Expression condition, List<Map<String, Expression>> updateSets) {
    SqlTemplate.Builder sb = SqlTemplate.newBuilder(counter::next);
    sb.append("UPDATE ");
    buildTableClause(sb, from);
    buildJoinClause(sb, joins);
    buildUpdateSetsClause(sb, updateSets.get(0));
    buildWhereClause(sb, condition);
    return sb.build();
  }

  @Override
  protected Template buildQueryTemplate(From from, List<Join> joins, Expression condition, List<Select> selects, Group group, List<Order> orders, Limit limit, boolean distinct, boolean count, boolean forUpdate) {
    SqlTemplate.Builder sb = SqlTemplate.newBuilder(counter::next);
    buildSelectClause(sb, selects, distinct, count);
    buildFromClause(sb, from);
    buildJoinClause(sb, joins);
    buildWhereClause(sb, condition);
    buildGroupByClause(sb, group);
    buildOrderByClause(sb, orders);
    buildLimitClause(sb, limit);
    buildForUpdateClause(sb, forUpdate);
    return sb.build();
  }

  protected void buildDeleteClause(SqlTemplate.Builder sb, String tableAlias, List<String> deletes, List<Join> joins) {
    sb.append("DELETE");
    if (deletes == null || deletes.isEmpty()) {
      if (tableAlias != null) {
        sb.append(" ").append(escapeName(tableAlias));
      }
      if (joins != null && !joins.isEmpty()) {
        sb.append(", ");
        buildWithSep(sb, ", ", joins, (s, j) -> s.append(escapeName(j.alias)));
      }
    } else {
      sb.append(" ");
      buildWithSep(sb, ", ", deletes, (s, d) -> s.append(escapeName(d)));
    }
  }

  protected void buildSelectClause(SqlTemplate.Builder sb, List<Select> selects, boolean distinct, boolean count) {
    sb.append("SELECT ");
    if (count) {
      sb.append("COUNT(");
    }
    if (distinct) {
      sb.append("DISTINCT ");
    }
    if (selects == null || selects.isEmpty()) {
      sb.append(count && !distinct ? "1" : "*");
    } else {
      Visitor visitor = newVisitor(sb);
      buildWithSep(sb, ", ", selects, (b, s) -> {
        String alias = s.alias;
        if (alias == null) {
          if (s.expr instanceof MemberAccessExpression) {
            List<String> paths = paths(s.expr);
            Model.Property aliasPrefixProp = getSelectAliasPrefixProp(paths.get(0));
            if (aliasPrefixProp != null) {
              String selectAliasPrefix = aliasPrefixProp.selectAliasPrefix();
              if ("*".equals(paths.get(1))) {
                String a = paths.get(0);
                Model model = model(resultType(aliasPrefixProp.field().getGenericType()));
                buildWithSep(sb, ", ", model.persistenceProperties(), (builder, property) -> {
                  field(a + "." + property.name()).visit(visitor);
                  builder.append(" AS ").append(column(visitor, Strings.joinCamelCase(selectAliasPrefix, property.name())));
                });
              } else {
                s.expr.visit(visitor);
                alias = Strings.joinCamelCase(selectAliasPrefix, paths.get(1));
              }
            } else {
              s.expr.visit(visitor);
              if (visitor.context.property.isJsonPath()) {
                paths = paths.size() == 3 ? paths.subList(1, 3) : paths;
                alias = paths.get(1);
                alias = Strings.joinCamelCase(paths.get(0), alias);
              } else {
                alias = paths.get(1);
              }
            }
          } else {
            s.expr.visit(visitor);
          }
        } else {
          s.expr.visit(visitor);
        }
        if (alias != null && !"*".equals(alias)) {
          // update alias
          updateAlias(s.expr, alias);
          b.append(" AS ").append(column(visitor, alias));
        }
      });
    }
    if (count) {
      sb.append(")");
    }
  }

  private Class<?> resultType(Type type) {
    if (type instanceof ParameterizedType) {
      for (Class<?> baseClass : Arrays.asList(List.class, Set.class)) {
        List<Type> types = TypeInfo.getTypeParameters(type, baseClass);
        if (types != null) {
          Type t = types.get(0);
          if (t instanceof ParameterizedType) {
            return resultType(((ParameterizedType) t).getRawType());
          }
          return resultType(t);
        }
      }
    } else if (type instanceof Class) {
      Class<?> c = (Class<?>) type;
      if (c.isArray()) { // array
        return c.getComponentType();
      }
      return c;
    }
    ResolvedType resolvedType = ResolvedType.resolve(type);
    throw new IllegalArgumentException("unknown type: " + resolvedType.simpleName());
  }

  private Model.Property getSelectAliasPrefixProp(String ref) {
    Join join = joins().stream()
      .filter(j -> j.alias.equals(ref))
      .findFirst()
      .orElse(null);
    return join == null ? null : mainModel.transientProperties()
      .stream()
      .filter(p -> resultType(p.field().getGenericType()) == join.joinedClass())
      .findFirst()
      .orElse(null);
  }

  private void updateAlias(Expression expr, String alias) {
    int i = -1;
    for (int j = 0; j < selects().size(); j++) {
      if (selects().get(j).expr == expr) {
        i = j;
        break;
      }
    }
    if (i >= 0 && i < selects().size()) {
      selects().set(i, new Select(expr, alias));
    }
  }

  private String column(Visitor visitor, String alias) {
    if (visitor.context != null && visitor.context.model != null) {
      return escapeName(visitor.context.model.column(alias));
    } else {
      return escapeName(mainModel.column(alias));
    }
  }

  protected void buildFromClause(SqlTemplate.Builder sb, From from) {
    sb.append(" FROM ");
    buildTableClause(sb, from);
  }

  protected void buildTableClause(SqlTemplate.Builder sb, From from) {
    if (from.subQueryExpression() != null) {
      sb.append("(");
      sb.append(null, from.subQueryExpression());
      sb.append(")");
    } else {
      Model model = model(from);
      sb.append(escapeName(model.tableName()));
    }
    if (from.alias != null) {
      sb.append(" AS ").append(escapeName(from.alias));
    }
  }

  protected void buildJoinClause(SqlTemplate.Builder sb, List<Join> joins) {
    if (joins != null && !joins.isEmpty()) {
      joins.forEach(join -> {
        switch (join.mode) {
          case LEFT:
            sb.append(" LEFT JOIN ");
            break;
          case INNER:
            sb.append(" INNER JOIN ");
            break;
          case RIGHT:
            sb.append(" RIGHT JOIN ");
            break;
        }
        if (join.subQueryExpression() != null) {
          buildSubJoinOnClause(sb, join.subQueryExpression());
        } else {
          sb.append(escapeName(model(join).tableName()));
        }
        sb.append(" AS ").append(escapeName(join.alias));
        buildJoinOnClause(sb, join.on);
      });
    }
  }

  protected void buildSubJoinOnClause(SqlTemplate.Builder sb, Expression subQueryExpression) {
    sb.append(" LATERAL(");
    sb.append(null, subQueryExpression);
    sb.append(")");
  }

  protected void buildJoinOnClause(SqlTemplate.Builder sb, Expression on) {
    if (on != null) {
      sb.append(" ON ");
      on.visit(newVisitor(sb));
    } else {
      sb.append(" ON TRUE");
    }
  }

  protected void buildWhereClause(SqlTemplate.Builder sb, Expression condition) {
    if (condition != null && condition != Expression.EMPTY) {
      sb.append(" WHERE ");
      condition.visit(newVisitor(sb));
    }
  }

  protected void buildUpdateSetsClause(SqlTemplate.Builder sb, Map<String, Expression> updateSets) {
    sb.append(" SET ");
    Visitor visitor = newVisitor(sb);
    buildWithSep(sb, ", ", updateSets.entrySet(), (b, e) -> {
      field(e.getKey()).visit(visitor);
      sb.append(" = ");
      e.getValue().visit(visitor);
    });
  }

  protected void buildGroupByClause(SqlTemplate.Builder sb, Group group) {
    if (group != null && !group.groups.isEmpty()) {
      List<Expression> groups = group.groups;
      Expression having = group.having;
      sb.append(" GROUP BY ");
      Visitor visitor = newVisitor(sb);
      buildWithSep(sb, ", ", groups, (b, e) -> e.visit(visitor));
      if (having != null) {
        sb.append(" HAVING ");
        having.visit(visitor);
      }
    }
  }

  protected void buildOrderByClause(SqlTemplate.Builder sb, List<Order> orders) {
    if (orders != null && !orders.isEmpty()) {
      sb.append(" ORDER BY ");
      Visitor visitor = newVisitor(sb);
      buildWithSep(sb, ", ", orders, (b, o) -> {
        o.expression.visit(visitor);
        switch (o.mode) {
          case ASC:
            b.append(" ASC");
            break;
          case DESC:
            b.append(" DESC");
            break;
        }
      });
    }
  }

  protected void buildLimitClause(SqlTemplate.Builder sb, Limit limit) {
    if (limit != null) {
      Visitor visitor = newVisitor(sb);
      sb.append(" LIMIT ");
      limit.count.visit(visitor);
      sb.append(" OFFSET ");
      limit.start.visit(visitor);
    }
  }

  protected void buildForUpdateClause(SqlTemplate.Builder sb, boolean forUpdate) {
    if (forUpdate) {
      sb.append(" FOR UPDATE");
    }
  }

  protected <T> void buildWithSep(SqlTemplate.Builder sb, String sep, Iterable<T> values, BiConsumer<SqlTemplate.Builder, T> func) {
    Iterator<T> it = values.iterator();
    if (it.hasNext()) {
      func.accept(sb, it.next());
    }
    while (it.hasNext()) {
      sb.append(sep);
      func.accept(sb, it.next());
    }
  }

  protected void checkName(String name) {
    if (INVALID_CHARS.matcher(name).matches()) {
      throw new IllegalArgumentException("参数中包含特殊字符");
    }
  }

  protected String escapeName(String name) {
    checkName(name);
    return name;
  }

  private List<String> paths(Expression expr) {
    List<String> members = new ArrayList<>();
    Expression e = expr;
    while (true) {
      if (e instanceof Identifier) {
        members.add(((Identifier) e).name());
        break;
      }
      if (e instanceof MemberAccessExpression) {
        MemberAccessExpression mae = (MemberAccessExpression) e;
        e = mae.object();
        members.add(mae.member());
      } else {
        throw new IllegalArgumentException();
      }
    }
    Collections.reverse(members);
    return members;
  }

  private Model findModelByAlias(String alias) {
    if (alias.equals(from.alias)) {
      return mainModel;
    }
    return byAliasFromContext(alias);
  }

  protected Visitor newVisitor(SqlTemplate.Builder sb) {
    return new Visitor(sb);
  }

  protected class Visitor extends AbstractSqlVisitor<Void> {
    public Context context = new Context();
    protected final SqlTemplate.Builder sb;

    public Visitor(SqlTemplate.Builder sb) {
      this.sb = sb;
    }

    private void buildWithParenthesis(Expression expr, boolean hasParenthesis) {
      if (hasParenthesis) {
        sb.append("(");
        expr.visit(this);
        sb.append(")");
      } else {
        expr.visit(this);
      }
    }

    @Override
    public Void onUnaryExpression(UnaryExpression expr) {
      Operator op = expr.operator();
      switch (op) {
        case NOT:
          sb.append(" NOT ");
          break;
        default:
          throw new IllegalArgumentException();
      }
      Expression operand = expr.operand();
      buildWithParenthesis(operand, op.priority > operand.priority());
      return null;
    }

    @Override
    public Void onBinaryExpression(BinaryExpression expr) {
      Operator op = expr.operator();
      Expression leftOperand = expr.leftOperand();
      Expression rightOperand = expr.rightOperand();
      buildWithParenthesis(leftOperand, op.priority > leftOperand.priority());
      switch (op) {
        case PLUS:
          sb.append(" + ");
          break;
        case SUB:
          sb.append(" - ");
          break;
        case MUL:
          sb.append(" * ");
          break;
        case DIV:
          sb.append(" / ");
          break;
        case MOD:
          sb.append(" % ");
          break;
        case EQ:
          sb.append(" = ");
          break;
        case GE:
          sb.append(" >= ");
          break;
        case GT:
          sb.append(" > ");
          break;
        case LE:
          sb.append(" <= ");
          break;
        case LT:
          sb.append(" < ");
          break;
        case NE:
          sb.append(" != ");
          break;
        case AND:
          sb.append(" AND ");
          break;
        case OR:
          sb.append(" OR ");
          break;
        default:
          throw new IllegalArgumentException();
      }
      buildWithParenthesis(rightOperand, op.priority > rightOperand.priority());
      return null;
    }

    @Override
    public Void onMemberAccessExpression(MemberAccessExpression expr) {
      List<String> paths = paths(expr);
      if (paths.size() > 3) {
        throw new IllegalArgumentException("The number of members can not more than three");
      }
      if (paths.size() > 1) {
        String head = paths.get(0);
        Model model = findModelByAlias(head);
        if (model == null) {
          context.property = mainModel.findProperty(head);
          // json path
          context.model = mainModel;
          if (context.property.hasJsonPath()) {
            context.property.isJsonPath(true);
            this.onJsonPathExpression(JsonPathExpression.of(Expression.id(head), paths.get(1), true));
          } else {
            onIdentifier(Identifier.of(Iterators.mkString(paths, ".")));
          }
        } else {
          context.model = model;
          context.property = model.findProperty(paths.get(1));
          head = paths.get(0) + "." + paths.get(1);
          if (paths.size() == 3) { // json path
            if (context.property.hasJsonPath()) {
              context.property.isJsonPath(true);
              this.onJsonPathExpression(JsonPathExpression.of(Expression.id(head), paths.get(2), true));
            } else {
              onIdentifier(Identifier.of(Iterators.mkString(paths, ".")));
            }
          } else {
            context.property.isJsonPath(false);
            onIdentifier(Identifier.of(head));
          }
        }
      }
      return null;
    }

    @Override
    protected Void onCastExpression(Expression value, String dataType) {
      sb.append("CAST(");
      value.visit(this);
      sb.append(" AS ").append(dataType);
      sb.append(")");
      return null;
    }

    @Override
    protected Void onBetweenExpression(Expression value, Expression start, Expression end) {
      sb.append("(");
      value.visit(this);
      sb.append(" BETWEEN ");
      buildWithSep(sb, " AND ", Arrays.asList(start, end), (b, e) -> e.visit(this));
      sb.append(")");
      return null;
    }

    @Override
    protected Void onInExpression(Expression value, List<Expression> parameters, boolean isIn) {
      sb.append("(");
      value.visit(this);
      sb.append(isIn ? " IN(" : " NOT IN(");
      buildWithSep(sb, ", ", parameters, (builder, param) -> param.visit(this));
      sb.append("))");
      return null;
    }

    @Override
    protected Void onFuncExpression(Expression func, List<Expression> parameters) {
      if (func instanceof Identifier) {
        String value = ((Identifier) func).name();
        checkName(value);
        sb.append(value);
      } else if (func instanceof MemberAccessExpression) {
        MemberAccessExpression mae = (MemberAccessExpression) func;
        Expression object = mae.object();
        String member = mae.member();
        checkName(member);
        object.visit(this);
        sb.append(".").append(member);
      } else {
        throw new IllegalArgumentException("syntax error of function: " + func.toString());
      }
      sb.append("(");
      buildWithSep(sb, ", ", parameters, (b, e) -> e.visit(this));
      sb.append(")");
      return null;
    }

    @Override
    public Void onJsonPathExpression(JsonPathExpression expr) {
      expr.object().visit(this);
      sb.append(expr.unquoted() ? "->>'" : "->'");
      escape(sb, context.model.column(expr.path()));
      sb.append("'");
      return null;
    }

    @Override
    public Void onCaseExpression(CaseExpression expr) {
      List<CaseBranch> branches = expr.branches();
      Expression expression = branches.get(0).condition();
      if (expression != Expression.EMPTY) {
        sb.append("(CASE ");
        expression.visit(this);
      } else {
        sb.append("(CASE");
      }
      branches.subList(1, branches.size())
        .forEach(branch -> {
          Expression condition = branch.condition();
          Tuple.Tuple2<Expression, Expression> value = branch.value();
          if (condition == Expression.EMPTY) {
            if (value._2 != Expression.EMPTY) {
              sb.append(" ELSE ");
              if (value._1 != Expression.EMPTY) {
                sb.setAppendable(false);
                value._1.visit(this);
                sb.setAppendable(true);
              }
              value._2.visit(this);
            }
          } else {
            sb.append(" WHEN ");
            condition.visit(this);
            sb.append(" THEN ");
            if (value._1 != Expression.EMPTY) {
              sb.setAppendable(false);
              value._1.visit(this);
              sb.setAppendable(true);
            }
            value._2.visit(this);
          }
        });
      sb.append(" END)");
      return null;
    }

    @Override
    protected Void onLikeExpression(Expression value, Expression parameter, LikeModel model) {
      sb.append("(");
      value.visit(this);
      sb.append(" LIKE CONCAT(");
      if (model == LikeModel.ENDS_WITH || model == LikeModel.CONTAINS) {
        sb.append("'%', ");
      }
      parameter.visit(this);
      if (model == LikeModel.STARTS_WITH || model == LikeModel.CONTAINS) {
        sb.append(", '%'");
      }
      sb.append("))");
      return null;
    }

    @Override
    protected Void onIsNotNullExpression(Expression value) {
      value.visit(this);
      sb.append(" IS NOT NULL");
      return null;
    }

    @Override
    protected Void onOverExpression(Expression value, String sql) {
      value.visit(this);
      sb.append(" OVER(");
      sb.append(sql);
      sb.append(")");
      return null;
    }

    @Override
    protected Void onDistinctExpression(Expression value) {
      sb.append("DISTINCT ");
      value.visit(this);
      return null;
    }

    @Override
    protected Void onIsNullExpression(Expression value) {
      value.visit(this);
      sb.append(" IS NULL");
      return null;
    }

    @Override
    public Void onIdentifier(Identifier expr) {
      String name = expr.name();
      if (context.property == null) {
        context.model = mainModel;
        context.property = mainModel.findProperty(name);
      } else if (!name.contains(".")) {
        context.model = mainModel;
        context.property = context.model.findProperty(name);
      }
      buildWithSep(sb, ".", Arrays.asList(name.split("\\.")), (builder, s) -> {
        String ref = context.model.column(s);
        if ("*".equals(ref) || Strings.isInt(ref)) {
          builder.append(ref);
        } else {
          builder.append(escapeName(ref));
        }
      });
      return null;
    }

    @Override
    public Void onParameter(Parameter expr) {
      sb.append(context.property, expr);
      return null;
    }

    @Override
    public Void onLiteral(Literal expr) {
      sb.append(context.property, expr.value());
      return null;
    }

    @Override
    public Void onEmpty(Expression expr) {
      sb.append(context.property, null);
      return null;
    }

    @Override
    public Void onTemplateExpression(TemplateExpression expr) {
      sb.append(context.property, expr);
      return null;
    }
  }

  private static void escape(Appendable sb, String s) {
    try {
      sb.append(Strings.escape(s));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static class Context {
    private Model model;
    private Model.Property property;
  }
}