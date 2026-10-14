package com.zaig100.dg.utils.dgscript.lib;

public class NumberVal implements Value {

    public static final NumberVal MINUS_ONE, ZERO, ONE;

    private static final int CACHE_MIN = -128, CACHE_MAX = 127;
    private static final NumberVal[] NUMBER_CACHE;

    static {
        final int length = CACHE_MAX - CACHE_MIN + 1;
        NUMBER_CACHE = new NumberVal[length];
        int value = CACHE_MIN;
        for (int i = 0; i < length; i++) {
            NUMBER_CACHE[i] = new NumberVal(value++);
        }

        final int zeroIndex = -CACHE_MIN;
        MINUS_ONE = NUMBER_CACHE[zeroIndex - 1];
        ZERO = NUMBER_CACHE[zeroIndex];
        ONE = NUMBER_CACHE[zeroIndex + 1];
    }

    public static NumberVal fromBoolean(boolean b) {
        return b ? ONE : ZERO;
    }

    public static NumberVal of(int value) {
        if (CACHE_MIN <= value && value <= CACHE_MAX) {
            return NUMBER_CACHE[-CACHE_MIN + value];
        }
        return new NumberVal(value);
    }

    public static NumberVal of(Number value) {
        return new NumberVal(value);
    }

    private final Number val;

    public NumberVal(Number val) {
        this.val = val;
    }

    @Override
    public Number raw() {
        return val;
    }

    @Override
    public int asInt() {
        return val.intValue();
    }

    @Override
    public double asNum() {
        return val.doubleValue();
    }

    @Override
    public String asString() {
        return val.toString();
    }

    @Override
    public int type() {
        return Types.NUM;
    }
}
