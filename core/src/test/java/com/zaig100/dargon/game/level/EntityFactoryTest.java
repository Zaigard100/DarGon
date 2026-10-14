package com.zaig100.dargon.game.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.ecs.Component;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.ecs.WorldBuilder;
import com.zaig100.dargon.level.Direction;
import com.zaig100.dargon.level.GridPoint;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.level.LevelFormatException;
import com.zaig100.dargon.level.LevelParser;
import com.zaig100.dargon.level.PrefabLibrary;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EntityFactoryTest {

    static final class Emitter implements Component {
        Direction dir;
        float period;
        int length;

        @Override
        public void reset() {
            dir = null;
            period = 0;
            length = 0;
        }
    }

    static final class Blocking implements Component {
        @Override
        public void reset() {}
    }

    static final class Position implements Component {
        GridPoint at;

        @Override
        public void reset() {
            at = null;
        }
    }

    private static final String PREFAB =
            """
            { "id": "flamethrower",
              "params": { "dir": { "type": "direction", "default": "right" },
                          "period": { "type": "number", "default": 2.0 },
                          "length": { "type": "int", "default": 3 } },
              "components": { "Emitter": { "dir": "$dir", "period": "$period", "length": "$length" }, "Blocking": {} } }
            """;

    private static World world() {
        return new WorldBuilder()
                .component(Emitter.class, Emitter::new)
                .component(Blocking.class, Blocking::new)
                .component(Position.class, Position::new)
                .build();
    }

    private static LevelDefinition level(PrefabLibrary prefabs, String entity) {
        return new LevelParser(prefabs)
                .parse(
                        "{ \"formatVersion\": 1, \"name\": \"n\", \"spawn\": [0, 0], \"tiles\": [\"...\"], \"entities\": ["
                                + entity
                                + "] }",
                        "test");
    }

    private static PrefabLibrary prefabs() {
        PrefabLibrary prefabs = new PrefabLibrary(ItemCatalog.empty());
        prefabs.add(PREFAB, "flamethrower.json");
        return prefabs;
    }

    private static ComponentRegistry registry() {
        return new ComponentRegistry()
                .register(
                        "Emitter",
                        Emitter.class,
                        (c, data, spec) -> {
                            c.dir = data.getDirection("dir");
                            c.period = data.getFloat("period");
                            c.length = data.getInt("length");
                        })
                .register("Blocking", Blocking.class);
    }

    @Test
    void spawnsComponentsFromPrefabWithParamsAndDecorators() {
        World world = world();
        EntityFactory factory =
                new EntityFactory(registry())
                        .decorator(
                                (w, entity, spec) -> w.add(entity, Position.class).at = spec.at());
        int[] entities =
                factory.spawnAll(
                        world,
                        level(
                                prefabs(),
                                "{ \"prefab\": \"flamethrower\", \"at\": [2, 0], \"dir\": \"up\", \"length\": 1 }"));
        assertEquals(1, entities.length);
        Emitter emitter = world.get(entities[0], Emitter.class);
        assertEquals(Direction.UP, emitter.dir);
        assertEquals(2.0f, emitter.period);
        assertEquals(1, emitter.length);
        assertTrue(world.has(entities[0], Blocking.class));
        assertEquals(new GridPoint(2, 0), world.get(entities[0], Position.class).at);
    }

    @Test
    void unregisteredComponentIsReportedAndEntityRemoved() {
        World world = world();
        EntityFactory factory =
                new EntityFactory(new ComponentRegistry().register("Emitter", Emitter.class));
        LevelDefinition level =
                level(prefabs(), "{ \"prefab\": \"flamethrower\", \"at\": [2, 0] }");
        LevelFormatException e =
                assertThrows(LevelFormatException.class, () -> factory.spawnAll(world, level));
        assertEquals(
                "test: entities[0].components.Blocking: ошибка: компонент 'Blocking' из префаба 'flamethrower' не зарегистрирован в игре",
                e.getMessage());
        assertEquals(0, world.entityCount());
        assertEquals(
                Set.of("Blocking"),
                new ComponentRegistry().register("Emitter", Emitter.class).missing(prefabs()));
    }

    @Test
    void badComponentDataHasPath() {
        ComponentRegistry registry =
                new ComponentRegistry()
                        .register(
                                "Emitter",
                                Emitter.class,
                                (c, data, spec) -> c.length = data.getInt("missing"))
                        .register("Blocking", Blocking.class);
        World world = world();
        LevelFormatException e =
                assertThrows(
                        LevelFormatException.class,
                        () ->
                                new EntityFactory(registry)
                                        .spawnAll(
                                                world,
                                                level(
                                                        prefabs(),
                                                        "{ \"prefab\": \"flamethrower\", \"at\": [2, 0] }")));
        assertEquals(
                "test: entities[0].components.Emitter.missing: ошибка: нет обязательного поля",
                e.getMessage());
        assertFalse(world.isAlive(0));
    }
}
