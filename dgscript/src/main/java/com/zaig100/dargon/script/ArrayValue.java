package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.List;

/** Изменяемый массив. Сравнивается по ссылке. */
public final class ArrayValue implements Value {

    private final List<Value> items;

    public ArrayValue() {
        this.items = new ArrayList<>();
    }

    public ArrayValue(List<Value> items) {
        this.items = new ArrayList<>(items);
    }

    /** Живой список элементов (изменения видны скрипту). */
    public List<Value> items() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public Value get(int index) {
        return items.get(index);
    }

    public void add(Value value) {
        items.add(value);
    }

    @Override
    public String typeName() {
        return "array";
    }

    @Override
    public String display() {
        return Values.displayNested(this, 0);
    }

    @Override
    public String toString() {
        return display();
    }
}
