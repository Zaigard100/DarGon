package com.zaig100.dg.utils.dgscript.ast.expression;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.FunctionVal;
import com.zaig100.dg.utils.dgscript.lib.Functions;
import com.zaig100.dg.utils.dgscript.lib.UserDefFunc;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.utils.dgscript.lib.Variables;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

import java.util.ArrayList;
import java.util.List;

public class FunctionalExpression implements Expression {

    public final String name;
    public final List<Expression> args;

    public FunctionalExpression(String name) {
        this.name = name;
        args = new ArrayList<>();
    }

    public FunctionalExpression(String name, List<Expression> args) {
        this.name = name;
        this.args = args;
    }

    public void addArg(Expression exp) {
        args.add(exp);
    }

    @Override
    public Value eval() {
        final int size = args.size();
        Value[] vals = new Value[size];
        for (int i = 0; i < size; i++) {
            vals[i] = args.get(i).eval();
        }
        final Function function = getFunc(name);
        if (function instanceof UserDefFunc) {
            final UserDefFunc userFunc = (UserDefFunc) function;
            if (size != userFunc.argCount()) throw new RuntimeException("Args count mismatch");
            Variables.push();
            for (int i = 0; i < size; i++) {
                Variables.set(userFunc.getArgName(i), vals[i]);
            }
            final Value result = userFunc.execute(vals);
            Variables.pop();
            return result;
        }
        return function.execute(vals);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public <R, T> R accept(ResultVisitor<R, T> visitor, T t) {
        return visitor.visit(this, t);
    }

    private Function getFunc(String key) {
        if (Functions.isExist(key)) return Functions.get(key);
        if (Variables.isExist(key)) {
            final Value val = Variables.get(key);
            if (val instanceof FunctionVal) return ((FunctionVal) val).getVal();
        }
        throw new RuntimeException("Unknown function: " + key);
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(name).append("(");
        for (int i = 0; i < args.size(); i++) {
            buffer.append(args.get(i).toString());
            if (i < args.size() - 1) buffer.append(", ");
        }
        buffer.append(")");
        return buffer.toString();
    }
}
