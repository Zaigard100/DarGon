package com.zaig100.dargon.script;

/** {@code null}. */
public final class NullValue implements Value {

    public static final NullValue INSTANCE = new NullValue();

    private NullValue() {}

    @Override
    public String typeName() {
        return "null";
    }

    @Override
    public boolean isTruthy() {
        return false;
    }

    @Override
    public String display() {
        return "null";
    }

    @Override
    public String toString() {
        return "null";
    }
}
