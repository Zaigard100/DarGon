package com.zaig100.dargon.script;

/** {@code true} / {@code false}. */
public final class BoolValue implements Value {

    public static final BoolValue TRUE = new BoolValue(true);
    public static final BoolValue FALSE = new BoolValue(false);

    private final boolean value;

    private BoolValue(boolean value) {
        this.value = value;
    }

    public static BoolValue of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean value() {
        return value;
    }

    @Override
    public String typeName() {
        return "bool";
    }

    @Override
    public boolean isTruthy() {
        return value;
    }

    @Override
    public String display() {
        return Boolean.toString(value);
    }

    @Override
    public String toString() {
        return display();
    }
}
