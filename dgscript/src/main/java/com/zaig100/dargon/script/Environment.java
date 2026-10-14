package com.zaig100.dargon.script;

import java.util.HashMap;
import java.util.Map;

/**
 * Область видимости переменных. Создаётся на каждый вызов функции; блоки ({@code if}, циклы) своей
 * области не создают.
 *
 * <p>Присваивание {@code x = v} меняет ближайшую область, где {@code x} уже есть, иначе создаёт
 * {@code x} в текущей. Область встроенных функций «заморожена»: присваивание не меняет её, а
 * создаёт глобальную переменную с тем же именем.
 */
final class Environment {

    final Interpreter interpreter;
    private final Environment parent;
    private final Map<String, Value> variables = new HashMap<>();
    private final boolean frozen;

    private Environment(Interpreter interpreter, Environment parent, boolean frozen) {
        this.interpreter = interpreter;
        this.parent = parent;
        this.frozen = frozen;
    }

    static Environment builtins(Interpreter interpreter) {
        return new Environment(interpreter, null, true);
    }

    Environment child() {
        return new Environment(interpreter, this, false);
    }

    /** Значение переменной или {@code null} (Java), если её нет ни в одной области. */
    Value lookup(String name) {
        for (Environment env = this; env != null; env = env.parent) {
            Value value = env.variables.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    void assign(String name, Value value) {
        for (Environment env = this; env != null && !env.frozen; env = env.parent) {
            if (env.variables.containsKey(name)) {
                env.variables.put(name, value);
                return;
            }
        }
        define(name, value);
    }

    void define(String name, Value value) {
        if (frozen) {
            throw new IllegalStateException("область встроенных функций только для чтения");
        }
        variables.put(name, value);
    }

    /** Запись во встроенную область — только движком при установке модулей. */
    void defineBuiltin(String name, Value value) {
        variables.put(name, value);
    }

    Value getLocal(String name) {
        return variables.get(name);
    }

    /** Переменные этой области (для отладчика). */
    Map<String, Value> variables() {
        return java.util.Collections.unmodifiableMap(variables);
    }

    Environment parent() {
        return parent;
    }
}
