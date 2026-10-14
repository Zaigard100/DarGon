package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Телепорт в клетку {@code (targetX, targetY)}. */
public final class Teleporter implements Component {
    public int targetX;
    public int targetY;

    @Override
    public void reset() {
        targetX = 0;
        targetY = 0;
    }
}
