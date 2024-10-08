package com.github.sun.foundation.expression;

import com.github.sun.foundation.boot.utility.Tuple;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface Expression {
    static Expression id(String name) {
        return Identifier.of(name);
    }

    static Expression literal(Object value) {
        return Literal.of(value);
    }

    static Expression path(String path) {
        if (path == null) {
            throw new IllegalArgumentException();
        }
        List<String> paths = Arrays.asList(path.split("\\."));
        Expression e = id(paths.get(0));
        return e.member(paths.subList(1, paths.size()));
    }

    static List<Expression> params(Collection<?> values) {
        return values.stream().map(v -> {
            if (v instanceof Expression) {
                return (Expression) v;
            }
            return literal(v);
        }).collect(Collectors.toList());
    }

    int priority();

    <T> T visit(Visitor<T> visitor);

    interface Visitor<T> {
        T onUnaryExpression(UnaryExpression expr);

        T onBinaryExpression(BinaryExpression expr);

        T onMemberAccessExpression(MemberAccessExpression expr);

        T onCallExpression(CallExpression expr);

        T onCaseExpression(CaseExpression expr);

        T onJsonPathExpression(JsonPathExpression expr);

        T onIdentifier(Identifier expr);

        T onLiteral(Literal expr);

        T onParameter(Parameter expr);

        T onEmpty(Expression expr);
    }

    default Expression eq(Expression right) {
        return BinaryExpression.of(this, Operator.EQ, right);
    }

    default Expression eq(Object right) {
        return eq(literal(right));
    }

    default Expression ge(Expression right) {
        return BinaryExpression.of(this, Operator.GE, right);
    }

    default Expression ge(Object right) {
        return ge(literal(right));
    }

    default Expression gt(Expression right) {
        return BinaryExpression.of(this, Operator.GT, right);
    }

    default Expression gt(Object right) {
        return gt(literal(right));
    }

    default Expression le(Expression right) {
        return BinaryExpression.of(this, Operator.LE, right);
    }

    default Expression le(Object right) {
        return le(literal(right));
    }

    default Expression lt(Expression right) {
        return BinaryExpression.of(this, Operator.LT, right);
    }

    default Expression lt(Object right) {
        return lt(literal(right));
    }

    default Expression ne(Expression right) {
        return BinaryExpression.of(this, Operator.NE, right);
    }

    default Expression ne(Object right) {
        return ne(literal(right));
    }

    default Expression plus(Expression right) {
        return BinaryExpression.of(this, Operator.PLUS, right);
    }

    default Expression plus(Object right) {
        return plus(literal(right));
    }

    default Expression sub(Expression right) {
        return BinaryExpression.of(this, Operator.SUB, right);
    }

    default Expression sub(Object right) {
        return sub(literal(right));
    }

    default Expression mul(Expression right) {
        return BinaryExpression.of(this, Operator.MUL, right);
    }

    default Expression mul(Object right) {
        return mul(literal(right));
    }

    default Expression div(Expression right) {
        return BinaryExpression.of(this, Operator.DIV, right);
    }

    default Expression div(Object right) {
        return div(literal(right));
    }

    default Expression mod(Expression right) {
        return BinaryExpression.of(this, Operator.MOD, right);
    }

    default Expression mod(Object right) {
        return mod(literal(right));
    }

    default Expression and(Expression right) {
        return isEmpty(this) ?
                (isEmpty(right) ? EMPTY : right) :
                (isEmpty(right) ? this : BinaryExpression.of(this, Operator.AND, right));
    }

    default Expression or(Expression right) {
        return isEmpty(this) ?
                (isEmpty(right) ? EMPTY : right) :
                (isEmpty(right) ? this : BinaryExpression.of(this, Operator.OR, right));
    }

    default Expression not() {
        return UnaryExpression.of(this, Operator.NOT);
    }

    default Expression call(List<Expression> args) {
        if (args == null || (args.size() == 1 && (args.get(0) == null || args.get(0) == EMPTY))) {
            return EMPTY;
        }
        return CallExpression.of(this, args);
    }

    default Expression call(Expression... args) {
        return call(Arrays.asList(args));
    }

    default Expression call(Collection<?> args) {
        return call(params(args));
    }

    default Expression call(Object... args) {
        return call(Arrays.asList(args));
    }

    default Expression member(List<String> path) {
        Expression e = this;
        for (String m : path) {
            e = MemberAccessExpression.of(e, m);
        }
        return e;
    }

    default Expression member(String... path) {
        return member(Arrays.asList(path));
    }

    // normal function
    default Expression in(List<Expression> args) {
        return member("in").call(args);
    }

    default Expression in(Expression... args) {
        return member("in").call(args);
    }

    default Expression in(Collection<?> args) {
        return member("in").call(args);
    }

    default Expression in(Object... args) {
        return member("in").call(args);
    }

    default Expression notIn(List<Expression> args) {
        return member("notIn").call(args);
    }

    default Expression notIn(Expression... args) {
        return member("notIn").call(args);
    }

    default Expression notIn(Collection<?> args) {
        return member("notIn").call(args);
    }

    default Expression notIn(Object... args) {
        return member("notIn").call(args);
    }

    default Expression concat(String arg) {
        return concat(literal(arg));
    }

    // value is in [start, end)
    default Expression between(Expression start, Expression end) {
        return member("between").call(start, end);
    }

    default Expression between(Object start, Object end) {
        return between(literal(start), literal(end));
    }

    default Expression startsWith(Expression arg) {
        return member("startsWith").call(arg);
    }

    default Expression startsWith(String arg) {
        return member("startsWith").call(arg);
    }

    default Expression contains(Expression arg) {
        return member("contains").call(arg);
    }

    default Expression contains(String arg) {
        return member("contains").call(arg);
    }

    default Expression endsWith(Expression arg) {
        return member("endsWith").call(arg);
    }

    default Expression endsWith(String arg) {
        return member("endsWith").call(arg);
    }

    default Expression cast(String dataType) {
        return member("cast").call(dataType);
    }

    default Expression isNull() {
        return member("isNull").call();
    }

    default Expression isNotNull() {
        return member("isNotNull").call();
    }

    default Expression distinct() {
        return member("distinct").call();
    }

    default CaseAble onCase() {
        return new CaseAbleImpl(this);
    }

    default Expression concat(Expression arg) {
        return id("CONCAT").call(this, arg);
    }

    // aggregate function
    default Expression count() {
        return id("COUNT").call(this);
    }

    default Expression sum() {
        return id("SUM").call(this);
    }

    default Expression avg() {
        return id("AVG").call(this);
    }

    default Expression min() {
        return id("MIN").call(this);
    }

    default Expression max() {
        return id("MAX").call(this);
    }

    default Expression over(String sql) {
        return member("over").call(id(sql));
    }

    default Expression over() {
        return over("");
    }

    enum Operator {
        PLUS(50), SUB(50), MUL(51), DIV(51), MOD(52),
        EQ(40), GE(40), GT(40), LE(40), LT(40), NE(40),
        AND(31), OR(30), NOT(32);

        public final int priority;

        Operator(int priority) {
            this.priority = priority;
        }
    }

    @Data
    @Builder
    class UnaryExpression implements Expression {
        private Expression operand;
        private Operator operator;

        @Override
        public int priority() {
            return operator.priority;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.onUnaryExpression(this);
        }

        static Expression of(Expression operand, Operator operator) {
            return UnaryExpression.builder().operand(operand).operator(operator).build();
        }
    }

    @Data
    @Builder
    class BinaryExpression implements Expression {
        private Expression leftOperand;
        private Operator operator;
        private Expression rightOperand;

        @Override
        public int priority() {
            return operator.priority;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.onBinaryExpression(this);
        }

        static Expression of(Expression leftOperand, Expression.Operator operator, Expression rightOperand) {
            return BinaryExpression.builder()
                    .leftOperand(leftOperand)
                    .operator(operator)
                    .rightOperand(rightOperand)
                    .build();
        }
    }

    @Data
    @Builder
    class MemberAccessExpression implements Expression {
        private Expression object;
        private String member;

        @Override
        public int priority() {
            return 200;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.onMemberAccessExpression(this);
        }

        static Expression of(Expression object, String member) {
            return MemberAccessExpression.builder().object(object).member(member).build();
        }
    }

    @Data
    @Builder
    class CallExpression implements Expression {
        private Expression function;
        private List<Expression> parameters;

        @Override
        public int priority() {
            return 200;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.onCallExpression(this);
        }

        static Expression of(Expression function, List<Expression> parameters) {
            return CallExpression.builder().function(function).parameters(parameters).build();
        }
    }

    @Data
    @Builder
    class JsonPathExpression implements Expression {
        private Expression object;
        private String path;
        private boolean unquoted;

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.onJsonPathExpression(this);
        }

        public static JsonPathExpression of(Expression object, String path, boolean unquoted) {
            return JsonPathExpression.builder()
                    .object(object)
                    .path(path)
                    .unquoted(unquoted)
                    .build();
        }
    }

    @Data
    @Builder
    class CaseExpression implements Expression {
        private List<CaseBranch> branches;

        @Override
        public int priority() {
            return 200;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.onCaseExpression(this);
        }

        static Expression of(List<Expression.CaseBranch> branches) {
            return CaseExpression.builder().branches(branches).build();
        }
    }

    @Data
    @Builder
    class CaseBranch {
        private Expression condition;
        private Tuple.Tuple2<Expression, Expression> value;

        static CaseBranch of(Expression condition, Tuple.Tuple2<Expression, Expression> value) {
            return CaseBranch.builder().condition(condition).value(value).build();
        }
    }

    interface CaseAble {
        CaseAble when(Expression condition);

        default CaseAble when(Object condition) {
            return when(literal(condition));
        }

        CaseAble then(Expression at, Expression value);

        default CaseAble then(Expression value) {
            return then(EMPTY, value);
        }

        default CaseAble then(String at, Object value) {
            return then(path(at), literal(value));
        }

        default CaseAble then(Object value) {
            return then(literal(value));
        }

        Expression Else(Expression result);

        default Expression Else() {
            return Else(EMPTY);
        }

        default Expression Else(Object result) {
            return Else(literal(result));
        }
    }

    class CaseAbleImpl implements CaseAble {
        private final Expression expression;

        private List<Expression> conditions;
        private List<Tuple.Tuple2<Expression, Expression>> values;

        public CaseAbleImpl(Expression expression) {
            this.expression = expression;
        }

        @Override
        public CaseAble when(Expression condition) {
            if (conditions == null) {
                this.conditions = new ArrayList<>();
            }
            this.conditions.add(condition);
            return this;
        }

        @Override
        public CaseAble then(Expression at, Expression value) {
            if (values == null) {
                this.values = new ArrayList<>();
            }
            this.values.add(Tuple.of(at, value));
            return this;
        }

        @Override
        public Expression Else(Expression result) {
            List<CaseBranch> branches = new ArrayList<>();
            branches.add(CaseBranch.of(expression, Tuple.of(EMPTY, EMPTY))); // head
            if (conditions != null && values != null) {
                if (conditions.size() != values.size()) {
                    throw new IllegalArgumentException("syntax error with case-when-then");
                }
                for (int i = 0; i < conditions.size(); i++) {
                    branches.add(CaseBranch.of(conditions.get(i), values.get(i)));
                }
            }
            Expression at = EMPTY;
            if (values != null && values.size() > 0) {
                at = values.get(0)._1;
            }
            branches.add(CaseBranch.of(EMPTY, Tuple.of(at, result))); // tail
            return CaseExpression.of(branches);
        }
    }

    @Data
    @Builder
    class Identifier implements Expression {
        private String name;

        @Override
        public int priority() {
            return 200;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.onIdentifier(this);
        }

        public static Identifier of(String name) {
            return Identifier.builder().name(name).build();
        }
    }

    @Data
    @Builder
    class Literal implements Expression {
        protected Object value;

        @Override
        public int priority() {
            return 200;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.onLiteral(this);
        }

        static Expression of(Object value) {
            return value == null ? EMPTY : Literal.builder().value(value).build();
        }
    }

    @Data
    @Builder
    class Parameter implements Expression {
        private String name;
        private Object value;

        @Override
        public <A> A visit(Visitor<A> visitor) {
            return visitor.onParameter(this);
        }

        @Override
        public int priority() {
            return 200;
        }

        public static Parameter of(String name, Object value) {
            return Parameter.builder().name(name).value(value).build();
        }
    }

    static boolean isEmpty(Expression expr) {
        return expr == null || expr == EMPTY;
    }

    Expression EMPTY = new Expression() {
        @Override
        public int priority() {
            return 200;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.onEmpty(this);
        }
    };

    abstract class AbstractVisitor<T> implements Visitor<T> {
        @Override
        public T onUnaryExpression(UnaryExpression expr) {
            return null;
        }

        @Override
        public T onBinaryExpression(BinaryExpression expr) {
            return null;
        }

        @Override
        public T onMemberAccessExpression(MemberAccessExpression expr) {
            return null;
        }

        @Override
        public T onCallExpression(CallExpression expr) {
            return null;
        }

        @Override
        public T onJsonPathExpression(JsonPathExpression expr) {
            return null;
        }

        @Override
        public T onCaseExpression(CaseExpression expr) {
            return null;
        }

        @Override
        public T onIdentifier(Identifier expr) {
            return null;
        }

        @Override
        public T onLiteral(Literal expr) {
            return null;
        }

        @Override
        public T onParameter(Parameter expr) {
            return null;
        }

        @Override
        public T onEmpty(Expression expr) {
            return null;
        }
    }

    static Predicate nonNull(Object value) {
        return when(value != null);
    }

    static Predicate nonEmpty(String value) {
        return when(value != null && !value.trim().isEmpty());
    }

    static Predicate when(boolean judge) {
        return expr -> judge ? expr : EMPTY;
    }

    interface Predicate {
        Expression then(Expression expr);
    }
}
