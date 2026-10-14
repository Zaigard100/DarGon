package com.zaig100.dg.utils.dgscript.ast.expression;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.exeptions.OperationIsNotSupportedExeption;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

public final class ConditionalExpression implements Expression {

    public static enum Operator {
        PLUS("+"),
        MINUS("-"),
        MULTIPLY("*"),
        DIVIDE("/"),

        EQUALS("=="),
        NOT_EQUALS("!="),

        LT("<"),
        LTEQ("<="),
        GT(">"),
        GTEQ(">="),

        AND("&&"),
        OR("||");

        private final String name;

        private Operator(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public final Expression ex1, ex2;
    public final Operator opr;

    public ConditionalExpression(Operator opr, Expression ex1, Expression ex2) {
        this.ex1 = ex1;
        this.ex2 = ex2;
        this.opr = opr;
    }

    @Override
    public Value eval() {
        final Value val1 = ex1.eval();
        final Value val2 = ex2.eval();

        double num1, num2;
        if (val1 instanceof StringVal) {
            num1 = val1.asString().compareTo(val2.asString());
            num2 = 0;
        } else {
            num1 = val1.asNum();
            num2 = val2.asNum();
        }
        boolean resault;
        switch (opr) {

            case LT:
                resault = num1 < num2;
                break;
            case LTEQ:
                resault = num1 <= num2;
                break;
            case GT:
                resault = num1 > num2;
                break;
            case GTEQ:
                resault = num1 >= num2;
                break;
            case NOT_EQUALS:
                resault = num1 != num2;
                break;

            case AND:
                resault = (num1 != 0) && (num2 != 0);
                break;
            case OR:
                resault = (num1 != 0) || (num2 != 0);
                break;
            case EQUALS:
                resault = num1 == num2;
                break;
            default:
                throw new OperationIsNotSupportedExeption(opr);
        }
        return NumberVal.fromBoolean(resault);
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
        return String.format("(%s %s %s)", ex1, opr.getName(), ex2);
    }
}
