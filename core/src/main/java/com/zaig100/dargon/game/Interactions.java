package com.zaig100.dargon.game;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.game.component.Container;
import com.zaig100.dargon.game.component.Door;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Interactable;
import com.zaig100.dargon.game.component.LevelExit;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.Pickup;
import com.zaig100.dargon.game.component.ScriptHandlers;
import com.zaig100.dargon.game.component.ShopStock;
import com.zaig100.dargon.game.component.Teleporter;
import com.zaig100.dargon.game.component.Text;
import com.zaig100.dargon.game.item.ItemStack;

/**
 * Что сделает «использовать» прямо сейчас — для значка на сенсорной кнопке действия. Правила
 * досягаемости те же, что у взаимодействия: своя клетка или соседняя.
 */
public final class Interactions {

    /** Подсказка: вид действия и (для предмета) сам предмет. */
    public record Preview(Kind kind, ItemStack item) {}

    /** Вид действия в порядке важности. */
    public enum Kind {
        NONE,
        EXIT,
        TELEPORT,
        PICKUP,
        BUTTON,
        DOOR,
        CHEST,
        SHOP,
        TABLET
    }

    private static final Preview NONE = new Preview(Kind.NONE, null);
    private static final Family USABLE =
            Family.all(Interactable.class, GridPos.class).exclude(Marker.Disabled.class);

    private Interactions() {}

    public static Preview preview(LevelRuntime level) {
        World world = level.world();
        GridPos at = level.playerPos();
        EntitySet usable = world.entities(USABLE);
        ComponentMapper<GridPos> pos = world.mapper(GridPos.class);
        ComponentMapper<Interactable> interactable = world.mapper(Interactable.class);
        Preview best = NONE;
        for (int i = 0; i < usable.size(); i++) {
            int e = usable.get(i);
            GridPos p = pos.get(e);
            int dx = Math.abs(p.x - at.x);
            int dy = Math.abs(p.y - at.y);
            boolean self = dx == 0 && dy == 0;
            boolean reach =
                    interactable.get(e).reach == Interactable.Reach.SELF
                            ? self
                            : !self && dx <= 1 && dy <= 1;
            if (!reach) {
                continue;
            }
            Preview candidate = kind(world, e);
            if (candidate.kind().ordinal() > 0
                    && (best.kind() == Kind.NONE
                            || candidate.kind().ordinal() < best.kind().ordinal())) {
                best = candidate;
            }
        }
        return best;
    }

    private static Preview kind(World world, int e) {
        if (world.has(e, LevelExit.class)) {
            return new Preview(Kind.EXIT, null);
        }
        if (world.has(e, Teleporter.class)) {
            return new Preview(Kind.TELEPORT, null);
        }
        Pickup pickup = world.mapper(Pickup.class).getOrNull(e);
        if (pickup != null) {
            return new Preview(Kind.PICKUP, pickup.item);
        }
        if (world.has(e, ScriptHandlers.class)) {
            return new Preview(Kind.BUTTON, null);
        }
        if (world.has(e, Door.class)) {
            return new Preview(Kind.DOOR, null);
        }
        if (world.has(e, Container.class)) {
            return new Preview(Kind.CHEST, null);
        }
        if (world.has(e, ShopStock.class)) {
            return new Preview(Kind.SHOP, null);
        }
        if (world.has(e, Text.class)) {
            return new Preview(Kind.TABLET, null);
        }
        return NONE;
    }
}
