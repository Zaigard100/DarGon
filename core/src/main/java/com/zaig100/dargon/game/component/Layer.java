package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Слой отрисовки: под игроком или над ним. */
public final class Layer implements Component {

    /** Слои в порядке отрисовки. */
    public enum Z {
        FLOOR,
        OVER
    }

    public Z z = Z.FLOOR;

    @Override
    public void reset() {
        z = Z.FLOOR;
    }
}
