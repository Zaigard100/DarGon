package com.zaig100.dargon.script;

/** Обёртка нативной функции как значения скрипта. */
final class NativeFunctionValue extends FunctionValue {

    private final NativeFunction function;

    NativeFunctionValue(String name, int arity, NativeFunction function) {
        super(name, arity);
        this.function = function;
    }

    @Override
    Value invoke(Interpreter interpreter, Value[] args) {
        Value result = function.call(args);
        return result == null ? NullValue.INSTANCE : result;
    }
}
