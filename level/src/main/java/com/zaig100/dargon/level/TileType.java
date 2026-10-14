package com.zaig100.dargon.level;

import java.util.Locale;

/** Тип клетки карты. Декоративные края стен не хранятся — их вычисляет {@link Autotiler}. */
public enum TileType {
    /** Пол: по нему можно ходить. */
    FLOOR('.', true),
    /** Стена или пустота: непроходима, рядом с полом рисуется край. */
    WALL('#', false);

    /** Символ в легенде по умолчанию. */
    public final char symbol;

    public final boolean walkable;

    TileType(char symbol, boolean walkable) {
        this.symbol = symbol;
        this.walkable = walkable;
    }

    /** Имя в файлах уровней: {@code "floor"}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Тип по имени из файла уровня; {@code null}, если такого нет. */
    public static TileType byId(String id) {
        for (TileType type : values()) {
            if (type.id().equals(id)) {
                return type;
            }
        }
        return null;
    }
}
