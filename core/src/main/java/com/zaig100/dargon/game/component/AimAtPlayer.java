package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Арбалет-ИИ: поворачивается к игроку в пределах {@code range}. */
public final class AimAtPlayer implements Component {
    public int range;
    public boolean diagonal;

    @Override
    public void reset() {
        range = 0;
        diagonal = false;
    }
}
