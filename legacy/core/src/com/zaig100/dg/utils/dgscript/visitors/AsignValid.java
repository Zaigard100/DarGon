package com.zaig100.dg.utils.dgscript.visitors;

import com.zaig100.dg.utils.dgscript.ast.statements.AssignStatement;
import com.zaig100.dg.utils.dgscript.lib.Variables;

public class AsignValid extends AbstractVisitor {
    @Override
    public void visit(AssignStatement s) {
        s.exp.accept(this);
        if (Variables.isExist(s.variable))
            throw new RuntimeException("Cannon assign value to constant");
    }
}
