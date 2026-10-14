package com.zaig100.dargon.game.item;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.game.Balance;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Interactable;
import com.zaig100.dargon.game.component.Layer;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.component.Pickup;
import com.zaig100.dargon.game.component.ShopStock;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.ItemDefinition;

/**
 * Действия игрока с предметами: использовать, выбросить, купить. Эффекты предметов — как в DarGon3
 * и действуют до конца уровня.
 */
public final class PlayerActions {

    private static final float DEFAULT_POWER = 2f;

    private final World world;
    private final ItemCatalog items;
    private final LevelState level;
    private final PlayerState player;
    private final ComponentMapper<Health> health;
    private final ComponentMapper<Motion> motion;
    private final ComponentMapper<GridPos> pos;

    public PlayerActions(World world, ItemCatalog items) {
        this.world = world;
        this.items = items;
        this.level = world.resource(LevelState.class);
        this.player = level.player();
        this.health = world.mapper(Health.class);
        this.motion = world.mapper(Motion.class);
        this.pos = world.mapper(GridPos.class);
    }

    /**
     * Использует предмет из ячейки; если эффект сработал, предмет тратится.
     *
     * @return {@code true}, если эффект сработал
     */
    public boolean use(int slot) {
        ItemStack stack = player.inventory.get(slot);
        int entity = level.playerEntity();
        Health h = health.get(entity);
        if (stack == null || h.isDead()) {
            return false;
        }
        ItemDefinition definition = items.get(stack.id());
        String effect = definition == null ? "" : definition.effect();
        boolean consumed;
        switch (effect) {
            case "heal":
                consumed = h.hp > 0 && h.hp < h.max;
                if (consumed) {
                    h.hp = Math.min(h.max, h.hp + arg(definition, "amount", 1));
                    player.hp = h.hp;
                }
                break;
            case "shield":
                // Щиты суммируются, но не выше максимума.
                consumed = h.shield < Balance.SHIELD_MAX;
                h.shield = Math.min(Balance.SHIELD_MAX, h.shield + Balance.SHIELD);
                player.shield = h.shield;
                break;
            case "light":
                consumed = level.isDark();
                level.lightTorch();
                break;
            case "speed":
                consumed = speed(power(stack), true);
                break;
            case "slow":
                consumed = speed(1f / power(stack), false);
                break;
            case "trap_sight":
                consumed = !level.trapSightPotion();
                level.setTrapSight(true);
                break;
            case "coins":
                player.coins += stack.count();
                player.inventory.set(slot, null);
                return true;
            case "egg":
                return egg(slot, stack, h);
            default:
                consumed = false;
                break;
        }
        if (consumed) {
            player.inventory.take(slot, 1);
        }
        return consumed;
    }

    /** Пасхальные яйца DarGon3: бессмертие, выход из игры, куб. */
    private boolean egg(int slot, ItemStack stack, Health h) {
        String code = String.valueOf(stack.field("code"));
        if (code.equals("inf")) {
            // Бессмертие до конца уровня; кубик тратится.
            h.immortalForLevel = true;
            h.immortal = true;
            player.inventory.take(slot, 1);
            return true;
        }
        world.events().publish(new GameEvents.EasterEgg(code));
        player.inventory.take(slot, 1);
        return true;
    }

    private boolean speed(float value, boolean faster) {
        Motion m = motion.get(level.playerEntity());
        boolean changed = faster ? m.speed < value : m.speed > value;
        if (changed) {
            m.speed = value;
        }
        return changed;
    }

    private static float power(ItemStack stack) {
        Object power = stack.field("power");
        return power instanceof Number n ? n.floatValue() : DEFAULT_POWER;
    }

    private static int arg(ItemDefinition definition, String name, int fallback) {
        return definition.args() == null ? fallback : definition.args().getInt(name, fallback);
    }

    /** Выбрасывает один предмет из ячейки на клетку игрока. */
    public boolean drop(int slot) {
        ItemStack stack = player.inventory.get(slot);
        if (stack == null || health.get(level.playerEntity()).isDead()) {
            return false;
        }
        player.inventory.take(slot, 1);
        GridPos at = pos.get(level.playerEntity());
        int item = world.create();
        world.add(item, GridPos.class).set(at.x, at.y);
        world.add(item, Layer.class).z = Layer.Z.FLOOR;
        world.add(item, Interactable.class).reach = Interactable.Reach.SELF;
        world.add(item, Pickup.class).item = stack.withCount(1);
        return true;
    }

    /**
     * Покупка в магазине. Не проходит, если товар кончился, не хватает монет или места в инвентаре.
     */
    public boolean buy(int shop, int offer) {
        ShopStock stock = world.mapper(ShopStock.class).getOrNull(shop);
        if (stock == null || offer < 0 || offer >= stock.offers.size()) {
            return false;
        }
        ShopStock.Offer o = stock.offers.get(offer);
        if (o.stock() <= 0 || player.coins < o.price()) {
            return false;
        }
        if (o.item().id().equals(ItemStack.COINS)) {
            player.coins += o.item().count() - o.price();
            o.sell();
            return true;
        }
        if (!player.inventory.add(o.item())) {
            return false;
        }
        player.coins -= o.price();
        o.sell();
        return true;
    }
}
