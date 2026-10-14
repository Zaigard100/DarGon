package com.zaig100.dg.utils.dgscript.lib;

public class FunctionVal implements Value {

    private final Function val;

    public FunctionVal(Function val) {
        this.val = val;
    }

    @Override
    public Object raw() {
        return val;
    }

    @Override
    public int asInt() {
        throw new RuntimeException("Cannot cast function to number");
    }

    @Override
    public double asNum() {
        throw new RuntimeException("Cannot cast function to number");
    }

    @Override
    public String asString() {
        return val.toString();
    }

    @Override
    public int type() {
        return Types.FUNC;
    }

    public Function getVal() {
        return val;
    }

    @Override
    public String toString() {
        return asString();
    }
}
