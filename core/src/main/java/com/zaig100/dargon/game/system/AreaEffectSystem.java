package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.IteratingSystem;
import com.zaig100.dargon.game.Balance;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.component.Area;
import com.zaig100.dargon.game.component.AreaEffect;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Marker;

/**
 * Зоны: раз в {@code period} секунд лечат на 1 HP (до максимума; щит, если он есть, чинят до {@link
 * Balance#ZONE_SHIELD_REPAIR}) или снимают 1 HP сквозь щит игроку, который внутри. Часы зоны идут
 * всегда, как в DarGon3.
 */
public final class AreaEffectSystem extends IteratingSystem {

    private LevelState level;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Area> area;
    private ComponentMapper<AreaEffect> effect;
    private ComponentMapper<Health> health;

    public AreaEffectSystem() {
        super(
                Family.all(AreaEffect.class, Area.class, GridPos.class)
                        .exclude(Marker.Disabled.class));
    }

    @Override
    protected void process(int entity, float delta) {
        AreaEffect e = effect.get(entity);
        e.timer += delta;
        if (e.timer < e.period) {
            return;
        }
        e.timer = 0;
        int player = level.playerEntity();
        GridPos p = pos.get(player);
        GridPos at = pos.get(entity);
        Health h = health.get(player);
        if (!area.get(entity).contains(at.x, at.y, p.x, p.y) || h.isDead()) {
            return;
        }
        if (e.effect.equals("heal")) {
            if (h.hp < h.max) {
                h.hp++;
            }
            // Щит, если он есть, зона чинит, но не выше Balance.ZONE_SHIELD_REPAIR.
            if (h.hasShield() && h.shield < Balance.ZONE_SHIELD_REPAIR) {
                h.shield++;
            }
        } else {
            events.publish(GameEvents.Damage.periodic(player, 1, "zone"));
        }
    }
}
