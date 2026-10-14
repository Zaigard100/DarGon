package com.zaig100.dargon.game.level;

import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.ecs.Component;
import com.zaig100.dargon.ecs.WorldBuilder;
import com.zaig100.dargon.game.component.AimAtPlayer;
import com.zaig100.dargon.game.component.Area;
import com.zaig100.dargon.game.component.AreaEffect;
import com.zaig100.dargon.game.component.ChaseAI;
import com.zaig100.dargon.game.component.Container;
import com.zaig100.dargon.game.component.Door;
import com.zaig100.dargon.game.component.Emitter;
import com.zaig100.dargon.game.component.EntityId;
import com.zaig100.dargon.game.component.ExplodeOnContact;
import com.zaig100.dargon.game.component.Facing;
import com.zaig100.dargon.game.component.Fragile;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Hazard;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Interactable;
import com.zaig100.dargon.game.component.Layer;
import com.zaig100.dargon.game.component.LevelExit;
import com.zaig100.dargon.game.component.Lock;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.component.Pickup;
import com.zaig100.dargon.game.component.PlayerAnim;
import com.zaig100.dargon.game.component.Projectile;
import com.zaig100.dargon.game.component.ScriptHandlers;
import com.zaig100.dargon.game.component.ShopStock;
import com.zaig100.dargon.game.component.SpriteRef;
import com.zaig100.dargon.game.component.Tags;
import com.zaig100.dargon.game.component.Teleporter;
import com.zaig100.dargon.game.component.Text;
import com.zaig100.dargon.game.component.TriggeredCycle;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.level.ComponentData;
import com.zaig100.dargon.level.GridPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Все компоненты игры: регистрация в мире ({@link WorldBuilder}) и загрузчики для префабов ({@link
 * ComponentRegistry}). Новый компонент добавляется только здесь.
 */
public final class Components {

    private final WorldBuilder world;
    private final ComponentRegistry registry;

    private Components(WorldBuilder world, ComponentRegistry registry) {
        this.world = world;
        this.registry = registry;
    }

    /** Регистрирует все компоненты в мире и (если {@code registry != null}) их загрузчики. */
    public static void registerAll(WorldBuilder world, ComponentRegistry registry) {
        new Components(world, registry).all();
    }

    /** Реестр загрузчиков без мира — для проверок данных. */
    public static ComponentRegistry registry() {
        ComponentRegistry registry = new ComponentRegistry();
        registerAll(new WorldBuilder(), registry);
        return registry;
    }

    private void all() {
        // Общие для всех сущностей (добавляются фабрикой и игрой, не префабами).
        type(GridPos.class, GridPos::new);
        type(Motion.class, Motion::new);
        type(EntityId.class, EntityId::new);
        type(Tags.class, Tags::new);
        type(Marker.Disabled.class, Marker.Disabled::new);
        type(Marker.Player.class, Marker.Player::new);
        type(Health.class, Health::new);
        type(PlayerAnim.class, PlayerAnim::new);
        type(Projectile.class, Projectile::new);

        // Компоненты префабов.
        prefab(
                "SpriteRef",
                SpriteRef.class,
                SpriteRef::new,
                (c, d, e) -> {
                    c.region = d.getString("region", null);
                    c.frame = d.getInt("frame", 0);
                    c.flipX = d.getBool("flipX", false);
                    c.angle = d.getFloat("angle", 0);
                    if (d.has("offset")) {
                        GridPoint offset = d.getPoint("offset");
                        c.offsetX = offset.x();
                        c.offsetY = offset.y();
                    }
                });
        prefab(
                "Layer",
                Layer.class,
                Layer::new,
                (c, d, e) -> c.z = enumValue(d, "z", Layer.Z.class));
        prefab("Hidden", Marker.Hidden.class, Marker.Hidden::new, null);
        prefab("Blocking", Marker.Blocking.class, Marker.Blocking::new, null);
        prefab(
                "Area",
                Area.class,
                Area::new,
                (c, d, e) -> {
                    c.width = d.getInt("width");
                    c.height = d.getInt("height");
                });
        prefab(
                "AreaEffect",
                AreaEffect.class,
                AreaEffect::new,
                (c, d, e) -> {
                    c.effect = d.getString("effect");
                    c.period = d.getFloat("period");
                });
        prefab(
                "Interactable",
                Interactable.class,
                Interactable::new,
                (c, d, e) -> c.reach = enumValue(d, "reach", Interactable.Reach.class));
        prefab(
                "ScriptHandlers",
                ScriptHandlers.class,
                ScriptHandlers::new,
                (c, d, e) -> c.code.putAll(e.handlers()));
        prefab(
                "Container",
                Container.class,
                Container::new,
                (c, d, e) -> {
                    c.items.addAll(stacks(d.get("items")));
                    c.open = d.getBool("open", false);
                });
        prefab(
                "Lock",
                Lock.class,
                Lock::new,
                (c, d, e) -> {
                    c.key = d.getString("key", null);
                    c.locked = d.getBool("locked", true);
                });
        prefab("Door", Door.class, Door::new, (c, d, e) -> c.open = d.getBool("open", false));
        prefab("Facing", Facing.class, Facing::new, (c, d, e) -> c.dir = d.getDirection("dir"));
        prefab(
                "Emitter",
                Emitter.class,
                Emitter::new,
                (c, d, e) -> {
                    c.kind = d.getString("kind");
                    c.dir = d.getDirection("dir");
                    c.period = d.getFloat("period");
                    c.maxLength = d.getInt("maxLength", 0);
                    c.stage = d.getInt("stage", 0);
                });
        prefab(
                "Fragile",
                Fragile.class,
                Fragile::new,
                (c, d, e) -> {
                    c.cracks = d.getInt("cracks", 0);
                    c.maxCracks = d.getInt("maxCracks");
                    c.delay = d.getFloat("delay");
                });
        prefab(
                "Hazard",
                Hazard.class,
                Hazard::new,
                (c, d, e) -> {
                    int[] range = damage(d);
                    c.damage = range[0];
                    c.damageMax = range[1];
                    c.kind = d.getString("kind");
                    c.shieldable = d.getBool("shieldable", false);
                    c.once = d.getBool("once", false);
                    c.whenBroken = d.getBool("whenBroken", false);
                });
        prefab(
                "Pickup",
                Pickup.class,
                Pickup::new,
                (c, d, e) -> c.item = ItemStack.fromJson(d.get("item")));
        prefab(
                "ChaseAI",
                ChaseAI.class,
                ChaseAI::new,
                (c, d, e) -> {
                    c.radius = d.getInt("radius");
                    c.depth = d.getInt("depth");
                    c.speed = d.getFloat("speed", 1f);
                });
        prefab(
                "ExplodeOnContact",
                ExplodeOnContact.class,
                ExplodeOnContact::new,
                (c, d, e) -> {
                    int[] range = damage(d);
                    c.damage = range[0];
                    c.damageMax = range[1];
                });
        prefab(
                "ShopStock",
                ShopStock.class,
                ShopStock::new,
                (c, d, e) -> {
                    for (JsonValue offer = d.get("offers").child;
                            offer != null;
                            offer = offer.next) {
                        c.offers.add(
                                new ShopStock.Offer(
                                        ItemStack.fromJson(offer.get("item")),
                                        offer.getInt("price"),
                                        offer.getInt("stock", 1)));
                    }
                });
        prefab(
                "AimAtPlayer",
                AimAtPlayer.class,
                AimAtPlayer::new,
                (c, d, e) -> {
                    c.range = d.getInt("range");
                    c.diagonal = d.getBool("diagonal", false);
                });
        prefab(
                "TriggeredCycle",
                TriggeredCycle.class,
                TriggeredCycle::new,
                (c, d, e) -> {
                    c.active = d.getBool("active", false);
                    c.delay = d.getFloat("delay");
                });
        prefab(
                "LevelExit",
                LevelExit.class,
                LevelExit::new,
                (c, d, e) -> c.next = d.getString("next", null));
        prefab(
                "Text",
                Text.class,
                Text::new,
                (c, d, e) -> {
                    JsonValue text = d.get("text");
                    if (text.isArray()) {
                        for (JsonValue line = text.child; line != null; line = line.next) {
                            c.lines.add(line.asString());
                        }
                    } else {
                        c.lines.add(d.getString("text"));
                    }
                });
        prefab(
                "Teleporter",
                Teleporter.class,
                Teleporter::new,
                (c, d, e) -> {
                    GridPoint target = d.getPoint("target");
                    c.targetX = target.x();
                    c.targetY = target.y();
                });
    }

    private <T extends Component> void type(Class<T> type, Supplier<T> factory) {
        world.component(type, factory);
    }

    private <T extends Component> void prefab(
            String name, Class<T> type, Supplier<T> factory, ComponentRegistry.Loader<T> loader) {
        world.component(type, factory);
        if (registry != null) {
            if (loader == null) {
                registry.register(name, type);
            } else {
                registry.register(name, type, loader);
            }
        }
    }

    /** Урон: {@code "kill"}, число или диапазон {@code [min, max]}. */
    private static int[] damage(ComponentData d) {
        JsonValue damage = d.get("damage");
        if (damage != null && damage.isString() && damage.asString().equals("kill")) {
            return new int[] {Hazard.KILL, Hazard.KILL};
        }
        if (damage != null && damage.isArray() && damage.size == 2) {
            int min = damage.getInt(0);
            int max = damage.getInt(1);
            if (min < 0 || max < min) {
                throw d.error("damage", "ожидался диапазон [min, max] с 0 <= min <= max");
            }
            return new int[] {min, max};
        }
        int value = d.getInt("damage");
        return new int[] {value, value};
    }

    private static List<ItemStack> stacks(JsonValue array) {
        List<ItemStack> result = new ArrayList<>();
        if (array != null) {
            for (JsonValue stack = array.child; stack != null; stack = stack.next) {
                result.add(ItemStack.fromJson(stack));
            }
        }
        return result;
    }

    private static <E extends Enum<E>> E enumValue(ComponentData data, String key, Class<E> type) {
        String value = data.getString(key);
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw data.error(key, "неизвестное значение '" + value + "'");
        }
    }
}
