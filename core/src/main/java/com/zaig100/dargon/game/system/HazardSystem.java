package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.IteratingSystem;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.component.Fragile;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Hazard;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.TriggeredCycle;

/**
 * Опасности бьют игрока, который стоит в их клетке. Клетка игрока меняется в начале шага, поэтому
 * ловушка срабатывает сразу, как на неё шагнули (как в DarGon3).
 */
public final class HazardSystem extends IteratingSystem {

    private LevelState level;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Hazard> hazard;
    private ComponentMapper<Fragile> fragile;
    private ComponentMapper<TriggeredCycle> cycle;
    private ComponentMapper<Health> health;

    public HazardSystem() {
        super(Family.all(Hazard.class, GridPos.class).exclude(Marker.Disabled.class));
    }

    @Override
    protected void process(int entity, float delta) {
        int player = level.playerEntity();
        if (!world.isAlive(player)) {
            return;
        }
        GridPos p = pos.get(player);
        if (!pos.get(entity).is(p.x, p.y)) {
            return;
        }
        // Пока игрок неуязвим, опасность не срабатывает и ждёт (одноразовая ловушка не сгорает).
        if (health.get(player).invulnerable > 0) {
            return;
        }
        Hazard h = hazard.get(entity);
        Fragile f = fragile.getOrNull(entity);
        if (h.whenBroken && (f == null || !f.isBroken())) {
            return;
        }
        TriggeredCycle c = cycle.getOrNull(entity);
        if (c != null && !c.active) {
            return;
        }
        events.publish(
                h.damage == Hazard.KILL
                        ? GameEvents.Damage.kill(player, h.kind)
                        : GameEvents.Damage.hit(
                                player, h.damage, h.damageMax, h.kind, h.shieldable));
        if (h.once) {
            world.destroy(entity);
        }
    }
}
