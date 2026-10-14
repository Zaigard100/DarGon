package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Выход с уровня. {@code next == null} — конец игры. */
public final class LevelExit implements Component {
    public String next;

    @Override
    public void reset() {
        next = null;
    }
}
