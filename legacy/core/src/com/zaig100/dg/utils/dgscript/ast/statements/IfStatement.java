package com.zaig100.dg.utils.dgscript.ast.statements;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

public class IfStatement implements Statement {

    public final Expression exp;
    public final Statement ifSt, elseSt;

    public IfStatement(Expression exp, Statement ifSt, Statement elseSt) {
        this.exp = exp;
        this.ifSt = ifSt;
        this.elseSt = elseSt;
    }

    @Override
    public void execute() {
        final double result = exp.eval().asNum();
        if (result != 0) {
            ifSt.execute();
        } else if (elseSt != null) {
            elseSt.execute();
        }

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
        final StringBuilder result = new StringBuilder();
        result.append("if ").append(exp).append(" {\n").append(ifSt).append("}");
        if (elseSt != null) {
            result.append("\nelse {\n").append(elseSt).append("}\n");
        }
        return result.toString();
    }
}
