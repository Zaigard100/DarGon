package com.zaig100.dg.utils.dgscript.ast.statements;

import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.moduls.Module;
import com.zaig100.dg.utils.dgscript.moduls.dargon.dargon;
import com.zaig100.dg.utils.dgscript.moduls.math.math;
import com.zaig100.dg.utils.dgscript.moduls.std.std;
import com.zaig100.dg.utils.dgscript.moduls.system.system;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

public class UseStatement implements Statement {

    public final String moduleName;

    public UseStatement(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public void execute() {
        //moduleInit(moduleName).init();
    }

    public Module moduleInit(String name) {
        switch (name) {
            case "dargon":
                return new dargon();
            case "std":
                return new std();
            case "math":
                return new math();
            case "system":
                return new system();
        }
        throw new RuntimeException("No exsist module: " + name);
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
        return "use \"" + moduleName + "\"";
    }
}
