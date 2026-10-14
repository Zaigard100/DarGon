package com.zaig100.dg.utils.dgscript.visitors;

import com.zaig100.dg.utils.dgscript.ast.statements.FunctionDefineStatement;
import com.zaig100.dg.utils.dgscript.lib.Functions;
import com.zaig100.dg.utils.dgscript.lib.UserDefFunc;

public class FuntionAdder extends AbstractVisitor {

    @Override
    public void visit(FunctionDefineStatement s) {
        s.body.accept(this);
        Functions.set(s.name, new UserDefFunc(s.argNames, s.body));
    }

}
