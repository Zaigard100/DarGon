package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.IteratingSystem;
import com.zaig100.dargon.game.Balance;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.component.AimAtPlayer;
import com.zaig100.dargon.game.component.Emitter;
import com.zaig100.dargon.game.component.Facing;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Layer;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.component.Projectile;
import com.zaig100.dargon.game.component.SpriteRef;
import com.zaig100.dargon.level.Direction;

/**
 * Арбалеты и огнемёты.
 *
 * <p>Арбалет выпускает стрелу в первый тик, когда он включён, и затем раз в {@code period} секунд.
 * Арбалет-ИИ стреляет, только пока игрок в пределах {@code range}, и поворачивается к нему, если
 * игрок на одной линии с ним (по горизонтали, вертикали или — если разрешено — диагонали) и между
 * ними нет стен.
 *
 * <p>Огнемёт: пламя растёт на клетку раз в {@code period} секунд до {@code maxLength}, затем
 * гаснет. Как в DarGon3, таймер идёт тем быстрее, чем длиннее пламя: в {@code длина + 1} раз. Пламя
 * убивает и проходит сквозь стены.
 */
public final class EmitterSystem extends IteratingSystem {

    /** Смена кадра огня. */
    private static final float FIRE_FRAME_TIME = 0.2f;

    private LevelState level;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Emitter> emitter;
    private ComponentMapper<Facing> facing;
    private ComponentMapper<AimAtPlayer> aim;
    private ComponentMapper<Health> health;

    public EmitterSystem() {
        super(Family.all(Emitter.class, GridPos.class).exclude(Marker.Disabled.class));
    }

    @Override
    protected void process(int entity, float delta) {
        Emitter e = emitter.get(entity);
        if (e.kind.equals("fire")) {
            fire(entity, e, delta);
        } else {
            arrows(entity, e, delta);
        }
    }

    private void arrows(int entity, Emitter e, float delta) {
        if (!e.started) {
            // Первая стрела — сразу при появлении арбалета (и у арбалета-ИИ тоже).
            e.started = true;
            shoot(entity, e);
        }
        AimAtPlayer ai = aim.getOrNull(entity);
        GridPos at = pos.get(entity);
        GridPos player = pos.get(level.playerEntity());
        if (ai != null) {
            int dx = player.x - at.x;
            int dy = player.y - at.y;
            if (dx * dx + dy * dy > ai.range * ai.range) {
                return;
            }
        }
        e.timer += delta;
        if (e.timer >= e.period) {
            shoot(entity, e);
            e.timer = 0;
        }
        if (ai != null) {
            aimAt(entity, e, ai, at, player);
        }
    }

    private void aimAt(int entity, Emitter e, AimAtPlayer ai, GridPos at, GridPos player) {
        int dx = player.x - at.x;
        int dy = player.y - at.y;
        boolean straight = (dx == 0) != (dy == 0);
        boolean diagonal = ai.diagonal && dx != 0 && Math.abs(dx) == Math.abs(dy);
        if ((!straight && !diagonal) || !clearLine(at, dx, dy)) {
            return;
        }
        e.dir = Direction.of(Integer.signum(dx), Integer.signum(dy));
        Facing f = facing.getOrNull(entity);
        if (f != null) {
            f.dir = e.dir;
        }
    }

    /** Между клетками на одной линии нет стен (сами концы не проверяются). */
    private boolean clearLine(GridPos from, int dx, int dy) {
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        int sx = Integer.signum(dx);
        int sy = Integer.signum(dy);
        for (int i = 1; i < steps; i++) {
            if (!level.tiles().isFloor(from.x + sx * i, from.y + sy * i)) {
                return false;
            }
        }
        return true;
    }

    private void shoot(int entity, Emitter e) {
        GridPos at = pos.get(entity);
        int arrow = world.create();
        world.add(arrow, GridPos.class).set(at.x, at.y);
        world.add(arrow, Motion.class);
        world.add(arrow, Layer.class).z = Layer.Z.OVER;
        world.add(arrow, Facing.class).dir = e.dir;
        SpriteRef sprite = world.add(arrow, SpriteRef.class);
        sprite.region = "arrow";
        sprite.angle = 180;
        Projectile p = world.add(arrow, Projectile.class);
        p.dx = e.dir.dx;
        p.dy = e.dir.dy;
        p.damage = Balance.ARROW_MIN;
        p.damageMax = Balance.ARROW_MAX;
        // Стрела пролетает удвоенный больший размер карты, как в DarGon3.
        p.remaining = 2 * Math.max(level.tiles().width(), level.tiles().height());
    }

    private void fire(int entity, Emitter e, float delta) {
        int speedUp = e.stage + 1;
        e.timer += delta * speedUp;
        if (e.timer >= e.period) {
            e.stage++;
            e.timer = 0;
            if (e.stage > e.maxLength) {
                e.stage = 0;
            }
        }
        e.fireTimer += delta * speedUp;
        if (e.fireTimer >= FIRE_FRAME_TIME) {
            e.fireFrame = (e.fireFrame + 1) % 2;
            e.fireTimer = 0;
        }
        int player = level.playerEntity();
        if (health.get(player).isDead()) {
            return;
        }
        GridPos at = pos.get(entity);
        GridPos p = pos.get(player);
        for (int i = 1; i <= e.stage; i++) {
            if (p.is(at.x + e.dir.dx * i, at.y + e.dir.dy * i)) {
                events.publish(
                        GameEvents.Damage.hit(
                                player, Balance.FIRE_MIN, Balance.FIRE_MAX, "fire", true));
                return;
            }
        }
    }
}
