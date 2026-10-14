package com.zaig100.dg.utils.dgscript.visitors.optimizators;

import com.zaig100.dg.utils.dgscript.ast.Node;
import com.zaig100.dg.utils.dgscript.ast.expression.BinExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.ConditionalExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.UnaryExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.ValueExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.VariableExpression;
import com.zaig100.dg.utils.dgscript.lib.Types;
import com.zaig100.dg.utils.dgscript.lib.Value;

public class ExpressionSimplification extends OptimizationVisitor<Void> implements Optimizer.Info {

    int simpCount;

    public ExpressionSimplification() {
        simpCount = 0;
    }

    @Override
    public int optimizCount() {
        return simpCount;
    }

    @Override
    public String sumInfo() {
        if (simpCount == 0) return "";
        return "Simple: " + simpCount;
    }

    @Override
    public Node visit(BinExpression s, Void unused) {
        final boolean exp1IsZero = isIntrgerVal(s.ex1, 0);
        if (exp1IsZero || isIntrgerVal(s.ex2, 0)) {
            switch (s.opr) {
                case '+':
                    simpCount++;
                    return exp1IsZero ? s.ex2 : s.ex1;
                case '-':
                    simpCount++;
                    if (exp1IsZero) {
                        return new UnaryExpression('-', s.ex2);
                    }
                    return s.ex1;
                case '*':
                    simpCount++;
                    return new ValueExpression(0);
                case '/':
                    if (exp1IsZero) {
                        simpCount++;
                        return new ValueExpression(0);
                    }
                    break;
            }
        }

        final boolean exp1IsOne = isIntrgerVal(s.ex1, 1);

        if (exp1IsOne || isIntrgerVal(s.ex2, 1)) {
            switch (s.opr) {
                case '*':
                    simpCount++;
                    return exp1IsOne ? s.ex2 : s.ex1;
                case '/':
                    if (!exp1IsOne) {
                        simpCount++;
                        return s.ex1;
                    }
                    break;
            }
        }

        if (isIntrgerVal(s.ex2, -1)) {
            simpCount++;
            return new UnaryExpression('-', s.ex1);
        }
        if (isSameVariables(s.ex1, s.ex2) && s.opr == '-') {
            simpCount++;
            return new ValueExpression(0);
        }

        return super.visit(s, unused);
    }

    @Override
    public Node visit(ConditionalExpression s, Void unused) {
        if ((isIntrgerVal(s.ex1, 0) || (isIntrgerVal(s.ex2, 0)) && s.opr == ConditionalExpression.Operator.AND)) {
            simpCount++;
            return new ValueExpression(0);
        }
        if ((isIntrgerVal(s.ex1, 1) || (isIntrgerVal(s.ex2, 1)) && s.opr == ConditionalExpression.Operator.OR)) {
            simpCount++;
            return new ValueExpression(1);
        }
        return super.visit(s, unused);
    }

    private boolean isIntrgerVal(Node node, int valCheck) {
        if (!(node instanceof ValueExpression)) return false;
        final Value val = ((ValueExpression) node).value;
        if (val.type() != Types.NUM) return false;
        return val.asNum() == valCheck;
    }

    private boolean isSameVariables(Node n1, Node n2) {
        if ((n1 instanceof VariableExpression) && (n2 instanceof VariableExpression)) {
            final VariableExpression v1 = (VariableExpression) n1;
            final VariableExpression v2 = (VariableExpression) n2;
            return v1.name.equals(v2.name);
        }
        return false;
    }

}
