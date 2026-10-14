package com.zaig100.dg.utils.dgscript.visitors;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.ast.Statement;
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

public abstract class AbstractVisitor implements Visitor {
    @Override
    public void visit(ArrayAssignExpression s) {
        for (Expression exp : s.indeces) {
            exp.accept(this);
        }
    }

    @Override
    public void visit(ArrayExpression s) {
        for (Expression exp : s.elements) {
            exp.accept(this);
        }
    }

    @Override
    public void visit(BinExpression s) {
        s.ex1.accept(this);
        s.ex2.accept(this);
    }

    @Override
    public void visit(ConditionalExpression s) {
        s.ex1.accept(this);
        s.ex2.accept(this);
    }

    @Override
    public void visit(FunctionalExpression s) {
        for (Expression exp : s.args) {
            exp.accept(this);
        }
    }

    @Override
    public void visit(ValueExpression s) {

    }



    @Override
    public void visit(UnaryExpression s) {
        s.ex.accept(this);
    }

    @Override
    public void visit(VariableExpression s) {

    }

    @Override
    public void visit(ArrayAssignStatement s) {
        s.array.accept(this);
        s.exp.accept(this);
    }

    @Override
    public void visit(AssignStatement s) {
        s.exp.accept(this);
    }

    @Override
    public void visit(BlockStatement s) {
        for (Statement statement : s.statemens) {
            statement.accept(this);
        }
    }

    @Override
    public void visit(BreakStatement s) {

    }

    @Override
    public void visit(ContinueStatement s) {

    }

    @Override
    public void visit(DoWhileStatement s) {
        s.exp.accept(this);
        s.statement.accept(this);
    }

    @Override
    public void visit(ForStatement s) {
        s.statement.accept(this);
        s.increment.accept(this);
        s.exp.accept(this);
        s.init.accept(this);
    }


    @Override
    public void visit(FunctionDefineStatement s) {
        s.body.accept(this);
    }

    @Override
    public void visit(FunctionStatement s) {
        s.func.accept(this);
    }

    @Override
    public void visit(IfStatement s) {
        if (s.elseSt != null) {
            s.elseSt.accept(this);
        }
        s.exp.accept(this);
        s.ifSt.accept(this);
    }

    @Override
    public void visit(ReturnStatement s) {
        s.exp.accept(this);
    }

    @Override
    public void visit(WhileStatement s) {
        s.exp.accept(this);
        s.statement.accept(this);

    }

    @Override
    public void visit(UseStatement s) {

    }

    @Override
    public void visit(ExprStatement s) {
        s.exp.accept(this);
    }
}
