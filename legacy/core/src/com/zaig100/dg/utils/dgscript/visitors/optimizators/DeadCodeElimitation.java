package com.zaig100.dg.utils.dgscript.visitors.optimizators;

import com.zaig100.dg.utils.dgscript.ast.Node;
import com.zaig100.dg.utils.dgscript.ast.expression.ValueExpression;
import com.zaig100.dg.utils.dgscript.ast.statements.ExprStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.IfStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.WhileStatement;

public class DeadCodeElimitation extends OptimizationVisitor<Void> implements Optimizer.Info {

    private int ifStatCount, whileStatCount;

    public DeadCodeElimitation() {
        ifStatCount = 0;
        whileStatCount = 0;
    }

    @Override
    public int optimizCount() {
        return ifStatCount + whileStatCount;
    }

    @Override
    public String sumInfo() {
        if (optimizCount() == 0) return "";
        final StringBuilder sb = new StringBuilder();
        if (ifStatCount > 0) {
            sb.append("\nIf: ").append(ifStatCount);
        }
        if (whileStatCount > 0) {
            sb.append("\nWhile: ").append(whileStatCount);
        }
        return sb.toString();
    }

    @Override
    public Node visit(IfStatement s, Void unused) {
        if (s.exp instanceof ValueExpression) {
            ifStatCount++;
            if (s.exp.eval().asNum() != 0) {
                return s.ifSt;
            }
            if (s.elseSt != null) {
                return s.elseSt;
            }
            return new ExprStatement(s.exp);
        }
        return super.visit(s, unused);
    }

    @Override
    public Node visit(WhileStatement s, Void unused) {
        if (s.exp instanceof ValueExpression) {
            if (s.exp.eval().asNum() == 0) {
                whileStatCount++;
                return new ExprStatement(s.exp);
            }
        }
        return super.visit(s, unused);
    }
}
