package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;
import com.zaig100.dargon.level.Direction;

/** Куда смотрит сущность (арбалет, огнемёт). */
public final class Facing implements Component {
    public Direction dir = Direction.RIGHT;

    @Override
    public void reset() {
        dir = Direction.RIGHT;
    }
}
