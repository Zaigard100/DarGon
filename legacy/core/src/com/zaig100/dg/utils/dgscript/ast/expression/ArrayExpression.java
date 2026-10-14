package com.zaig100.dg.utils.dgscript.ast.expression;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.lib.ArrayValue;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

import java.util.List;


public class ArrayExpression implements Expression {

    public final List<Expression> elements;

    public ArrayExpression(List elements) {
        this.elements = elements;
    }

    @Override
    public Value eval() {
        final int size = elements.size();
        final ArrayValue array = new ArrayValue(size);
        for (int i = 0; i < size; i++) {
            array.set(i, elements.get(i).eval());
        }
        return array;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public <R, T> R accept(ResultVisitor<R, T> visitor, T t) {
        return visitor.visit(this, t);
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        for (int i = 0; i < elements.size(); i++) {
            buffer.append(elements.get(i).toString());
            if (!(i < elements.size() + 1)) buffer.append(",");
        }
        buffer.append("]");
        return buffer.toString();
    }
}
