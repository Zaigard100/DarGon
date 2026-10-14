package com.zaig100.dg.utils.dgscript.visitors;

import com.zaig100.dg.utils.dgscript.ast.expression.VariableExpression;
import com.zaig100.dg.utils.dgscript.ast.statements.AssignStatement;

public class ValPrinter extends AbstractVisitor {

    @Override
    public void visit(VariableExpression s) {
        System.out.println(s.name);
    }

    @Override
    public void visit(AssignStatement s) {
        s.exp.accept(this);
        System.out.println(s.variable);
    }

}
