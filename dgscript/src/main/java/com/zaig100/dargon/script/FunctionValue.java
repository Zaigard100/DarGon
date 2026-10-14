package com.zaig100.dargon.script;

/**
 * Функция: объявленная в скрипте или нативная (Java). Вызывается через {@link ScriptEngine#call}.
 */
public abstract class FunctionValue implements Value {

    /** Число аргументов, или {@code -1} для функций с переменным числом аргументов. */
    public static final int VARIADIC = -1;

    private final String name;
    private final int arity;

    FunctionValue(String name, int arity) {
        this.name = name;
        this.arity = arity;
    }

    /** Имя функции (для лямбд — {@code "<lambda>"}). */
    public String name() {
        return name;
    }

    public int arity() {
        return arity;
    }

    abstract Value invoke(Interpreter interpreter, Value[] args);

    @Override
    public String typeName() {
        return "function";
    }

    @Override
    public String display() {
        return "<function " + name + ">";
    }

    @Override
    public String toString() {
        return display();
    }
}
