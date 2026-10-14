package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Инструкции синтаксического дерева. {@link #execute} возвращает способ завершения: {@link
 * Interpreter#NORMAL}, {@link Interpreter#BREAK}, {@link Interpreter#CONTINUE} или {@link
 * Interpreter#RETURN} — без исключений для управления потоком.
 */
abstract class Stmt {

    final SourcePosition position;

    Stmt(SourcePosition position) {
        this.position = position;
    }

    /** Выполняет инструкцию: тратит шаг лимита и проставляет позицию ошибкам без неё. */
    final int execute(Environment env) {
        // Блок — не остановка для отладчика: остановки на его инструкциях.
        env.interpreter.step(position, this instanceof Block ? null : env);
        try {
            return run(env);
        } catch (ScriptException e) {
            throw e.at(position);
        }
    }

    abstract int run(Environment env);

    // ------------------------------------------------------------------ простые

    /** Выражение-вызов как инструкция. */
    static final class Expression extends Stmt {
        final Expr expression;

        Expression(SourcePosition position, Expr expression) {
            super(position);
            this.expression = expression;
        }

        @Override
        int run(Environment env) {
            expression.evaluate(env);
            return Interpreter.NORMAL;
        }
    }

    /** {@code target = value}, {@code target += value} и т.п. */
    static final class Assign extends Stmt {
        final Expr target;
        final TokenType operator;
        final Expr value;

        Assign(SourcePosition position, Expr target, TokenType operator, Expr value) {
            super(position);
            this.target = target;
            this.operator = operator;
            this.value = value;
        }

        @Override
        int run(Environment env) {
            if (target instanceof Expr.Variable variable) {
                Value result = value.evaluate(env);
                if (operator != TokenType.ASSIGN) {
                    result = combine(variable.evaluate(env), result);
                }
                env.assign(variable.name, result);
            } else if (target instanceof Expr.Member member) {
                Value object = member.target.evaluate(env);
                Value result = value.evaluate(env);
                if (operator != TokenType.ASSIGN) {
                    result = combine(Expr.Member.get(object, member.name, member), result);
                }
                Expr.Member.set(object, member.name, result, member);
            } else if (target instanceof Expr.Index index) {
                Value object = index.target.evaluate(env);
                Value key = index.index.evaluate(env);
                Value result = value.evaluate(env);
                if (operator != TokenType.ASSIGN) {
                    result = combine(Expr.Index.get(object, key, index), result);
                }
                Expr.Index.set(object, key, result, index);
            } else {
                throw new IllegalStateException("недопустимая цель присваивания");
            }
            return Interpreter.NORMAL;
        }

        private Value combine(Value current, Value operand) {
            TokenType binary =
                    switch (operator) {
                        case PLUS_ASSIGN -> TokenType.PLUS;
                        case MINUS_ASSIGN -> TokenType.MINUS;
                        case STAR_ASSIGN -> TokenType.STAR;
                        case SLASH_ASSIGN -> TokenType.SLASH;
                        default -> throw new IllegalStateException("оператор " + operator);
                    };
            return Expr.Binary.apply(binary, current, operand, value);
        }
    }

    static final class Block extends Stmt {
        final List<Stmt> statements;

        Block(SourcePosition position, List<Stmt> statements) {
            super(position);
            this.statements = statements;
        }

        @Override
        int run(Environment env) {
            for (Stmt statement : statements) {
                int completion = statement.execute(env);
                if (completion != Interpreter.NORMAL) {
                    return completion;
                }
            }
            return Interpreter.NORMAL;
        }
    }

    // ------------------------------------------------------------------ управление

    static final class If extends Stmt {
        final Expr condition;
        final Stmt then;
        final Stmt otherwise;

        If(SourcePosition position, Expr condition, Stmt then, Stmt otherwise) {
            super(position);
            this.condition = condition;
            this.then = then;
            this.otherwise = otherwise;
        }

        @Override
        int run(Environment env) {
            if (condition.evaluate(env).isTruthy()) {
                return then.execute(env);
            }
            return otherwise == null ? Interpreter.NORMAL : otherwise.execute(env);
        }
    }

    static final class While extends Stmt {
        final Expr condition;
        final Stmt body;

        While(SourcePosition position, Expr condition, Stmt body) {
            super(position);
            this.condition = condition;
            this.body = body;
        }

        @Override
        int run(Environment env) {
            while (condition.evaluate(env).isTruthy()) {
                int completion = body.execute(env);
                if (completion == Interpreter.BREAK) {
                    break;
                }
                if (completion == Interpreter.RETURN) {
                    return completion;
                }
            }
            return Interpreter.NORMAL;
        }
    }

    static final class DoWhile extends Stmt {
        final Stmt body;
        final Expr condition;

        DoWhile(SourcePosition position, Stmt body, Expr condition) {
            super(position);
            this.body = body;
            this.condition = condition;
        }

        @Override
        int run(Environment env) {
            do {
                int completion = body.execute(env);
                if (completion == Interpreter.BREAK) {
                    break;
                }
                if (completion == Interpreter.RETURN) {
                    return completion;
                }
                env.interpreter.step(position, env);
            } while (condition.evaluate(env).isTruthy());
            return Interpreter.NORMAL;
        }
    }

    /** {@code for (init, condition, increment) body} — в стиле старого DGScript. */
    static final class For extends Stmt {
        final Stmt init;
        final Expr condition;
        final Stmt increment;
        final Stmt body;

        For(SourcePosition position, Stmt init, Expr condition, Stmt increment, Stmt body) {
            super(position);
            this.init = init;
            this.condition = condition;
            this.increment = increment;
            this.body = body;
        }

        @Override
        int run(Environment env) {
            for (init.execute(env); condition.evaluate(env).isTruthy(); increment.execute(env)) {
                int completion = body.execute(env);
                if (completion == Interpreter.BREAK) {
                    break;
                }
                if (completion == Interpreter.RETURN) {
                    return completion;
                }
            }
            return Interpreter.NORMAL;
        }
    }

    /** {@code for x in collection body}: элементы массива, ключи словаря, символы строки. */
    static final class ForIn extends Stmt {
        final String variable;
        final Expr collection;
        final Stmt body;

        ForIn(SourcePosition position, String variable, Expr collection, Stmt body) {
            super(position);
            this.variable = variable;
            this.collection = collection;
            this.body = body;
        }

        @Override
        int run(Environment env) {
            Value source = collection.evaluate(env);
            List<Value> items;
            if (source instanceof ArrayValue array) {
                items = new ArrayList<>(array.items()); // снимок: тело может менять массив
            } else if (source instanceof MapValue map) {
                items = new ArrayList<>(map.entries().size());
                for (Map.Entry<String, Value> e : map.entries().entrySet()) {
                    items.add(StringValue.of(e.getKey()));
                }
            } else if (source instanceof StringValue string) {
                String s = string.value();
                items = new ArrayList<>(s.length());
                for (int i = 0; i < s.length(); i++) {
                    items.add(StringValue.of(String.valueOf(s.charAt(i))));
                }
            } else {
                throw new ScriptException(
                        "for-in: нельзя перебрать значение типа " + source.typeName(),
                        collection.position);
            }
            for (Value item : items) {
                env.assign(variable, item);
                int completion = body.execute(env);
                if (completion == Interpreter.BREAK) {
                    break;
                }
                if (completion == Interpreter.RETURN) {
                    return completion;
                }
            }
            return Interpreter.NORMAL;
        }
    }

    static final class Break extends Stmt {
        Break(SourcePosition position) {
            super(position);
        }

        @Override
        int run(Environment env) {
            return Interpreter.BREAK;
        }
    }

    static final class Continue extends Stmt {
        Continue(SourcePosition position) {
            super(position);
        }

        @Override
        int run(Environment env) {
            return Interpreter.CONTINUE;
        }
    }

    static final class Return extends Stmt {
        final Expr value;

        Return(SourcePosition position, Expr value) {
            super(position);
            this.value = value;
        }

        @Override
        int run(Environment env) {
            env.interpreter.returnValue = value == null ? NullValue.INSTANCE : value.evaluate(env);
            return Interpreter.RETURN;
        }
    }

    /** {@code def name(params) body} — объявление функции в текущей области. */
    static final class FunctionDeclaration extends Stmt {
        final Expr.FunctionLiteral function;

        FunctionDeclaration(SourcePosition position, Expr.FunctionLiteral function) {
            super(position);
            this.function = function;
        }

        @Override
        int run(Environment env) {
            env.assign(function.name, function.evaluate(env));
            return Interpreter.NORMAL;
        }
    }

    /** {@code use "module"}. Модуль подключается движком при выполнении. */
    static final class Use extends Stmt {
        final String module;

        Use(SourcePosition position, String module) {
            super(position);
            this.module = module;
        }

        @Override
        int run(Environment env) {
            env.interpreter.modules.load(module);
            return Interpreter.NORMAL;
        }
    }

    /** Подключение модулей по имени — реализует {@link ScriptEngine}. */
    interface ModuleLoader {
        void load(String module);
    }
}
