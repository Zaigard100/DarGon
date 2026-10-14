package com.zaig100.dargon.ecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Живой набор сущностей, подходящих под {@link Family}. Получается через {@link World#entities}.
 *
 * <p>Внутри {@link World#update} набор меняется только в точках синхронизации (после каждой
 * системы), поэтому его можно безопасно обходить по индексу, даже если во время обхода сущности
 * удаляются или меняют состав:
 *
 * <pre>{@code
 * for (int i = 0; i < set.size(); i++) {
 *     int entity = set.get(i);
 *     ...
 * }
 * }</pre>
 *
 * <p>Порядок элементов не гарантирован, но детерминирован: одинаковая последовательность действий
 * даёт одинаковый порядок.
 */
public final class EntitySet {

    /** Подписчик на изменения набора. Вызывается в точке синхронизации мира. */
    public interface Listener {
        /** Сущность вошла в набор. */
        default void added(int entity) {}

        /**
         * Сущность вышла из набора (удалена или сменила состав). Компоненты, из-за которых она
         * вышла, в этот момент ещё доступны для чтения.
         */
        default void removed(int entity) {}
    }

    private static final int ABSENT = -1;

    private final World world;
    private final Family family;
    private final long[] all;
    private final long[] one;
    private final long[] exclude;
    private final boolean hasOne;

    private int[] entities = new int[16];
    private int[] positions = new int[0];
    private int size;
    private final List<Listener> listeners = new ArrayList<>();

    EntitySet(World world, Family family, long[] all, long[] one, long[] exclude) {
        this.world = world;
        this.family = family;
        this.all = all;
        this.one = one;
        this.exclude = exclude;
        boolean any = false;
        for (long word : one) {
            any |= word != 0;
        }
        this.hasOne = any;
    }

    public Family family() {
        return family;
    }

    /** Размер набора. Вне {@link World#update} сначала синхронизирует мир. */
    public int size() {
        world.syncIfIdle();
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /** Сущность по позиции {@code 0 ≤ i < size()}. */
    public int get(int i) {
        if (i >= size) {
            throw new IndexOutOfBoundsException(i + " >= " + size);
        }
        return entities[i];
    }

    /** Содержит ли набор именно эту сущность (с учётом поколения). */
    public boolean contains(int entity) {
        world.syncIfIdle();
        int index = Entity.index(entity);
        return index < positions.length
                && positions[index] != ABSENT
                && entities[positions[index]] == entity;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    boolean matches(long[] composition, int offset) {
        boolean anyOne = !hasOne;
        for (int w = 0; w < all.length; w++) {
            long bits = composition[offset + w];
            if ((bits & all[w]) != all[w] || (bits & exclude[w]) != 0) {
                return false;
            }
            anyOne |= (bits & one[w]) != 0;
        }
        return anyOne;
    }

    boolean containsIndex(int index) {
        return index < positions.length && positions[index] != ABSENT;
    }

    void insert(int entity) {
        int index = Entity.index(entity);
        if (index >= positions.length) {
            int oldLength = positions.length;
            positions = Arrays.copyOf(positions, Math.max(index + 1, Math.max(64, oldLength * 2)));
            Arrays.fill(positions, oldLength, positions.length, ABSENT);
        }
        if (size == entities.length) {
            entities = Arrays.copyOf(entities, size * 2);
        }
        entities[size] = entity;
        positions[index] = size;
        size++;
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).added(entity);
        }
    }

    void erase(int index) {
        int position = positions[index];
        int entity = entities[position];
        int last = size - 1;
        if (position != last) {
            entities[position] = entities[last];
            positions[Entity.index(entities[position])] = position;
        }
        positions[index] = ABSENT;
        size--;
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).removed(entity);
        }
    }

    @Override
    public String toString() {
        return "EntitySet{" + family + ", size=" + size + "}";
    }
}
