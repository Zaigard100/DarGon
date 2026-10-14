package com.zaig100.dg.utils.dgscript.lib;

public class StringVal implements Value {
    private final String val;

    public StringVal(String val) {
        this.val = val;
    }

    @Override
    public Object raw() {
        return val;
    }

    @Override
    public int asInt() {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public double asNum() {
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String asString() {
        return val;
    }

    @Override
    public int type() {
        return Types.STR;
    }

    @Override
    public String toString() {
        return asString();
    }
}
