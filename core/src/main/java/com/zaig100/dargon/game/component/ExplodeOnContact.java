package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Взрывается при касании игрока. */
public final class ExplodeOnContact implements Component {
    public int damage;
    public int damageMax;

    @Override
    public void reset() {
        damage = 0;
        damageMax = 0;
    }
}
