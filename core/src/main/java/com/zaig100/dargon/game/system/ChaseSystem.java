package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.IteratingSystem;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.Occupancy;
import com.zaig100.dargon.game.component.ChaseAI;
import com.zaig100.dargon.game.component.ExplodeOnContact;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.nav.DistanceField;
import java.util.ArrayList;
import java.util.List;

/**
 * Камикадзе (DarGon3): замечает игрока на расстоянии не больше {@code radius} (по прямой) и идёт к
 * клетке, где видел его последний раз, по 8 направлениям, путём не длиннее {@code depth}. Стоя в
 * клетке игрока, взрывается: урон проходит сквозь щит.
 *
 * <p>Путь читается из общего {@link DistanceField}: одно поле на клетку-цель, пересчёт — когда цель
 * сдвинулась (игрок шагнул) или изменилась проходимость (скрипт, дверь).
 */
public final class ChaseSystem extends IteratingSystem {

    private LevelState level;
    private Occupancy occupancy;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Motion> motion;
    private ComponentMapper<ChaseAI> chase;
    private ComponentMapper<ExplodeOnContact> explode;
    private ComponentMapper<Health> health;

    /** Поля расстояний; поле, не нужное в этом тике, переиспользуется для другой цели. */
    private final List<DistanceField> fields = new ArrayList<>();

    private final int[] step = new int[2];
    private int tick;

    public ChaseSystem() {
        super(Family.all(ChaseAI.class, GridPos.class).exclude(Marker.Disabled.class));
    }

    @Override
    protected void begin(float delta) {
        tick++;
    }

    @Override
    protected void process(int entity, float delta) {
        ChaseAI ai = chase.get(entity);
        GridPos at = pos.get(entity);
        int player = level.playerEntity();
        GridPos p = pos.get(player);
        int dx = p.x - at.x;
        int dy = p.y - at.y;
        if (dx * dx + dy * dy <= ai.radius * ai.radius
                && !(ai.targetX == p.x && ai.targetY == p.y)) {
            ai.targetX = p.x;
            ai.targetY = p.y;
            ai.active = true;
        }
        if (!ai.active) {
            return;
        }
        Motion m = motion.getOrNull(entity);
        if (m == null) {
            m = world.add(entity, Motion.class);
        }
        m.speed = ai.speed;
        if (m.isMoving()) {
            return;
        }
        if (at.is(p.x, p.y) && !health.get(player).isDead()) {
            ExplodeOnContact boom = explode.getOrNull(entity);
            if (boom != null) {
                events.publish(
                        GameEvents.Damage.hit(
                                player, boom.damage, boom.damageMax, "explosion", true));
                world.destroy(entity);
                return;
            }
        }
        DistanceField field = field(ai.targetX, ai.targetY);
        if (field.nextStep(at.x, at.y, ai.depth, occupancy::isWalkable, step)) {
            m.offsetX = (at.x - step[0]) * PlayerControlSystem.TILE;
            m.offsetY = (at.y - step[1]) * PlayerControlSystem.TILE;
            m.prevOffsetX = m.offsetX;
            m.prevOffsetY = m.offsetY;
            at.set(step[0], step[1]);
        }
    }

    private DistanceField field(int x, int y) {
        DistanceField chosen = null;
        for (DistanceField field : fields) {
            if (field.isFor(x, y)) {
                chosen = field;
                break;
            }
            if (chosen == null && field.lastUsed() < tick) {
                chosen = field;
            }
        }
        if (chosen == null) {
            chosen = new DistanceField(level.tiles().width(), level.tiles().height());
            fields.add(chosen);
        }
        chosen.update(x, y, level.tilesVersion(), occupancy::isWalkable);
        chosen.markUsed(tick);
        return chosen;
    }
}
