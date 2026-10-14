package com.zaig100.dargon.script;

/** Число (double). Целые значения печатаются без дробной части. */
public final class NumberValue implements Value {

    private static final int CACHE_MIN = -128;
    private static final int CACHE_MAX = 1023;
    private static final NumberValue[] CACHE = new NumberValue[CACHE_MAX - CACHE_MIN + 1];

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new NumberValue(i + CACHE_MIN);
        }
    }

    public static final NumberValue ZERO = of(0);
    public static final NumberValue ONE = of(1);

    private final double value;

    private NumberValue(double value) {
        this.value = value;
    }

    public static NumberValue of(double value) {
        if (value >= CACHE_MIN && value <= CACHE_MAX && value == (int) value) {
            return CACHE[(int) value - CACHE_MIN];
        }
        return new NumberValue(value);
    }

    public double value() {
        return value;
    }

    /** Целая часть (отбрасывание дроби). */
    public int intValue() {
        return (int) value;
    }

    /** Является ли число целым. */
    public boolean isInteger() {
        return value == Math.rint(value) && !Double.isInfinite(value);
    }

    @Override
    public String typeName() {
        return "number";
    }

    @Override
    public boolean isTruthy() {
        return value != 0;
    }

    @Override
    public String display() {
        if (isInteger() && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NumberValue other && other.value == value;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public String toString() {
        return display();
    }
}
