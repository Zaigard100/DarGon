package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Действие области на игрока раз в {@code period} секунд: лечение или урон. */
public final class AreaEffect implements Component {
    public String effect;
    public float period;
    public float timer;

    @Override
    public void reset() {
        effect = null;
        period = 0;
        timer = 0;
    }
}
