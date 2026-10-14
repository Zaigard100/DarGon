package com.zaig100.dg.utils.dgscript.ast.expression;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.exeptions.OperationIsNotSupportedExeption;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

public class UnaryExpression implements Expression {

    public final Expression ex;
    public final char opr;

    public UnaryExpression(char opr, Expression ex) {
        this.ex = ex;
        this.opr = opr;
    }

    @Override
    public Value eval() {
        switch (opr) {
            case '-':
                return new NumberVal(-ex.eval().asNum());
            case '*':
                return new NumberVal(ex.eval().asNum() * ex.eval().asNum());
            case '/':
                return new NumberVal(Math.sqrt(ex.eval().asNum()));
            case '+':
                return ex.eval();
            default:
                throw new OperationIsNotSupportedExeption(opr);
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
        return opr + "(" + ex.toString() + ")";
    }
}
