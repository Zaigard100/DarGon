package com.zaig100.dargon.script;

import java.util.List;
import java.util.Map;

/** Создание значений, сравнение и проверки типов для нативных функций. */
public final class Values {

    private static final int MAX_DISPLAY_DEPTH = 8;

    private Values() {}

    public static Value of(double number) {
        return NumberValue.of(number);
    }

    public static Value of(boolean bool) {
        return BoolValue.of(bool);
    }

    /** Строка; {@code null} превращается в {@code null}-значение. */
    public static Value of(String string) {
        return string == null ? NullValue.INSTANCE : StringValue.of(string);
    }

    public static Value nil() {
        return NullValue.INSTANCE;
    }

    public static ArrayValue array(List<Value> items) {
        return new ArrayValue(items);
    }

    /** Нативная функция как значение (например, чтобы положить её в словарь). */
    public static FunctionValue function(String name, int arity, NativeFunction function) {
        return new NativeFunctionValue(name, arity, function);
    }

    /** Равенство {@code ==}: по значению для null/bool/number/string, иначе по ссылке. */
    public static boolean equal(Value a, Value b) {
        if (a == b) {
            return true;
        }
        if (a instanceof NumberValue x && b instanceof NumberValue y) {
            return x.value() == y.value();
        }
        if (a instanceof StringValue x && b instanceof StringValue y) {
            return x.value().equals(y.value());
        }
        if (a instanceof ScriptObject && b instanceof ScriptObject) {
            return a.equals(b);
        }
        return false;
    }

    // ---------------------------------------------------------------- проверки аргументов

    /** Аргумент-число или ошибка вида «abs: аргумент 1 должен быть числом, получено string». */
    public static double number(String function, Value[] args, int index) {
        if (args[index] instanceof NumberValue n) {
            return n.value();
        }
        throw typeError(function, index, "числом", args[index]);
    }

    /** Аргумент — целое число. */
    public static int integer(String function, Value[] args, int index) {
        if (args[index] instanceof NumberValue n && n.isInteger()) {
            return n.intValue();
        }
        throw typeError(function, index, "целым числом", args[index]);
    }

    public static String string(String function, Value[] args, int index) {
        if (args[index] instanceof StringValue s) {
            return s.value();
        }
        throw typeError(function, index, "строкой", args[index]);
    }

    public static ArrayValue array(String function, Value[] args, int index) {
        if (args[index] instanceof ArrayValue a) {
            return a;
        }
        throw typeError(function, index, "массивом", args[index]);
    }

    public static MapValue map(String function, Value[] args, int index) {
        if (args[index] instanceof MapValue m) {
            return m;
        }
        throw typeError(function, index, "словарём", args[index]);
    }

    public static FunctionValue function(String function, Value[] args, int index) {
        if (args[index] instanceof FunctionValue f) {
            return f;
        }
        throw typeError(function, index, "функцией", args[index]);
    }

    private static ScriptException typeError(
            String function, int index, String expected, Value got) {
        return new ScriptException(
                function
                        + ": аргумент "
                        + (index + 1)
                        + " должен быть "
                        + expected
                        + ", получено "
                        + got.typeName());
    }

    // ---------------------------------------------------------------- отображение

    static String displayNested(Value value, int depth) {
        if (value instanceof StringValue s) {
            return depth == 0 ? s.value() : '"' + s.value() + '"';
        }
        if (depth > MAX_DISPLAY_DEPTH) {
            return "...";
        }
        if (value instanceof ArrayValue array) {
            StringBuilder sb = new StringBuilder("[");
            List<Value> items = array.items();
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(displayNested(items.get(i), depth + 1));
            }
            return sb.append(']').toString();
        }
        if (value instanceof MapValue map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Value> e : map.entries().entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(e.getKey()).append(": ").append(displayNested(e.getValue(), depth + 1));
            }
            return sb.append('}').toString();
        }
        return value.display();
    }
}
