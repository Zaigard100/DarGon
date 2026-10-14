package com.zaig100.dg.utils.dgscript.visitors.optimizators;

import com.zaig100.dg.utils.dgscript.ast.Node;
import com.zaig100.dg.utils.dgscript.ast.expression.BinExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.ConditionalExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.UnaryExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.ValueExpression;
import com.zaig100.dg.utils.dgscript.exeptions.OperationIsNotSupportedExeption;

public class ConstantFolding extends OptimizationVisitor<Void> implements Optimizer.Info {

    private int binExpCount, condExpCount, unExpCount;

    public ConstantFolding() {
        binExpCount = 0;
        condExpCount = 0;
        unExpCount = 0;
    }

    @Override
    public int optimizCount() {
        return binExpCount + condExpCount + unExpCount;
    }

    @Override
    public String sumInfo() {
        if (optimizCount() == 0) return "";
        final StringBuilder sb = new StringBuilder();
        if (binExpCount > 0) {
            sb.append("\nBinExp: ").append(binExpCount);
        }
        if (condExpCount > 0) {
            sb.append("\nCondExp: ").append(condExpCount);
        }
        if (unExpCount > 0) {
            sb.append("\nUnExp: ").append(unExpCount);
        }
        return sb.toString();
    }

    @Override
    public Node visit(BinExpression s, Void unused) {
        if ((s.ex1 instanceof ValueExpression) && (s.ex2 instanceof ValueExpression)) {
            binExpCount++;
            try {
                return new ValueExpression(s.eval());
            } catch (OperationIsNotSupportedExeption op) {
                binExpCount--;
            }
        }
        return super.visit(s, unused);
    }

    @Override
    public Node visit(ConditionalExpression s, Void unused) {
        if ((s.ex1 instanceof ValueExpression) && (s.ex2 instanceof ValueExpression)) {
            condExpCount++;
            try {
                return new ValueExpression(s.eval());
            } catch (OperationIsNotSupportedExeption op) {
                condExpCount--;
            }
        }
        return super.visit(s, unused);
    }

    @Override
    public Node visit(UnaryExpression s, Void unused) {
        if ((s.ex instanceof ValueExpression)) {
            unExpCount++;
            try {
                return new ValueExpression(s.eval());
            } catch (OperationIsNotSupportedExeption op) {
                unExpCount--;
            }
        }
        return super.visit(s, unused);
    }
}
