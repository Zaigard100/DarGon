package com.zaig100.dg.utils.dgscript.visitors.optimizators;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.ast.Node;
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

import java.util.ArrayList;
import java.util.List;

public abstract class OptimizationVisitor<T> implements ResultVisitor<Node, T> {
    @Override
    public Node visit(ArrayAssignExpression s, T t) {
        boolean charged = false;
        final List<Expression> inds = new ArrayList<>(s.indeces.size());
        for (Expression exp : s.indeces) {
            final Node node = exp.accept(this, t);
            if (node != exp) {
                charged = true;
            }
            inds.add((Expression) node);
        }
        if (charged) {
            return new ArrayAssignExpression(s.arrName, inds);
        }
        return s;
    }

    @Override
    public Node visit(ArrayExpression s, T t) {
        final List<Expression> elements = new ArrayList<>(s.elements.size());
        boolean changed = false;
        for (Expression exp : s.elements) {
            final Node node = exp.accept(this, t);
            if (node != exp) {
                changed = true;
            }
            elements.add((Expression) node);
        }
        if (changed) {
            return new ArrayExpression(elements);
        }
        return s;
    }

    @Override
    public Node visit(BinExpression s, T t) {
        final Node exp1 = s.ex1.accept(this, t);
        final Node exp2 = s.ex2.accept(this, t);
        if (exp1 != s.ex1 || exp2 != s.ex2) {
            return new BinExpression(s.opr, (Expression) exp1, (Expression) exp2);
        }
        return s;
    }

    @Override
    public Node visit(ConditionalExpression s, T t) {
        final Node exp1 = s.ex1.accept(this, t);
        final Node exp2 = s.ex2.accept(this, t);
        if (exp1 != s.ex1 || exp2 != s.ex2) {
            return new ConditionalExpression(s.opr, (Expression) exp1, (Expression) exp2);
        }
        return s;
    }

    @Override
    public Node visit(FunctionalExpression s, T t) {
        final List<Expression> elements = new ArrayList<>(s.args.size());
        boolean changed = false;
        for (Expression exp : s.args) {
            final Node node = exp.accept(this, t);
            if (node != exp) {
                changed = true;
            }
            elements.add((Expression) node);
        }
        if (changed) {
            return new ArrayExpression(elements);
        }
        return s;
    }


    @Override
    public Node visit(ValueExpression s, T t) {
        return s;
    }

    @Override
    public Node visit(UnaryExpression s, T t) {
        final Node node = s.ex.accept(this, t);
        if (node != s.ex) {
            return new UnaryExpression(s.opr, (Expression) node);
        }
        return s;
    }

    @Override
    public Node visit(VariableExpression s, T t) {
        return s;
    }

    @Override
    public Node visit(ArrayAssignStatement s, T t) {
        final Node exp = s.exp.accept(this, t);
        final Node arrExp = s.array.accept(this, t);
        if (exp != s.exp || arrExp != s.array) {
            return new ArrayAssignStatement((ArrayAssignExpression) arrExp, (Expression) exp);
        }
        return s;
    }

    @Override
    public Node visit(AssignStatement s, T t) {
        final Node node = s.exp.accept(this, t);
        if (node != s.exp) {
            return new AssignStatement(s.variable, (Expression) node);
        }
        return s;
    }

    @Override
    public Node visit(BlockStatement s, T t) {
        boolean changed = false;
        final BlockStatement res = new BlockStatement();
        for (Statement stat : s.statemens) {
            final Node node = stat.accept(this, t);
            if (node != stat) {
                changed = true;
            }
            res.add((Statement) node);
        }
        if (changed) {
            return res;
        }
        return s;
    }

    @Override
    public Node visit(BreakStatement s, T t) {
        return s;
    }

    @Override
    public Node visit(ContinueStatement s, T t) {
        return s;
    }

    @Override
    public Node visit(DoWhileStatement s, T t) {
        final Node cond = s.exp.accept(this, t);
        final Node stat = s.statement.accept(this, t);
        if (cond != s.exp || stat != s.statement) {
            return new DoWhileStatement((Expression) cond, (Statement) stat);
        }
        return s;
    }

    @Override
    public Node visit(ForStatement s, T t) {
        final Node init = s.init.accept(this, t);
        final Node exp = s.exp.accept(this, t);
        final Node increment = s.increment.accept(this, t);
        final Node statement = s.statement.accept(this, t);
        if (init != s.init || exp != s.exp || increment != s.increment || statement != s.statement) {
            return new ForStatement((Statement) init, (Expression) exp, (Statement) increment, (Statement) statement);
        }
        return s;
    }

    @Override
    public Node visit(FunctionDefineStatement s, T t) {
        final Node node = s.body.accept(this, t);
        if (node != s.body) {
            return new FunctionDefineStatement(s.name, s.argNames, (Statement) node);
        }
        return s;
    }

    @Override
    public Node visit(FunctionStatement s, T t) {
        final Node node = s.func.accept(this, t);
        if (node != s.func) {
            return new FunctionStatement((Expression) node);
        }
        return s;
    }

    @Override
    public Node visit(IfStatement s, T t) {
        final Node ifSt = s.ifSt.accept(this, t);
        final Node exp = s.exp.accept(this, t);
        if (s.elseSt != null) {
            final Node elseSt = s.elseSt.accept(this, t);
            if (ifSt != s.ifSt || exp != s.exp || elseSt != s.elseSt) {
                return new IfStatement((Expression) exp, (Statement) ifSt, (Statement) elseSt);
            }
        } else {
            if (ifSt != s.ifSt || exp != s.exp) {
                return new IfStatement((Expression) exp, (Statement) ifSt, null);
            }
        }
        return s;
    }

    @Override
    public Node visit(ReturnStatement s, T t) {
        final Node node = s.exp.accept(this, t);
        if (node != s.exp) {
            return new ReturnStatement((Expression) node);
        }
        return s;
    }

    @Override
    public Node visit(UseStatement s, T t) {
        return s;
    }

    @Override
    public Node visit(WhileStatement s, T t) {
        final Node cond = s.exp.accept(this, t);
        final Node stat = s.statement.accept(this, t);
        if (cond != s.exp || stat != s.statement) {
            return new WhileStatement((Expression) cond, (Statement) stat);
        }
        return s;
    }

    @Override
    public Node visit(ExprStatement s, T t) {
        final Node node = s.exp.accept(this, t);
        if (node != s.exp) {
            return new ExprStatement((Expression) node);
        }
        return s;
    }
}
