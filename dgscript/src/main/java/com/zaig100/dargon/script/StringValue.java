package com.zaig100.dargon.script;

/** Неизменяемая строка. */
public final class StringValue implements Value {

    public static final StringValue EMPTY = new StringValue("");

    private final String value;

    private StringValue(String value) {
        this.value = value;
    }

    public static StringValue of(String value) {
        return value.isEmpty() ? EMPTY : new StringValue(value);
    }

    public String value() {
        return value;
    }

    @Override
    public String typeName() {
        return "string";
    }

    @Override
    public String display() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StringValue other && other.value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return '"' + value + '"';
    }
}
