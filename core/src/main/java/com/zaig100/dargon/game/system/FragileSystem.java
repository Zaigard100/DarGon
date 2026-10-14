package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.IteratingSystem;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.component.Fragile;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.SpriteRef;

/**
 * Хрупкая плитка (DarGon3): пока на ней стоят, раз в {@code delay} секунд добавляется трещина;
 * первая — сразу, как наступили. Разрушенная убивает ({@link HazardSystem}).
 */
public final class FragileSystem extends IteratingSystem {

    /** Кадры трещин: 1, 2 и 3 трещины (разрушена). */
    private static final String[] CRACKS = {null, "2_clvl", "1_clvl", "0_clvl"};

    private LevelState level;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Health> health;
    private ComponentMapper<Fragile> fragile;
    private ComponentMapper<SpriteRef> sprite;

    public FragileSystem() {
        super(Family.all(Fragile.class, GridPos.class).exclude(Marker.Disabled.class));
    }

    @Override
    protected void process(int entity, float delta) {
        Fragile f = fragile.get(entity);
        int player = level.playerEntity();
        GridPos p = pos.get(player);
        if (pos.get(entity).is(p.x, p.y)
                && !health.get(player).isDead()
                && f.timer < 0
                && !f.isBroken()) {
            f.cracks++;
            f.timer = f.delay;
        }
        f.timer -= delta;
        SpriteRef s = sprite.getOrNull(entity);
        if (s != null) {
            s.region = CRACKS[Math.min(f.cracks, CRACKS.length - 1)];
        }
    }
}
