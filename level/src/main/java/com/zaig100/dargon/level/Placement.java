package com.zaig100.dargon.level;

import java.util.Locale;

/** Где может стоять сущность префаба. */
public enum Placement {
    /** На клетке пола внутри карты. */
    FLOOR,
    /** На стене или за краем карты (огнемёты, арбалеты). */
    WALL,
    /** Где угодно, проверка не нужна (области, кусты). */
    ANY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Placement byId(String id) {
        for (Placement placement : values()) {
            if (placement.id().equals(id)) {
                return placement;
            }
        }
        return null;
    }
}
