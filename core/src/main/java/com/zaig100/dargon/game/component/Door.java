package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Дверь. */
public final class Door implements Component {
    public boolean open;

    @Override
    public void reset() {
        open = false;
    }
}
