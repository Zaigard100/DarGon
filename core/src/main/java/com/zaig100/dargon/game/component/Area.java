package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Прямоугольная область от клетки сущности вправо и вниз (кусты, зоны). */
public final class Area implements Component {
    public int width = 1;
    public int height = 1;

    /** Клетка внутри области сущности с левым верхним углом {@code (x0, y0)}. */
    public boolean contains(int x0, int y0, int x, int y) {
        return x >= x0 && y >= y0 && x < x0 + width && y < y0 + height;
    }

    @Override
    public void reset() {
        width = 1;
        height = 1;
    }
}
