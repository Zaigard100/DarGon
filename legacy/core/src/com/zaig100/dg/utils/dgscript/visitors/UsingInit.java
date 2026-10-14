package com.zaig100.dg.utils.dgscript.visitors;

import com.zaig100.dg.utils.dgscript.ast.statements.UseStatement;

public class UsingInit extends AbstractVisitor {
    @Override
    public void visit(UseStatement s) {
        s.moduleInit(s.moduleName).init();
    }
}
