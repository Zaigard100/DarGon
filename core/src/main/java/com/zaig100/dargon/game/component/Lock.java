package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Замок: открывается ключом с тем же {@code key}. */
public final class Lock implements Component {
    public String key;
    public boolean locked = true;

    @Override
    public void reset() {
        key = null;
        locked = true;
    }
}
