package com.zaig100.dg.utils.dgscript.lib;

public class ArrayValue implements Value {

    private final Value[] elements;

    public ArrayValue(int size) {
        this.elements = new Value[size];
        //for(int i = 0;i < elements.length; i++){ elements[i] = NumberVal.ZERO; }
    }

    public ArrayValue(Value[] elements) {
        this.elements = new Value[elements.length];
        System.arraycopy(elements, 0, this.elements, 0, elements.length);
    }

    public ArrayValue(ArrayValue arrval) {
        this(arrval.elements);
    }

    public Value get(int index) {
        return elements[index];
    }

    public void set(int index, Value val) {
        if (index >= elements.length)
            throw new RuntimeException("Index goes beyond the size of the array");
        elements[index] = val;
    }

    public int getLastIndex() {
        return elements.length - 1;
    }

    public Value getLastVal() {
        return get(getLastIndex());
    }


    @Override
    public Object raw() {
        return elements;
    }

    @Override
    public int asInt() {
        return 0;
    }

    @Override
    public double asNum() {
        throw new RuntimeException("Cannot cast array to number");
    }

    @Override
    public String asString() {
        final StringBuilder buff = new StringBuilder();
        buff.append('[');
        for (int i = 0; i < elements.length; i++) {
            buff.append(elements[i].asString());
            if (i != elements.length - 1) buff.append(", ");
        }
        buff.append(']');
        return buff.toString();
    }

    @Override
    public int type() {
        return Types.ARR;
    }


    @Override
    public String toString() {
        return asString();
    }
}
