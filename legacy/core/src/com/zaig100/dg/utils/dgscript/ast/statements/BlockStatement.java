package com.zaig100.dg.utils.dgscript.ast.statements;

import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

import java.util.ArrayList;
import java.util.List;

public class BlockStatement implements Statement {

    public List<Statement> statemens;

    public BlockStatement() {
        this.statemens = new ArrayList<>();
    }

    public void add(Statement statement) {
        statemens.add(statement);
    }

    @Override
    public void execute() {
        for (Statement statement : statemens) {
            statement.execute();
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public <R, T> R accept(ResultVisitor<R, T> visitor, T t) {
        return visitor.visit(this, t);
    }

    @SuppressWarnings("NewApi")
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        for (Statement statement : statemens) {
            buffer.append(statement.toString()).append(System.lineSeparator());
        }
        return buffer.toString();
    }
}
