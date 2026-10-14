package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Хрупкая плитка: трескается, пока на ней стоят; при {@code cracks == maxCracks} разрушена. */
public final class Fragile implements Component {
    public int cracks;
    public int maxCracks;
    public float delay;
    public float timer;

    public boolean isBroken() {
        return cracks >= maxCracks;
    }

    @Override
    public void reset() {
        cracks = 0;
        maxCracks = 0;
        delay = 0;
        timer = 0;
    }
}
