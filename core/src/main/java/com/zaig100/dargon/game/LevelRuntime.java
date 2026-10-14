package com.zaig100.dargon.game;

import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.ecs.WorldBuilder;
import com.zaig100.dargon.game.component.Container;
import com.zaig100.dargon.game.component.Door;
import com.zaig100.dargon.game.component.EntityId;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.component.PlayerAnim;
import com.zaig100.dargon.game.component.SpriteRef;
import com.zaig100.dargon.game.component.Tags;
import com.zaig100.dargon.game.level.ComponentRegistry;
import com.zaig100.dargon.game.level.Components;
import com.zaig100.dargon.game.level.EntityFactory;
import com.zaig100.dargon.game.script.ScriptHost;
import com.zaig100.dargon.game.system.AreaEffectSystem;
import com.zaig100.dargon.game.system.ChaseSystem;
import com.zaig100.dargon.game.system.DamageSystem;
import com.zaig100.dargon.game.system.EmitterSystem;
import com.zaig100.dargon.game.system.FragileSystem;
import com.zaig100.dargon.game.system.HazardSystem;
import com.zaig100.dargon.game.system.InteractionSystem;
import com.zaig100.dargon.game.system.MotionSystem;
import com.zaig100.dargon.game.system.PlayerControlSystem;
import com.zaig100.dargon.game.system.ProjectileSystem;
import com.zaig100.dargon.game.system.ScriptSystem;
import com.zaig100.dargon.game.system.SpikeSystem;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.LevelDefinition;
import java.util.Random;
import java.util.function.Function;

/**
 * Идущий уровень: мир ECS с системами, сущности уровня и игрок. Не зависит от графики — так же
 * работает в тестах.
 */
public final class LevelRuntime {

    /** Шаг логики. */
    public static final float TICK = 1f / 60f;

    private final World world;
    private final LevelState state;
    private final ActionState input;
    private final int player;

    private LevelRuntime(World world, LevelState state, ActionState input, int player) {
        this.world = world;
        this.state = state;
        this.input = input;
        this.player = player;
    }

    /**
     * Запускает уровень.
     *
     * @param player состояние игрока; уровень меняет его (здоровье, монеты, инвентарь)
     */
    public static LevelRuntime start(LevelDefinition level, PlayerState player, ActionState input) {
        return start(level, player, input, null, GameLog.NONE);
    }

    /**
     * Запускает уровень со скриптом.
     *
     * @param scriptSource текст файла скрипта уровня ({@link LevelDefinition#script()}) или {@code
     *     null}
     */
    public static LevelRuntime start(
            LevelDefinition level,
            PlayerState player,
            ActionState input,
            String scriptSource,
            GameLog log) {
        return start(level, player, input, scriptSource, log, new Cheats());
    }

    /** Запуск с инструментами разработчика сессии. */
    public static LevelRuntime start(
            LevelDefinition level,
            PlayerState player,
            ActionState input,
            String scriptSource,
            GameLog log,
            Cheats cheats) {
        return start(level, player, input, scriptSource, path -> null, log, cheats);
    }

    /**
     * Запуск со скриптами пака.
     *
     * @param library общие скрипты для {@code use}: путь относительно каталога уровней — исходник
     *     или {@code null}
     */
    public static LevelRuntime start(
            LevelDefinition level,
            PlayerState player,
            ActionState input,
            String scriptSource,
            Function<String, String> library,
            GameLog log,
            Cheats cheats) {
        return start(level, player, input, scriptSource, library, log, cheats, new Random(), null);
    }

    /** Запуск с заданным генератором случайных чисел (тесты: воспроизводимый урон). */
    public static LevelRuntime start(
            LevelDefinition level,
            PlayerState player,
            ActionState input,
            String scriptSource,
            GameLog log,
            Cheats cheats,
            Random random) {
        return start(level, player, input, scriptSource, path -> null, log, cheats, random, null);
    }

    /** Запуск под отладчиком скриптов (редактор паков). */
    public static LevelRuntime start(
            LevelDefinition level,
            PlayerState player,
            ActionState input,
            String scriptSource,
            Function<String, String> library,
            GameLog log,
            Cheats cheats,
            Random random,
            com.zaig100.dargon.script.Debugger debugger) {
        LevelState state = new LevelState(level, player);
        ScriptHost scripts =
                new ScriptHost(
                        log, level.id(), level.scriptPath(), scriptSource, library, debugger);
        Occupancy occupancy = new Occupancy();
        ComponentRegistry registry = new ComponentRegistry();
        WorldBuilder builder = new WorldBuilder();
        Components.registerAll(builder, registry);
        World world =
                builder.resource(state)
                        .resource(occupancy)
                        .resource(input)
                        .resource(scripts)
                        .resource(cheats)
                        .resource(Random.class, random)
                        .system(new PlayerControlSystem())
                        .system(new MotionSystem())
                        .system(new ChaseSystem())
                        .system(new EmitterSystem())
                        .system(new ProjectileSystem())
                        .system(new HazardSystem())
                        .system(new SpikeSystem())
                        .system(new FragileSystem())
                        .system(new AreaEffectSystem())
                        .system(new DamageSystem())
                        .system(new InteractionSystem())
                        .system(new ScriptSystem())
                        .build();
        occupancy.bind(world);
        new EntityFactory(registry)
                .decorator(
                        (w, entity, spec) -> {
                            w.add(entity, GridPos.class).set(spec.at().x(), spec.at().y());
                            if (spec.id() != null) {
                                w.add(entity, EntityId.class).id = spec.id();
                            }
                            if (!spec.tags().isEmpty()) {
                                w.add(entity, Tags.class).tags.addAll(spec.tags());
                            }
                            if (!spec.enabled()) {
                                w.add(entity, Marker.Disabled.class);
                            }
                        })
                .spawnAll(world, level);
        int entity = world.create();
        world.add(entity, Marker.Player.class);
        world.add(entity, GridPos.class).set(level.spawn().x(), level.spawn().y());
        world.add(entity, Motion.class);
        world.add(entity, PlayerAnim.class);
        Health health = world.add(entity, Health.class);
        health.hp = player.hp;
        health.max = PlayerState.MAX_HP;
        health.shield = player.shield;
        state.setPlayerEntity(entity);
        prepareObjects(world);
        world.sync();
        scripts.bind(world);
        return new LevelRuntime(world, state, input, entity);
    }

    /** Состояние объектов, которое зависит от параметров: двери, открытые сундуки. */
    private static void prepareObjects(World world) {
        EntitySet doors = world.entities(Family.all(Door.class));
        for (int i = 0; i < doors.size(); i++) {
            InteractionSystem.syncDoor(world, doors.get(i), world.get(doors.get(i), Door.class));
        }
        EntitySet chests = world.entities(Family.all(Container.class, SpriteRef.class));
        for (int i = 0; i < chests.size(); i++) {
            if (world.get(chests.get(i), Container.class).open) {
                world.get(chests.get(i), SpriteRef.class).frame = 1;
            }
        }
    }

    /** Один тик логики. */
    public void tick() {
        input.beginTick();
        world.update(TICK);
        state.advance(TICK);
        Health h = world.get(player, Health.class);
        state.player().hp = h.hp;
        state.player().shield = h.shield;
    }

    public World world() {
        return world;
    }

    public LevelState state() {
        return state;
    }

    public int player() {
        return player;
    }

    public Health health() {
        return world.get(player, Health.class);
    }

    public GridPos playerPos() {
        return world.get(player, GridPos.class);
    }

    public boolean isPlayerDead() {
        return health().isDead();
    }

    public LevelOutcome outcome() {
        return state.outcome();
    }

    public void dispose() {
        world.dispose();
    }
}
