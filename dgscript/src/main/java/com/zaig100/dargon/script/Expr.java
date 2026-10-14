package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.List;

/**
 * Выражения синтаксического дерева. Каждое знает свою позицию в исходнике для сообщений об ошибках.
 */
abstract class Expr {

    final SourcePosition position;

    Expr(SourcePosition position) {
        this.position = position;
    }

    abstract Value evaluate(Environment env);

    ScriptException error(String message) {
        return new ScriptException(message, position);
    }

    // ------------------------------------------------------------------ литералы и переменные

    static final class Literal extends Expr {
        final Value value;

        Literal(SourcePosition position, Value value) {
            super(position);
            this.value = value;
        }

        @Override
        Value evaluate(Environment env) {
            return value;
        }
    }

    static final class Variable extends Expr {
        final String name;

        Variable(SourcePosition position, String name) {
            super(position);
            this.name = name;
        }

        @Override
        Value evaluate(Environment env) {
            Value value = env.lookup(name);
            if (value == null) {
                throw error("неизвестная переменная '" + name + "'");
            }
            return value;
        }
    }

    static final class ArrayLiteral extends Expr {
        final List<Expr> items;

        ArrayLiteral(SourcePosition position, List<Expr> items) {
            super(position);
            this.items = items;
        }

        @Override
        Value evaluate(Environment env) {
            List<Value> values = new ArrayList<>(items.size());
            for (Expr item : items) {
                values.add(item.evaluate(env));
            }
            return new ArrayValue(values);
        }
    }

    static final class MapLiteral extends Expr {
        final List<String> keys;
        final List<Expr> values;

        MapLiteral(SourcePosition position, List<String> keys, List<Expr> values) {
            super(position);
            this.keys = keys;
            this.values = values;
        }

        @Override
        Value evaluate(Environment env) {
            MapValue map = new MapValue();
            for (int i = 0; i < keys.size(); i++) {
                map.put(keys.get(i), values.get(i).evaluate(env));
            }
            return map;
        }
    }

    static final class FunctionLiteral extends Expr {
        final String name;
        final List<String> parameters;
        final Stmt body;

        FunctionLiteral(SourcePosition position, String name, List<String> parameters, Stmt body) {
            super(position);
            this.name = name;
            this.parameters = parameters;
            this.body = body;
        }

        @Override
        Value evaluate(Environment env) {
            return new UserFunction(name, parameters, body, env);
        }
    }

    // ------------------------------------------------------------------ операторы

    static final class Unary extends Expr {
        final TokenType operator;
        final Expr operand;

        Unary(SourcePosition position, TokenType operator, Expr operand) {
            super(position);
            this.operator = operator;
            this.operand = operand;
        }

        @Override
        Value evaluate(Environment env) {
            Value value = operand.evaluate(env);
            if (operator == TokenType.BANG) {
                return BoolValue.of(!value.isTruthy());
            }
            if (value instanceof NumberValue n) {
                return NumberValue.of(-n.value());
            }
            throw error("унарный минус применим только к числу, получено " + value.typeName());
        }
    }

    static final class Binary extends Expr {
        final TokenType operator;
        final Expr left;
        final Expr right;

        Binary(SourcePosition position, TokenType operator, Expr left, Expr right) {
            super(position);
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        Value evaluate(Environment env) {
            return apply(operator, left.evaluate(env), right.evaluate(env), this);
        }

        static Value apply(TokenType operator, Value a, Value b, Expr at) {
            switch (operator) {
                case EQ:
                    return BoolValue.of(Values.equal(a, b));
                case NOT_EQ:
                    return BoolValue.of(!Values.equal(a, b));
                case PLUS:
                    if (a instanceof StringValue || b instanceof StringValue) {
                        return StringValue.of(a.display() + b.display());
                    }
                    break;
                case LT:
                case LT_EQ:
                case GT:
                case GT_EQ:
                    return compare(operator, a, b, at);
                default:
                    break;
            }
            if (!(a instanceof NumberValue x) || !(b instanceof NumberValue y)) {
                throw at.error(
                        "оператор '"
                                + symbol(operator)
                                + "' не применим к "
                                + a.typeName()
                                + " и "
                                + b.typeName());
            }
            double l = x.value();
            double r = y.value();
            return switch (operator) {
                case PLUS -> NumberValue.of(l + r);
                case MINUS -> NumberValue.of(l - r);
                case STAR -> NumberValue.of(l * r);
                case SLASH -> {
                    if (r == 0) {
                        throw at.error("деление на ноль");
                    }
                    yield NumberValue.of(l / r);
                }
                case PERCENT -> {
                    if (r == 0) {
                        throw at.error("остаток от деления на ноль");
                    }
                    yield NumberValue.of(l % r);
                }
                default -> throw new IllegalStateException("оператор " + operator);
            };
        }

        private static Value compare(TokenType operator, Value a, Value b, Expr at) {
            int cmp;
            if (a instanceof NumberValue x && b instanceof NumberValue y) {
                cmp = Double.compare(x.value(), y.value());
            } else if (a instanceof StringValue x && b instanceof StringValue y) {
                cmp = x.value().compareTo(y.value());
            } else {
                throw at.error(
                        "нельзя сравнить "
                                + a.typeName()
                                + " и "
                                + b.typeName()
                                + " оператором '"
                                + symbol(operator)
                                + "'");
            }
            return BoolValue.of(
                    switch (operator) {
                        case LT -> cmp < 0;
                        case LT_EQ -> cmp <= 0;
                        case GT -> cmp > 0;
                        case GT_EQ -> cmp >= 0;
                        default -> throw new IllegalStateException("оператор " + operator);
                    });
        }

        static String symbol(TokenType operator) {
            return switch (operator) {
                case PLUS, PLUS_ASSIGN -> "+";
                case MINUS, MINUS_ASSIGN -> "-";
                case STAR, STAR_ASSIGN -> "*";
                case SLASH, SLASH_ASSIGN -> "/";
                case PERCENT -> "%";
                case LT -> "<";
                case LT_EQ -> "<=";
                case GT -> ">";
                case GT_EQ -> ">=";
                case EQ -> "==";
                case NOT_EQ -> "!=";
                default -> operator.name();
            };
        }
    }

    /** {@code &&} и {@code ||} с коротким замыканием. Результат — всегда bool. */
    static final class Logical extends Expr {
        final boolean isAnd;
        final Expr left;
        final Expr right;

        Logical(SourcePosition position, boolean isAnd, Expr left, Expr right) {
            super(position);
            this.isAnd = isAnd;
            this.left = left;
            this.right = right;
        }

        @Override
        Value evaluate(Environment env) {
            boolean l = left.evaluate(env).isTruthy();
            if (isAnd ? !l : l) {
                return BoolValue.of(l);
            }
            return BoolValue.of(right.evaluate(env).isTruthy());
        }
    }

    // ------------------------------------------------------------------ доступ и вызовы

    /** {@code obj.name}. */
    static final class Member extends Expr {
        final Expr target;
        final String name;

        Member(SourcePosition position, Expr target, String name) {
            super(position);
            this.target = target;
            this.name = name;
        }

        @Override
        Value evaluate(Environment env) {
            return get(target.evaluate(env), name, this);
        }

        static Value get(Value target, String name, Expr at) {
            if (target instanceof MapValue map) {
                return map.get(name);
            }
            if (target instanceof ScriptObject object) {
                try {
                    Value value = object.getMember(name);
                    return value == null ? NullValue.INSTANCE : value;
                } catch (ScriptException e) {
                    throw e.at(at.position);
                }
            }
            if ("length".equals(name)) {
                if (target instanceof ArrayValue array) {
                    return NumberValue.of(array.size());
                }
                if (target instanceof StringValue string) {
                    return NumberValue.of(string.value().length());
                }
            }
            throw at.error("у значения типа " + target.typeName() + " нет поля '" + name + "'");
        }

        static void set(Value target, String name, Value value, Expr at) {
            if (target instanceof MapValue map) {
                map.put(name, value);
                return;
            }
            if (target instanceof ScriptObject object) {
                try {
                    object.setMember(name, value);
                    return;
                } catch (ScriptException e) {
                    throw e.at(at.position);
                }
            }
            throw at.error(
                    "нельзя задать поле '" + name + "' у значения типа " + target.typeName());
        }
    }

    /** {@code obj[index]}. */
    static final class Index extends Expr {
        final Expr target;
        final Expr index;

        Index(SourcePosition position, Expr target, Expr index) {
            super(position);
            this.target = target;
            this.index = index;
        }

        @Override
        Value evaluate(Environment env) {
            return get(target.evaluate(env), index.evaluate(env), this);
        }

        static Value get(Value target, Value index, Expr at) {
            if (target instanceof ArrayValue array) {
                return array.get(position(index, array.size(), at));
            }
            if (target instanceof StringValue string) {
                int i = position(index, string.value().length(), at);
                return StringValue.of(String.valueOf(string.value().charAt(i)));
            }
            if (target instanceof MapValue map) {
                return map.get(key(index, at));
            }
            throw at.error("значение типа " + target.typeName() + " нельзя индексировать");
        }

        static void set(Value target, Value index, Value value, Expr at) {
            if (target instanceof ArrayValue array) {
                array.items().set(position(index, array.size(), at), value);
                return;
            }
            if (target instanceof MapValue map) {
                map.put(key(index, at), value);
                return;
            }
            throw at.error(
                    "в значение типа " + target.typeName() + " нельзя записывать по индексу");
        }

        private static int position(Value index, int size, Expr at) {
            if (!(index instanceof NumberValue n) || !n.isInteger()) {
                throw at.error("индекс должен быть целым числом, получено " + index.display());
            }
            int i = n.intValue();
            if (i < 0 || i >= size) {
                throw at.error("индекс " + i + " вне границ [0, " + size + ")");
            }
            return i;
        }

        private static String key(Value index, Expr at) {
            if (index instanceof StringValue s) {
                return s.value();
            }
            throw at.error("ключ словаря должен быть строкой, получено " + index.typeName());
        }
    }

    /** Вызов функции или метода объекта хоста. */
    static final class Call extends Expr {
        final Expr callee;
        final List<Expr> arguments;

        Call(SourcePosition position, Expr callee, List<Expr> arguments) {
            super(position);
            this.callee = callee;
            this.arguments = arguments;
        }

        @Override
        Value evaluate(Environment env) {
            Interpreter interpreter = env.interpreter;
            if (callee instanceof Member member) {
                Value target = member.target.evaluate(env);
                if (target instanceof ScriptObject object) {
                    Value[] args = evaluateArguments(env);
                    try {
                        Value result = object.callMethod(member.name, args);
                        return result == null ? NullValue.INSTANCE : result;
                    } catch (ScriptException e) {
                        throw e.at(position);
                    }
                }
                return invoke(interpreter, Member.get(target, member.name, member), env);
            }
            if (callee instanceof Variable variable) {
                Value function = env.lookup(variable.name);
                if (function == null) {
                    throw error("неизвестная функция '" + variable.name + "'");
                }
                return invoke(interpreter, function, env);
            }
            return invoke(interpreter, callee.evaluate(env), env);
        }

        private Value invoke(Interpreter interpreter, Value function, Environment env) {
            if (!(function instanceof FunctionValue f)) {
                throw error(
                        describeCallee() + " не является функцией (" + function.typeName() + ")");
            }
            return interpreter.call(f, evaluateArguments(env), position);
        }

        private Value[] evaluateArguments(Environment env) {
            Value[] args = new Value[arguments.size()];
            for (int i = 0; i < args.length; i++) {
                args[i] = arguments.get(i).evaluate(env);
            }
            return args;
        }

        private String describeCallee() {
            if (callee instanceof Variable v) {
                return "'" + v.name + "'";
            }
            if (callee instanceof Member m) {
                return "'" + m.name + "'";
            }
            return "значение";
        }
    }
}
