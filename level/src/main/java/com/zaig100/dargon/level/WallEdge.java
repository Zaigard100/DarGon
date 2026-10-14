package com.zaig100.dargon.level;

/**
 * Декоративный край стены, который рисуется на клетке-стене рядом с полом. Название — стороны, с
 * которых к стене примыкает пол. Номер колонки и строки — положение кадра в тайлсете (16×16).
 *
 * <p>Символ — запись края в поле {@code edges} уровня: {@code #} — без края, {@code 1}–{@code 8} —
 * края в порядке объявления.
 */
public enum WallEdge {
    /** Края нет: рядом нет пола, или пол только с одной стороны, или с двух противоположных. */
    NONE(-1, -1),
    LEFT_DOWN(1, 1),
    UP_RIGHT(2, 0),
    LEFT_UP(1, 0),
    DOWN_RIGHT(2, 1),
    LEFT_UP_RIGHT(3, 0),
    LEFT_DOWN_RIGHT(3, 1),
    UP_DOWN_RIGHT(4, 1),
    LEFT_UP_DOWN(4, 0);

    /** Колонка кадра в тайлсете. */
    public final int column;

    /** Строка кадра в тайлсете. */
    public final int row;

    WallEdge(int column, int row) {
        this.column = column;
        this.row = row;
    }

    /** Символ края в поле {@code edges} уровня. */
    public char symbol() {
        return this == NONE ? '#' : (char) ('0' + ordinal());
    }

    /** Край по символу из поля {@code edges}; {@code null}, если символ не край. */
    public static WallEdge bySymbol(char symbol) {
        for (WallEdge edge : values()) {
            if (edge.symbol() == symbol) {
                return edge;
            }
        }
        return null;
    }
}
