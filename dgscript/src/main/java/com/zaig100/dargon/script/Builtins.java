package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** Встроенные функции, доступные всегда (без {@code use}). */
final class Builtins {

    private Builtins() {}

    static void install(
            ScriptModule.Scope scope, Consumer<String> output, Random random, Timers timers) {
        // --- вывод
        NativeFunction print =
                args -> {
                    output.accept(join(args));
                    return null;
                };
        scope.function("print", FunctionValue.VARIADIC, print);
        scope.function(
                "println", FunctionValue.VARIADIC, print); // совместимость со старым DGScript

        // --- типы и преобразования
        scope.function("type", 1, args -> StringValue.of(args[0].typeName()));
        scope.function("str", 1, args -> StringValue.of(args[0].display()));
        scope.function("num", 1, Builtins::toNumber);
        scope.function("int", 1, args -> NumberValue.of((long) Values.number("int", args, 0)));
        scope.function("len", 1, Builtins::length);

        // --- коллекции
        scope.function(
                "push",
                FunctionValue.VARIADIC,
                args -> {
                    requireAtLeast("push", args, 2);
                    ArrayValue array = Values.array("push", args, 0);
                    for (int i = 1; i < args.length; i++) {
                        array.add(args[i]);
                    }
                    return NumberValue.of(array.size());
                });
        scope.function(
                "pop",
                1,
                args -> {
                    ArrayValue array = Values.array("pop", args, 0);
                    if (array.size() == 0) {
                        throw new ScriptException("pop: массив пуст");
                    }
                    return array.items().remove(array.size() - 1);
                });
        scope.function("remove", 2, Builtins::remove);
        scope.function("contains", 2, Builtins::contains);
        scope.function(
                "keys",
                1,
                args -> {
                    List<Value> keys = new ArrayList<>();
                    for (String key : Values.map("keys", args, 0).entries().keySet()) {
                        keys.add(StringValue.of(key));
                    }
                    return new ArrayValue(keys);
                });
        scope.function(
                "join",
                2,
                args -> {
                    ArrayValue array = Values.array("join", args, 0);
                    String separator = Values.string("join", args, 1);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < array.size(); i++) {
                        if (i > 0) {
                            sb.append(separator);
                        }
                        sb.append(array.get(i).display());
                    }
                    return StringValue.of(sb.toString());
                });
        scope.function("range", FunctionValue.VARIADIC, Builtins::range);
        scope.function(
                "array",
                FunctionValue.VARIADIC,
                args -> {
                    requireAtLeast("array", args, 1);
                    return filledArray(args, 0);
                });

        // --- случайные числа
        scope.function("rand", FunctionValue.VARIADIC, args -> rand(random, args));

        // --- таймеры
        scope.function(
                "after",
                2,
                args ->
                        NumberValue.of(
                                timers.after(
                                        Values.number("after", args, 0),
                                        Values.function("after", args, 1))));
        scope.function(
                "every",
                2,
                args ->
                        NumberValue.of(
                                timers.every(
                                        Values.number("every", args, 0),
                                        Values.function("every", args, 1))));
        scope.function(
                "cancel",
                1,
                args -> BoolValue.of(timers.cancel(Values.integer("cancel", args, 0))));
    }

    private static String join(Value[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(args[i].display());
        }
        return sb.toString();
    }

    private static Value toNumber(Value[] args) {
        Value value = args[0];
        if (value instanceof NumberValue) {
            return value;
        }
        if (value instanceof BoolValue b) {
            return b.value() ? NumberValue.ONE : NumberValue.ZERO;
        }
        if (value instanceof StringValue s) {
            try {
                return NumberValue.of(Double.parseDouble(s.value().trim()));
            } catch (NumberFormatException e) {
                return NullValue.INSTANCE;
            }
        }
        throw new ScriptException("num: нельзя превратить " + value.typeName() + " в число");
    }

    private static Value length(Value[] args) {
        Value value = args[0];
        if (value instanceof ArrayValue a) {
            return NumberValue.of(a.size());
        }
        if (value instanceof StringValue s) {
            return NumberValue.of(s.value().length());
        }
        if (value instanceof MapValue m) {
            return NumberValue.of(m.entries().size());
        }
        throw new ScriptException("len: у значения типа " + value.typeName() + " нет длины");
    }

    private static Value remove(Value[] args) {
        if (args[0] instanceof ArrayValue array) {
            int index = Values.integer("remove", args, 1);
            if (index < 0 || index >= array.size()) {
                throw new ScriptException(
                        "remove: индекс " + index + " вне границ [0, " + array.size() + ")");
            }
            return array.items().remove(index);
        }
        if (args[0] instanceof MapValue map) {
            Value removed = map.entries().remove(Values.string("remove", args, 1));
            return removed == null ? NullValue.INSTANCE : removed;
        }
        throw new ScriptException("remove: аргумент 1 должен быть массивом или словарём");
    }

    private static Value contains(Value[] args) {
        Value collection = args[0];
        if (collection instanceof ArrayValue array) {
            for (Value item : array.items()) {
                if (Values.equal(item, args[1])) {
                    return BoolValue.TRUE;
                }
            }
            return BoolValue.FALSE;
        }
        if (collection instanceof MapValue map) {
            return BoolValue.of(map.entries().containsKey(Values.string("contains", args, 1)));
        }
        if (collection instanceof StringValue s) {
            return BoolValue.of(s.value().contains(Values.string("contains", args, 1)));
        }
        throw new ScriptException(
                "contains: аргумент 1 должен быть массивом, словарём или строкой");
    }

    /** range(n), range(from, to), range(from, to, step) — как в Python, конец не включается. */
    private static Value range(Value[] args) {
        if (args.length < 1 || args.length > 3) {
            throw new ScriptException(
                    "range: ожидается от 1 до 3 аргументов, передано " + args.length);
        }
        double from = args.length == 1 ? 0 : Values.number("range", args, 0);
        double to = Values.number("range", args, args.length == 1 ? 0 : 1);
        double step = args.length == 3 ? Values.number("range", args, 2) : 1;
        if (step == 0) {
            throw new ScriptException("range: шаг не может быть нулём");
        }
        double count = Math.ceil((to - from) / step);
        if (count > 1_000_000) {
            throw new ScriptException("range: слишком много элементов (" + (long) count + ")");
        }
        List<Value> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(NumberValue.of(from + i * step));
        }
        return new ArrayValue(items);
    }

    /** array(n) — массив из n нулей; array(n, m) — n массивов по m нулей и т.д. */
    private static Value filledArray(Value[] args, int dimension) {
        int size = Values.integer("array", args, dimension);
        if (size < 0) {
            throw new ScriptException("array: размер не может быть отрицательным");
        }
        List<Value> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(
                    dimension == args.length - 1
                            ? NumberValue.ZERO
                            : filledArray(args, dimension + 1));
        }
        return new ArrayValue(items);
    }

    /** rand() — [0, 1); rand(n) — целое [0, n); rand(a, b) — целое [a, b]. */
    private static Value rand(Random random, Value[] args) {
        switch (args.length) {
            case 0:
                return NumberValue.of(random.nextDouble());
            case 1:
                {
                    int n = Values.integer("rand", args, 0);
                    if (n <= 0) {
                        throw new ScriptException("rand: n должно быть больше нуля");
                    }
                    return NumberValue.of(random.nextInt(n));
                }
            case 2:
                {
                    int a = Values.integer("rand", args, 0);
                    int b = Values.integer("rand", args, 1);
                    if (b < a) {
                        throw new ScriptException("rand: верхняя граница меньше нижней");
                    }
                    return NumberValue.of(a + random.nextInt(b - a + 1));
                }
            default:
                throw new ScriptException("rand: ожидается от 0 до 2 аргументов");
        }
    }

    private static void requireAtLeast(String function, Value[] args, int count) {
        if (args.length < count) {
            throw new ScriptException(
                    function
                            + ": ожидается хотя бы "
                            + count
                            + " аргумента, передано "
                            + args.length);
        }
    }
}
