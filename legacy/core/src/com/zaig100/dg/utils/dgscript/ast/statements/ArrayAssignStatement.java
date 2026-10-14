package com.zaig100.dg.utils.dgscript.ast.statements;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.ast.expression.ArrayAssignExpression;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

public class ArrayAssignStatement implements Statement {

    public final ArrayAssignExpression array;
    public final Expression exp;

    public ArrayAssignStatement(ArrayAssignExpression array, Expression exp) {
        this.array = array;
        this.exp = exp;
    }

    @Override
    public void execute() {
        array.getArr().set(array.lastIndex(), exp.eval());
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
        return array.toString() + " = " + exp.toString();
    }
}
