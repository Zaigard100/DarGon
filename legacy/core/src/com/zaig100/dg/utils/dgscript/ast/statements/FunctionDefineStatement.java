package com.zaig100.dg.utils.dgscript.ast.statements;

import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

import java.util.List;

public class FunctionDefineStatement implements Statement {

    public final String name;
    public final List<String> argNames;
    public final Statement body;

    public FunctionDefineStatement(String name, List<String> argNames, Statement body) {
        this.name = name;
        this.argNames = argNames;
        this.body = body;
    }

    @Override
    public void execute() {
        //Functions.set(name, new UserDefFunc(argNames, body));
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public <R, T> R accept(ResultVisitor<R, T> visitor, T t) {
        return visitor.visit(this, t);
    }

    @Override
    public String toString() {
        return "def " + name + "(" + argNames.toString() + ") " + "{\n" + body.toString() + "}";
    }
}
