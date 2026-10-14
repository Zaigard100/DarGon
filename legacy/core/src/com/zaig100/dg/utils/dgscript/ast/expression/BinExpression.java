package com.zaig100.dg.utils.dgscript.ast.expression;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.exeptions.OperationIsNotSupportedExeption;
import com.zaig100.dg.utils.dgscript.lib.ArrayValue;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

public final class BinExpression implements Expression {
    public final Expression ex1, ex2;
    public final char opr;

    public BinExpression(char opr, Expression ex1, Expression ex2) {
        this.ex1 = ex1;
        this.ex2 = ex2;
        this.opr = opr;
    }

    @Override
    public Value eval() {
        final Value val1 = ex1.eval();
        final Value val2 = ex2.eval();
        if (val1 instanceof StringVal || val2 instanceof StringVal || val1 instanceof ArrayValue || val2 instanceof ArrayValue) {
            final String str1 = val1.asString();
            final String str2 = val2.asString();
            switch (opr) {
                default:
                case '+':
                    return new StringVal(str1 + str2);
                case '*': {
                    final int itrs = (int) val2.asNum();
                    final StringBuilder buffer = new StringBuilder();
                    for (int i = 0; i < itrs; i++) {
                        buffer.append(str1);
                    }
                    return new StringVal(buffer.toString());
                }
            }
        } else if (val1 instanceof NumberVal && val2 instanceof NumberVal) {
            final double num1 = val1.asNum();
            final double num2 = val2.asNum();
            switch (opr) {
                case '-':
                    return new NumberVal(num1 - num2);
                case '*':
                    return new NumberVal(num1 * num2);
                case '/':
                    return new NumberVal(num1 / num2);
                default:
                case '+':
                    return new NumberVal(num1 + num2);
            }
        }
        throw new OperationIsNotSupportedExeption(opr);
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
        return String.format("(%s %c %s)", ex1, opr, ex2);
    }
}
