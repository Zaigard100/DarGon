package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.IteratingSystem;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.SpriteRef;
import com.zaig100.dargon.game.component.TriggeredCycle;

/**
 * Шипы (таймеры DarGon3): наступили — через {@code delay} выдвигаются, выдвинутые убираются через
 * {@code 2 × delay}. Урон наносит {@link HazardSystem}, пока шипы выдвинуты.
 */
public final class SpikeSystem extends IteratingSystem {

    private LevelState level;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Health> health;
    private ComponentMapper<TriggeredCycle> cycle;
    private ComponentMapper<SpriteRef> sprite;

    public SpikeSystem() {
        super(Family.all(TriggeredCycle.class, GridPos.class).exclude(Marker.Disabled.class));
    }

    @Override
    protected void process(int entity, float delta) {
        TriggeredCycle c = cycle.get(entity);
        int player = level.playerEntity();
        GridPos p = pos.get(player);
        if (pos.get(entity).is(p.x, p.y) && !health.get(player).isDead()) {
            c.triggered = true;
        }
        if (c.triggered) {
            if (c.timer > c.delay && !c.active) {
                c.active = true;
                c.timer = 0;
                c.activeTimer = 0;
                c.triggered = false;
            } else {
                c.timer += delta;
            }
        } else if (c.activeTimer > 2 * c.delay && c.active) {
            c.active = false;
            c.activeTimer = 0;
            c.timer = 0;
        } else {
            c.activeTimer += delta;
        }
        SpriteRef s = sprite.getOrNull(entity);
        if (s != null) {
            s.region = c.active ? "spike1" : "spike0";
        }
    }
}
