package com.zaig100.dg.utils.dgscript.lib;

import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.ast.statements.ReturnStatement;

import java.util.Arrays;
import java.util.List;

public class UserDefFunc implements Function {


    private final List<String> argNames;
    private final Statement body;

    public UserDefFunc(List<String> argNames, Statement body) {
        this.argNames = argNames;
        this.body = body;
    }

    public int argCount() {
        return argNames.size();
    }

    public String getArgName(int index) {
        if (index < 0 || index >= argCount()) return "";
        else return argNames.get(index);
    }

    @Override
    public Value execute(Value... args) {
        try {
            body.execute();
            return NumberVal.ZERO;
        } catch (ReturnStatement rt) {
            return rt.getVal();
        }
    }

    @Override
    public String toString() {
        return "(" + Arrays.toString(argNames.toArray()) + ") {" + body.toString() + "}";
    }
}
