package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.GameSystem;
import com.zaig100.dargon.ecs.IntList;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.LevelOutcome;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.component.Container;
import com.zaig100.dargon.game.component.Door;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Interactable;
import com.zaig100.dargon.game.component.LevelExit;
import com.zaig100.dargon.game.component.Lock;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.component.Pickup;
import com.zaig100.dargon.game.component.ScriptHandlers;
import com.zaig100.dargon.game.component.ShopStock;
import com.zaig100.dargon.game.component.SpriteRef;
import com.zaig100.dargon.game.component.Teleporter;
import com.zaig100.dargon.game.component.Text;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.game.script.ScriptHost;
import java.util.Iterator;

/**
 * «Использовать» (как в DarGon3 срабатывают все подходящие объекты сразу):
 *
 * <ul>
 *   <li>в клетке игрока — лестница, телепорт, предмет, кнопка (обработчик {@code onUse});
 *   <li>в соседних клетках (8 направлений) — дверь, сундук, табличка, магазин.
 * </ul>
 *
 * Монеты сразу попадают в кошелёк; другой предмет подбирается, только если для него есть место в
 * инвентаре. Дверь открывается ключом (ключ не тратится) и остаётся открытой. Сундук: первое
 * использование с ключом отпирает, следующее — отдаёт содержимое (что не поместилось, остаётся).
 * Кнопку можно нажать снова, когда игрок уйдёт с неё.
 */
public final class InteractionSystem extends GameSystem {

    /** Кадр открытого сундука. */
    private static final int CHEST_OPEN_FRAME = 1;

    private LevelState level;
    private ScriptHost scripts;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Motion> motion;
    private ComponentMapper<Health> health;
    private ComponentMapper<Interactable> interactable;
    private ComponentMapper<Teleporter> teleporter;
    private ComponentMapper<Pickup> pickup;
    private ComponentMapper<LevelExit> exit;
    private ComponentMapper<ScriptHandlers> handlers;
    private ComponentMapper<Door> door;
    private ComponentMapper<Lock> lock;
    private ComponentMapper<Container> container;
    private ComponentMapper<Text> text;
    private ComponentMapper<ShopStock> shop;
    private ComponentMapper<SpriteRef> sprite;
    private EntitySet usable;
    private EntitySet buttons;
    private final IntList found = new IntList();
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;

    @Override
    protected void initialize() {
        usable =
                world.entities(
                        Family.all(Interactable.class, GridPos.class)
                                .exclude(Marker.Disabled.class));
        buttons = world.entities(Family.all(ScriptHandlers.class, GridPos.class));
        events.subscribe(GameEvents.Use.class, this::use);
    }

    @Override
    protected void update(float delta) {
        int player = level.playerEntity();
        GridPos at = pos.get(player);
        for (int i = 0; i < buttons.size(); i++) {
            ScriptHandlers h = handlers.get(buttons.get(i));
            if (!pos.get(buttons.get(i)).is(at.x, at.y)) {
                h.armed = true;
            }
        }
        // Текст таблички скрывается, как только игрок пошёл.
        if (at.x != lastX || at.y != lastY) {
            if (lastX != Integer.MIN_VALUE) {
                level.message().clear();
            }
            lastX = at.x;
            lastY = at.y;
        }
    }

    private void use(GameEvents.Use use) {
        int player = use.player();
        if (!world.isAlive(player) || health.get(player).isDead()) {
            return;
        }
        GridPos at = pos.get(player);
        // Снимок: объекты могут исчезать и переносить игрока, но срабатывают все, кто был рядом.
        found.clear();
        for (int i = 0; i < usable.size(); i++) {
            int entity = usable.get(i);
            GridPos p = pos.get(entity);
            boolean self = p.is(at.x, at.y);
            boolean reach =
                    interactable.get(entity).reach == Interactable.Reach.SELF
                            ? self
                            : near(entity, at);
            if (reach) {
                found.add(entity);
            }
        }
        for (int i = 0; i < found.size(); i++) {
            int entity = found.get(i);
            if (world.isAlive(entity)) {
                activate(player, entity, at);
            }
        }
    }

    /** Соседняя клетка (8 направлений), но не та же. */
    private boolean near(int entity, GridPos at) {
        GridPos p = pos.get(entity);
        int dx = Math.abs(p.x - at.x);
        int dy = Math.abs(p.y - at.y);
        return dx <= 1 && dy <= 1 && (dx | dy) != 0;
    }

    private void activate(int player, int entity, GridPos at) {
        Teleporter tp = teleporter.getOrNull(entity);
        if (tp != null) {
            at.set(tp.targetX, tp.targetY);
            motion.get(player).snap();
        }
        Pickup item = pickup.getOrNull(entity);
        if (item != null && take(item.item)) {
            world.destroy(entity);
        }
        LevelExit levelExit = exit.getOrNull(entity);
        if (levelExit != null) {
            level.finish(LevelOutcome.exit(levelExit.next));
        }
        ScriptHandlers h = handlers.getOrNull(entity);
        if (h != null && h.armed && scripts.hasHandler(entity, "use")) {
            h.armed = false;
            scripts.fire(entity, "use");
        }
        Door d = door.getOrNull(entity);
        if (d != null) {
            toggleDoor(entity, d);
        } else if (container.has(entity)) {
            openChest(entity);
        }
        Text t = text.getOrNull(entity);
        if (t != null) {
            level.showMessage(t.lines);
        }
        if (shop.has(entity)) {
            level.setOpenShop(entity);
        }
    }

    /** Дверь открывается ключом и остаётся открытой. */
    private void toggleDoor(int entity, Door d) {
        Lock l = lock.getOrNull(entity);
        if (d.open || (l != null && !hasKey(l.key))) {
            return;
        }
        d.open = true;
        syncDoor(entity, d);
        level.walkabilityChanged();
    }

    /** Закрытая дверь непроходима и видна; открытая — нет. */
    public static void syncDoor(World world, int entity, Door d) {
        if (d.open) {
            world.remove(entity, Marker.Blocking.class);
        } else if (!world.has(entity, Marker.Blocking.class)) {
            world.add(entity, Marker.Blocking.class);
        }
    }

    private void syncDoor(int entity, Door d) {
        syncDoor(world, entity, d);
    }

    private void openChest(int entity) {
        Lock l = lock.getOrNull(entity);
        if (l != null && l.locked) {
            if (hasKey(l.key)) {
                l.locked = false;
            }
            return;
        }
        Container c = container.get(entity);
        if (c.items.isEmpty()) {
            return;
        }
        c.open = true;
        SpriteRef s = sprite.getOrNull(entity);
        if (s != null) {
            s.frame = CHEST_OPEN_FRAME;
        }
        for (Iterator<ItemStack> it = c.items.iterator(); it.hasNext(); ) {
            if (take(it.next())) {
                it.remove();
            }
        }
    }

    private boolean hasKey(String key) {
        if (key == null) {
            return true;
        }
        for (ItemStack stack : level.player().inventory.items()) {
            if (stack.id().equals("key") && key.equals(stack.field("key"))) {
                return true;
            }
        }
        return false;
    }

    /** Монеты — сразу в кошелёк, остальное — в инвентарь, если есть место. */
    private boolean take(ItemStack stack) {
        if (stack.id().equals(ItemStack.COINS)) {
            level.player().coins += stack.count();
            return true;
        }
        return level.player().inventory.add(stack);
    }
}
