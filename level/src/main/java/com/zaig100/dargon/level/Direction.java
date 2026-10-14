package com.zaig100.dargon.level;

import java.util.Locale;

/** Направление на сетке. Y растёт вниз: {@link #UP} — это {@code dy = -1}. */
public enum Direction {
    RIGHT(1, 0),
    DOWN_RIGHT(1, 1),
    DOWN(0, 1),
    DOWN_LEFT(-1, 1),
    LEFT(-1, 0),
    UP_LEFT(-1, -1),
    UP(0, -1),
    UP_RIGHT(1, -1);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** Горизонталь или вертикаль. */
    public boolean isOrthogonal() {
        return dx == 0 || dy == 0;
    }

    /** Угол поворота спрайта в градусах против часовой стрелки (RIGHT = 0, UP = 90). */
    public float degrees() {
        return ordinal() == 0 ? 0 : 360 - ordinal() * 45f;
    }

    /** Имя в файлах уровней: {@code "up_left"}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Направление по имени из файла уровня; {@code null}, если такого нет. */
    public static Direction byId(String id) {
        for (Direction direction : values()) {
            if (direction.id().equals(id)) {
                return direction;
            }
        }
        return null;
    }

    /** Направление по смещению (каждая компонента -1, 0 или 1); {@code null} для (0, 0). */
    public static Direction of(int dx, int dy) {
        for (Direction direction : values()) {
            if (direction.dx == Integer.signum(dx) && direction.dy == Integer.signum(dy)) {
                return direction;
            }
        }
        return null;
    }
}
