package com.zaig100.dg.utils.dgscript.ast.statements;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

public class WhileStatement implements Statement {
    public final Expression exp;
    public final Statement statement;

    public WhileStatement(Expression exp, Statement statement) {
        this.exp = exp;
        this.statement = statement;
    }

    @Override
    public void execute() {
        while (exp.eval().asNum() != 0) {
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
        return "while (" + exp + " ) {\n" + statement + "}";
    }
}
