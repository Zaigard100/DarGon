package com.zaig100.dg.utils.dgscript.visitors.optimizators;

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

public interface ResultVisitor<R, T> {

    R visit(ArrayAssignExpression s, T t);

    R visit(ArrayExpression s, T t);

    R visit(BinExpression s, T t);

    R visit(ConditionalExpression s, T t);

    R visit(FunctionalExpression s, T t);

    R visit(ValueExpression s, T t);

    R visit(UnaryExpression s, T t);

    R visit(VariableExpression s, T t);

    R visit(ArrayAssignStatement s, T t);

    R visit(AssignStatement s, T t);

    R visit(BlockStatement s, T t);

    R visit(BreakStatement s, T t);

    R visit(ContinueStatement s, T t);

    R visit(DoWhileStatement s, T t);

    R visit(ForStatement s, T t);

    R visit(FunctionDefineStatement s, T t);

    R visit(FunctionStatement s, T t);

    R visit(IfStatement s, T t);

    R visit(ReturnStatement s, T t);

    R visit(UseStatement s, T t);

    R visit(WhileStatement s, T t);

    R visit(ExprStatement s, T t);
}
