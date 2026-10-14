package com.zaig100.dg.utils.dgscript.ast.expression;

import com.zaig100.dg.utils.contain.ObjC;
import com.zaig100.dg.utils.dgscript.ast.Expression;
import com.zaig100.dg.utils.dgscript.lib.DGObjectValue;
import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.FunctionVal;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;
import com.zaig100.dg.world.objects.Obj;

public final class ValueExpression implements Expression {

    public final Value value;

    public ValueExpression(double value) {
        this.value = new NumberVal(value);
    }

    public ValueExpression(String value) {
        this.value = new StringVal(value);
    }

    public ValueExpression(Function value) {
        this.value = new FunctionVal(value);
    }

    public ValueExpression(Obj value, Obj.ObjType type) {
        this.value = new DGObjectValue(value,type);
    }

    public ValueExpression(Value value) {
        this.value = value;
    }

    @Override
    public Value eval() {
        return value;
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
        return value.asString();
    }
}
