package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Анимация игрока: кадр полосы idle/run и отражение. */
public final class PlayerAnim implements Component {

    /** Время одного кадра при обычной скорости. */
    public static final float FRAME_TIME = 0.2f;

    public int frame;
    public float timer;
    public boolean running;
    public boolean flipX;

    @Override
    public void reset() {
        frame = 0;
        timer = 0;
        running = false;
        flipX = false;
    }
}
