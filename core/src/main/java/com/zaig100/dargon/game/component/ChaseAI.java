package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/**
 * Преследует игрока: замечает его в пределах {@code radius} (по прямой) и идёт к клетке, где видел
 * его последний раз, путём не длиннее {@code depth} шагов.
 */
public final class ChaseAI implements Component {
    public int radius;
    public int depth;
    public float speed = 1f;
    public boolean active;

    /** Где видел игрока последний раз; пока не видел — нигде. */
    public int targetX = Integer.MIN_VALUE;

    public int targetY = Integer.MIN_VALUE;

    @Override
    public void reset() {
        radius = 0;
        depth = 0;
        speed = 1f;
        active = false;
        targetX = Integer.MIN_VALUE;
        targetY = Integer.MIN_VALUE;
    }
}
