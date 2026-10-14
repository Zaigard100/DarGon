package com.zaig100.dargon.ecs;

import java.util.Arrays;

/** Растущий список {@code int} без упаковки в {@code Integer}. */
public final class IntList {

    private int[] items;
    private int size;

    public IntList() {
        this(16);
    }

    public IntList(int capacity) {
        items = new int[Math.max(capacity, 4)];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int get(int i) {
        if (i >= size) {
            throw new IndexOutOfBoundsException(i + " >= " + size);
        }
        return items[i];
    }

    public void add(int value) {
        if (size == items.length) {
            items = Arrays.copyOf(items, items.length * 2);
        }
        items[size++] = value;
    }

    /** Удаляет и возвращает последний элемент. */
    public int pop() {
        if (size == 0) {
            throw new IllegalStateException("Список пуст");
        }
        return items[--size];
    }

    public void clear() {
        size = 0;
    }
}
