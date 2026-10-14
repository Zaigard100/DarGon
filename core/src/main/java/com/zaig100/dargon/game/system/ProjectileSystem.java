package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.IteratingSystem;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.component.Projectile;

/**
 * Стрелы летят по клетке за шаг со скоростью игрока, сквозь стены (как в DarGon3). Попадание —
 * когда стрела в клетке игрока: урон, щит снижает его на 1, стрела исчезает.
 */
public final class ProjectileSystem extends IteratingSystem {

    private LevelState level;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Motion> motion;
    private ComponentMapper<Projectile> projectile;
    private ComponentMapper<Health> health;

    public ProjectileSystem() {
        super(Family.all(Projectile.class, GridPos.class, Motion.class));
    }

    @Override
    protected void process(int entity, float delta) {
        Projectile p = projectile.get(entity);
        GridPos at = pos.get(entity);
        int player = level.playerEntity();
        GridPos target = pos.get(player);
        if (at.is(target.x, target.y) && !health.get(player).isDead()) {
            events.publish(GameEvents.Damage.hit(player, p.damage, p.damageMax, "arrow", true));
            world.destroy(entity);
            return;
        }
        if (p.remaining <= 0) {
            world.destroy(entity);
            return;
        }
        Motion m = motion.get(entity);
        if (!m.isMoving()) {
            at.set(at.x + p.dx, at.y + p.dy);
            m.offsetX = -p.dx * PlayerControlSystem.TILE;
            m.offsetY = -p.dy * PlayerControlSystem.TILE;
            m.prevOffsetX = m.offsetX;
            m.prevOffsetY = m.offsetY;
            p.remaining--;
        }
    }
}
