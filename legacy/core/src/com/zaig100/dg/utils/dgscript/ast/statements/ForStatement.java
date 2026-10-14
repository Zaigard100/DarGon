package com.zaig100.dg.utils.dgscript.ast.statements;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

public class ForStatement implements Statement {

    public final Statement init;
    public final Expression exp;
    public final Statement increment;
    public final Statement statement;

    public ForStatement(Statement init, Expression exp, Statement increment, Statement statement) {
        this.init = init;
        this.exp = exp;
        this.increment = increment;
        this.statement = statement;
    }

    @Override
    public void execute() {
        for (init.execute(); exp.eval().asNum() != 0; increment.execute()) {
            try {
                statement.execute();
            } catch (BreakStatement bs) {
                break;
            } catch (ContinueStatement cs) {
                //continue;
            }
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
        return "for (" + init + "," + exp + "," + increment + ") {\n" + statement + "}\n";
    }
}
