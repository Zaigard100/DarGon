package com.zaig100.dargon.ecs;

/**
 * Утилиты для идентификаторов сущностей.
 *
 * <p>Сущность — это {@code int}: младшие {@value #INDEX_BITS} бит — индекс слота, следующие {@value
 * #GENERATION_BITS} бит — поколение слота. При удалении сущности поколение слота увеличивается,
 * поэтому устаревший идентификатор (ссылка на уже удалённую сущность) не совпадает с новой
 * сущностью в том же слоте и распознаётся как мёртвый. Идентификатор всегда неотрицателен.
 */
public final class Entity {

    /** «Нет сущности». */
    public static final int NONE = -1;

    static final int INDEX_BITS = 20;
    static final int GENERATION_BITS = 11;

    /** Максимальное число одновременно живых сущностей. */
    public static final int MAX_ENTITIES = 1 << INDEX_BITS;

    static final int INDEX_MASK = MAX_ENTITIES - 1;
    static final int GENERATION_MASK = (1 << GENERATION_BITS) - 1;

    private Entity() {}

    /** Индекс слота сущности. */
    public static int index(int entity) {
        return entity & INDEX_MASK;
    }

    /** Поколение слота, к которому относится идентификатор. */
    public static int generation(int entity) {
        return (entity >>> INDEX_BITS) & GENERATION_MASK;
    }

    static int pack(int index, int generation) {
        return (generation << INDEX_BITS) | index;
    }

    /**
     * Человекочитаемое представление для логов и сообщений об ошибках: {@code #индекс.поколение}.
     */
    public static String toString(int entity) {
        return entity == NONE ? "#none" : "#" + index(entity) + "." + generation(entity);
    }
}
