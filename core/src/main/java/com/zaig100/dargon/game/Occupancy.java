package com.zaig100.dargon.game;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.IntList;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Marker;

/**
 * Ресурс мира: кто стоит в клетке и можно ли в неё шагнуть. Сущностей на уровне — десятки, поэтому
 * запросы — простой проход по набору.
 */
public final class Occupancy {

    private EntitySet placed;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Marker.Blocking> blocking;
    private LevelState level;
    private Cheats cheats;

    /** Вызывается после сборки мира. */
    public void bind(World world) {
        placed = world.entities(Family.all(GridPos.class).exclude(Marker.Disabled.class));
        pos = world.mapper(GridPos.class);
        blocking = world.mapper(Marker.Blocking.class);
        level = world.resource(LevelState.class);
        cheats = world.resource(Cheats.class);
    }

    /** Включённые сущности в клетке (кроме {@code except}) — в {@code out}. */
    public IntList at(int x, int y, int except, IntList out) {
        out.clear();
        for (int i = 0; i < placed.size(); i++) {
            int entity = placed.get(i);
            if (entity != except && pos.get(entity).is(x, y)) {
                out.add(entity);
            }
        }
        return out;
    }

    /**
     * Может ли игрок встать в клетку: обычно — {@link #isWalkable}, с «сквозь стены» — любая клетка
     * карты.
     */
    public boolean isWalkableForPlayer(int x, int y) {
        if (cheats != null && cheats.noclip) {
            return level.tiles().inBounds(x, y);
        }
        return isWalkable(x, y);
    }

    /** Можно ли встать в клетку: пол и нет непроходимых сущностей. */
    public boolean isWalkable(int x, int y) {
        if (!level.tiles().isFloor(x, y)) {
            return false;
        }
        for (int i = 0; i < placed.size(); i++) {
            int entity = placed.get(i);
            if (blocking.has(entity) && pos.get(entity).is(x, y)) {
                return false;
            }
        }
        return true;
    }
}
