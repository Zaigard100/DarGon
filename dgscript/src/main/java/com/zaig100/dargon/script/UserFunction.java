package com.zaig100.dargon.script;

import java.util.List;

/** Функция, объявленная в скрипте ({@code def}), вместе с областью, в которой она создана. */
final class UserFunction extends FunctionValue {

    private final List<String> parameters;
    private final Stmt body;
    private final Environment closure;

    UserFunction(String name, List<String> parameters, Stmt body, Environment closure) {
        super(name, parameters.size());
        this.parameters = parameters;
        this.body = body;
        this.closure = closure;
    }

    @Override
    Value invoke(Interpreter interpreter, Value[] args) {
        Environment local = closure.child();
        for (int i = 0; i < args.length; i++) {
            local.define(parameters.get(i), args[i]);
        }
        interpreter.returnValue = NullValue.INSTANCE;
        int completion = body.execute(local);
        Value result =
                completion == Interpreter.RETURN ? interpreter.returnValue : NullValue.INSTANCE;
        interpreter.returnValue = NullValue.INSTANCE;
        return result;
    }
}
