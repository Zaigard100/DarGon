package com.zaig100.dg.utils.dgscript.visitors;

import com.zaig100.dg.utils.dgscript.ast.expression.ArrayAssignExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.ArrayExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.BinExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.ConditionalExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.FunctionalExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.UnaryExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.ValueExpression;
import com.zaig100.dg.utils.dgscript.ast.expression.VariableExpression;
import com.zaig100.dg.utils.dgscript.ast.statements.ArrayAssignStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.AssignStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.BlockStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.BreakStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.ContinueStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.DoWhileStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.ExprStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.ForStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.FunctionDefineStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.FunctionStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.IfStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.ReturnStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.UseStatement;
import com.zaig100.dg.utils.dgscript.ast.statements.WhileStatement;

public interface Visitor {

    void visit(ArrayAssignExpression s);

    void visit(ArrayExpression s);

    void visit(BinExpression s);

    void visit(ConditionalExpression s);

    void visit(FunctionalExpression s);

    void visit(ValueExpression s);

    void visit(UnaryExpression s);

    void visit(VariableExpression s);

    void visit(ArrayAssignStatement s);

    void visit(AssignStatement s);

    void visit(BlockStatement blS);

    void visit(BreakStatement s);

    void visit(ContinueStatement s);

    void visit(DoWhileStatement s);

    void visit(ForStatement s);

    void visit(FunctionDefineStatement s);

    void visit(FunctionStatement s);

    void visit(IfStatement s);

    void visit(ReturnStatement s);

    void visit(WhileStatement s);

    void visit(UseStatement s);

    void visit(ExprStatement s);
}
