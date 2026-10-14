package com.zaig100.dargon.ecs;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Хранилище компонентов одного типа — sparse set.
 *
 * <p>Компоненты лежат плотным массивом (быстрый обход), {@code sparse[индекс сущности]} хранит
 * позицию в плотном массиве или {@code -1}. Удаление — перестановкой последнего элемента на место
 * удалённого. Удалённые компоненты сбрасываются и возвращаются в пул.
 */
final class ComponentStore<T extends Component> {

    private static final int ABSENT = -1;

    final ComponentType<T> type;

    private int[] sparse = new int[0];
    private int[] owners = new int[16];
    private Object[] dense = new Object[16];
    private int size;
    private final ArrayDeque<T> pool = new ArrayDeque<>();

    ComponentStore(ComponentType<T> type) {
        this.type = type;
    }

    int size() {
        return size;
    }

    boolean contains(int index) {
        return index < sparse.length && sparse[index] != ABSENT;
    }

    @SuppressWarnings("unchecked")
    T get(int index) {
        if (index >= sparse.length) {
            return null;
        }
        int position = sparse[index];
        return position == ABSENT ? null : (T) dense[position];
    }

    /** Добавляет компонент сущности (из пула или новый). Если он уже есть — возвращает его. */
    T add(int index) {
        T existing = get(index);
        if (existing != null) {
            return existing;
        }
        ensureSparse(index);
        if (size == dense.length) {
            dense = Arrays.copyOf(dense, size * 2);
            owners = Arrays.copyOf(owners, size * 2);
        }
        T component = pool.isEmpty() ? type.create() : pool.pop();
        dense[size] = component;
        owners[size] = index;
        sparse[index] = size;
        size++;
        return component;
    }

    /** Удаляет компонент сущности, сбрасывает его и возвращает в пул. */
    @SuppressWarnings("unchecked")
    void remove(int index) {
        if (!contains(index)) {
            return;
        }
        int position = sparse[index];
        T component = (T) dense[position];
        int last = size - 1;
        if (position != last) {
            dense[position] = dense[last];
            owners[position] = owners[last];
            sparse[owners[position]] = position;
        }
        dense[last] = null;
        sparse[index] = ABSENT;
        size--;
        component.reset();
        pool.push(component);
    }

    int pooled() {
        return pool.size();
    }

    private void ensureSparse(int index) {
        if (index < sparse.length) {
            return;
        }
        int oldLength = sparse.length;
        sparse = Arrays.copyOf(sparse, Math.max(index + 1, Math.max(64, oldLength * 2)));
        Arrays.fill(sparse, oldLength, sparse.length, ABSENT);
    }
}
