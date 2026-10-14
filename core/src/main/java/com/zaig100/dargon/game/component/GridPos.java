package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Клетка сущности. Y растёт вниз, как в файле уровня. */
public final class GridPos implements Component {
    public int x;
    public int y;

    public GridPos set(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public boolean is(int x, int y) {
        return this.x == x && this.y == y;
    }

    @Override
    public void reset() {
        x = 0;
        y = 0;
    }
}
