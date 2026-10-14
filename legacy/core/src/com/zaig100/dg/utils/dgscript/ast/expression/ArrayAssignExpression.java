package com.zaig100.dg.utils.dgscript.ast.expression;

import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.lib.ArrayValue;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.utils.dgscript.lib.Variables;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

import java.util.List;

public class ArrayAssignExpression implements Expression {

    public final String arrName;
    public final List<Expression> indeces;

    public ArrayAssignExpression(String arrName, List<Expression> indeces) {
        this.arrName = arrName;
        this.indeces = indeces;
    }

    @Override
    public Value eval() {
        return getArr().get(lastIndex());
    }

    public ArrayValue getArr() {
        ArrayValue arr = consumeArr(Variables.get(arrName));
        final int last = indeces.size() - 1;
        for (int i = 0; i < last; i++) {
            arr = consumeArr(arr.get(index(i)));
        }
        return arr;
    }

    public int lastIndex() {
        return index(indeces.size() - 1);
    }

    private int index(int index) {
        return (int) indeces.get(index).eval().asNum();
    }

    private ArrayValue consumeArr(Value var) {
        if (Variables.get(arrName) instanceof ArrayValue) return (ArrayValue) var;
        else throw new RuntimeException(arrName + "array doesn't exist");
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
        buffer.append(arrName);
        for (int i = 0; i < indeces.size(); i++) {
            buffer.append("[").append(indeces.get(i).toString()).append("]");
        }
        return buffer.toString();
    }
}
