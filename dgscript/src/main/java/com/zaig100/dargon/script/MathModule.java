package com.zaig100.dargon.script;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Модуль {@code math}: подключается через {@code use "math"}. */
final class MathModule {

    private MathModule() {}

    static void install(ScriptModule.Scope scope) {
        scope.constant("PI", NumberValue.of(Math.PI));
        scope.constant("E", NumberValue.of(Math.E));

        unary(scope, "abs", Math::abs);
        unary(scope, "floor", Math::floor);
        unary(scope, "ceil", Math::ceil);
        unary(scope, "sqrt", Math::sqrt);
        unary(scope, "sin", Math::sin);
        unary(scope, "cos", Math::cos);
        unary(scope, "tan", Math::tan);
        unary(scope, "sign", Math::signum);
        unary(scope, "toRadians", Math::toRadians);
        unary(scope, "toDegrees", Math::toDegrees);

        scope.function(
                "pow",
                2,
                args ->
                        NumberValue.of(
                                Math.pow(
                                        Values.number("pow", args, 0),
                                        Values.number("pow", args, 1))));
        scope.function(
                "atan2",
                2,
                args ->
                        NumberValue.of(
                                Math.atan2(
                                        Values.number("atan2", args, 0),
                                        Values.number("atan2", args, 1))));
        scope.function(
                "clamp",
                3,
                args -> {
                    double x = Values.number("clamp", args, 0);
                    double lo = Values.number("clamp", args, 1);
                    double hi = Values.number("clamp", args, 2);
                    return NumberValue.of(Math.max(lo, Math.min(hi, x)));
                });
        scope.function("min", FunctionValue.VARIADIC, args -> extreme("min", args, true));
        scope.function("max", FunctionValue.VARIADIC, args -> extreme("max", args, false));
        scope.function("round", FunctionValue.VARIADIC, MathModule::round);
    }

    private interface DoubleOp {
        double apply(double x);
    }

    private static void unary(ScriptModule.Scope scope, String name, DoubleOp op) {
        scope.function(name, 1, args -> NumberValue.of(op.apply(Values.number(name, args, 0))));
    }

    private static Value extreme(String name, Value[] args, boolean min) {
        if (args.length == 0) {
            throw new ScriptException(name + ": нужен хотя бы один аргумент");
        }
        double result = Values.number(name, args, 0);
        for (int i = 1; i < args.length; i++) {
            double x = Values.number(name, args, i);
            result = min ? Math.min(result, x) : Math.max(result, x);
        }
        return NumberValue.of(result);
    }

    /** round(x) — до целого; round(x, places) — до знаков после запятой (половина — вверх). */
    private static Value round(Value[] args) {
        if (args.length == 1) {
            return NumberValue.of(Math.round(Values.number("round", args, 0)));
        }
        if (args.length == 2) {
            int places = Values.integer("round", args, 1);
            if (places < 0) {
                throw new ScriptException("round: число знаков не может быть отрицательным");
            }
            BigDecimal value = BigDecimal.valueOf(Values.number("round", args, 0));
            return NumberValue.of(value.setScale(places, RoundingMode.HALF_UP).doubleValue());
        }
        throw new ScriptException("round: ожидается 1 или 2 аргумента, передано " + args.length);
    }
}
