package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Летящая стрела: шаг на клетку {@code (dx, dy)}, осталось {@code remaining} шагов. */
public final class Projectile implements Component {
    public int dx;
    public int dy;
    public int remaining;
    public int damage;
    public int damageMax;

    @Override
    public void reset() {
        dx = 0;
        dy = 0;
        remaining = 0;
        damage = 0;
        damageMax = 0;
    }
}
