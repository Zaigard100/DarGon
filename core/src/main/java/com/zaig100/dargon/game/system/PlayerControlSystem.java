package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.GameSystem;
import com.zaig100.dargon.ecs.IntList;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.Occupancy;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Interactable;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.nav.Pathfinder;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.GridPoint;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Превращает действия игрока в шаги и «использовать».
 *
 * <p>Как в DarGon3: новый шаг начинается, только когда закончился предыдущий; две нажатые стрелки
 * дают шаг по диагонали, и проверяется только клетка назначения. Из противоположных направлений
 * побеждает «вниз» и «вправо». Нажатие во время шага не теряется, а выполняется после него.
 *
 * <p>Клик по клетке строит путь и ведёт по нему обычными шагами. Клик по клетке игрока — это
 * «использовать»; клик по клетке с предметом, лестницей и т. п. — дойти и использовать. На тёмном
 * уровне без факела кликать можно только по клеткам в круге света.
 */
public final class PlayerControlSystem extends GameSystem {

    /** Размер клетки в пикселях тайла. */
    public static final int TILE = 16;

    /**
     * Радиус света вокруг игрока на тёмном уровне без факела, пиксели тайла (середина мягкого края
     * круга в darkness.frag): видны соседние клетки, включая диагональные.
     */
    private static final int LIGHT = 24;

    private ActionState input;
    private LevelState level;
    private Occupancy occupancy;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Motion> motion;
    private ComponentMapper<Health> health;
    private ComponentMapper<Interactable> interactable;

    private Pathfinder pathfinder;
    private final Deque<GridPoint> path = new ArrayDeque<>();
    private boolean useOnArrival;
    private int bufferedDx;
    private int bufferedDy;
    private final IntList found = new IntList();

    @Override
    protected void initialize() {
        pathfinder = new Pathfinder(level.tiles().width(), level.tiles().height());
    }

    @Override
    protected void update(float delta) {
        int player = level.playerEntity();
        if (!world.isAlive(player) || health.get(player).isDead()) {
            path.clear();
            bufferedDx = 0;
            bufferedDy = 0;
            return;
        }
        GridPos at = pos.get(player);
        GridPoint clicked = input.clicked();
        if (clicked != null) {
            onClick(at, clicked);
        }
        if (input.wasPressed(Action.USE)) {
            path.clear();
            useOnArrival = false;
            events.publish(new GameEvents.Use(player));
        }
        if (anyMoveKey()) {
            path.clear();
            useOnArrival = false;
        }
        Motion m = motion.get(player);
        if (m.isMoving()) {
            // Нажатие во время шага запоминается и выполняется сразу после него.
            bufferedDx = axis(Action.MOVE_LEFT, Action.MOVE_RIGHT, false, bufferedDx);
            bufferedDy = axis(Action.MOVE_UP, Action.MOVE_DOWN, false, bufferedDy);
            return;
        }
        int dx = axis(Action.MOVE_LEFT, Action.MOVE_RIGHT, true, 0);
        int dy = axis(Action.MOVE_UP, Action.MOVE_DOWN, true, 0);
        if (dx == 0 && dy == 0) {
            dx = bufferedDx;
            dy = bufferedDy;
        }
        bufferedDx = 0;
        bufferedDy = 0;
        if (dx != 0 || dy != 0) {
            step(at, m, at.x + dx, at.y + dy);
            return;
        }
        if (!path.isEmpty()) {
            GridPoint next = path.poll();
            if (!step(at, m, next.x(), next.y())) {
                // Путь перекрыли (закрылась дверь) — останавливаемся.
                path.clear();
                useOnArrival = false;
            }
        } else if (useOnArrival) {
            useOnArrival = false;
            events.publish(new GameEvents.Use(player));
        }
    }

    /**
     * Направление по оси: {@code -1}, {@code 1} или {@code fallback}. Из двух противоположных
     * побеждает положительное («вниз», «вправо»), как в DarGon3.
     *
     * @param held по удержанию ({@code true}) или по нажатиям с прошлого тика
     */
    private int axis(Action negative, Action positive, boolean held, int fallback) {
        int result = fallback;
        if (held ? input.isHeld(negative) : input.wasPressed(negative)) {
            result = -1;
        }
        if (held ? input.isHeld(positive) : input.wasPressed(positive)) {
            result = 1;
        }
        return result;
    }

    private boolean anyMoveKey() {
        return input.isHeld(Action.MOVE_UP)
                || input.isHeld(Action.MOVE_DOWN)
                || input.isHeld(Action.MOVE_LEFT)
                || input.isHeld(Action.MOVE_RIGHT);
    }

    /** Есть ли незавершённый путь по клику. */
    public boolean isFollowingPath() {
        return !path.isEmpty() || useOnArrival;
    }

    private void onClick(GridPos at, GridPoint target) {
        path.clear();
        useOnArrival = false;
        if (at.is(target.x(), target.y())) {
            events.publish(new GameEvents.Use(level.playerEntity()));
            return;
        }
        if (level.isDark() && !inLight(at, target)) {
            // В темноте не ходим туда, чего не видно; с факелом видно всё.
            return;
        }
        List<GridPoint> route =
                pathfinder.find(new GridPoint(at.x, at.y), target, occupancy::isWalkableForPlayer);
        if (route == null) {
            return;
        }
        path.addAll(route);
        useOnArrival = hasSelfInteractable(target);
    }

    private static boolean inLight(GridPos at, GridPoint cell) {
        int dx = (cell.x() - at.x) * TILE;
        int dy = (cell.y() - at.y) * TILE;
        return dx * dx + dy * dy <= LIGHT * LIGHT;
    }

    private boolean hasSelfInteractable(GridPoint cell) {
        occupancy.at(cell.x(), cell.y(), level.playerEntity(), found);
        for (int i = 0; i < found.size(); i++) {
            Interactable it = interactable.getOrNull(found.get(i));
            if (it != null && it.reach == Interactable.Reach.SELF) {
                return true;
            }
        }
        return false;
    }

    /** Шаг в клетку, если в неё можно встать. Клетка меняется сразу, картинка догоняет. */
    private boolean step(GridPos at, Motion m, int x, int y) {
        if (!occupancy.isWalkableForPlayer(x, y)) {
            return false;
        }
        m.offsetX = (at.x - x) * TILE;
        m.offsetY = (at.y - y) * TILE;
        m.prevOffsetX = m.offsetX;
        m.prevOffsetY = m.offsetY;
        at.set(x, y);
        world.events().publish(new GameEvents.Step(level.playerEntity(), x, y));
        return true;
    }
}
