package com.zaig100.dargon.level;

import static com.zaig100.dargon.level.TestData.errors;
import static com.zaig100.dargon.level.TestData.level;
import static com.zaig100.dargon.level.TestData.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonValue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LevelParserTest {

    private static final List<String> MAP = List.of("....", ".##.", "....");

    @Test
    void parsesTilesSpawnAndFlags() {
        LevelDefinition level =
                parse(
                        "{ \"formatVersion\": 1, \"name\": \"@n\", \"dark\": true, \"spawn\": [2, 2], \"tiles\": [\"..#\", \".##\", \"...\"], \"entities\": [ { \"prefab\": \"stairs\", \"at\": [0, 0] } ] }");
        assertEquals(3, level.tiles().width());
        assertEquals(3, level.tiles().height());
        assertTrue(level.tiles().isFloor(0, 0));
        assertFalse(level.tiles().isFloor(2, 0));
        assertEquals(new GridPoint(2, 2), level.spawn());
        assertTrue(level.dark());
        assertFalse(level.checkpoint());
        assertEquals("@n", level.name());
    }

    @Test
    void customLegend() {
        LevelDefinition level =
                parse(
                        "{ \"formatVersion\": 1, \"name\": \"n\", \"legend\": { \"o\": \"floor\", \"X\": \"wall\" }, \"spawn\": [0, 0], \"tiles\": [\"oX\"] }");
        assertTrue(level.tiles().isFloor(0, 0));
        assertFalse(level.tiles().isFloor(1, 0));
    }

    @Test
    void manualWallEdges() {
        LevelDefinition level =
                parse(
                        "{ \"formatVersion\": 1, \"name\": \"n\", \"spawn\": [0, 0], \"tiles\": [\".#\", \"##\"], \"edges\": [\".8\", \"#.\"] }");
        WallEdge[] edges = level.edges();
        assertNull(edges[0]);
        assertEquals(WallEdge.LEFT_UP_DOWN, edges[1]);
        assertEquals(WallEdge.NONE, edges[2]);
        assertNull(edges[3]);
        assertNull(parse(level("[0, 0]", MAP)).edges());
        assertTrue(
                errors(
                                "{ \"formatVersion\": 1, \"name\": \"n\", \"spawn\": [0, 0], \"tiles\": [\".#\"], \"edges\": [\".x\"] }")
                        .contains("edges[0]: ошибка: символ 'x'"));
        assertTrue(
                errors(
                                "{ \"formatVersion\": 1, \"name\": \"n\", \"spawn\": [0, 0], \"tiles\": [\".#\"], \"edges\": [\".\"] }")
                        .contains("edges[0]: ошибка: ожидалась строка длиной 2"));
    }

    @Test
    void fillsDefaultsAndSubstitutesParamsEverywhere() {
        LevelDefinition level =
                parse(
                        level(
                                "[0, 0]",
                                MAP,
                                "{ \"prefab\": \"flamethrower\", \"id\": \"f1\", \"at\": [1, 1], \"length\": 5 }"));
        EntitySpec flame = level.entity("f1");
        assertEquals("right", flame.param("dir").asString());
        assertEquals(5, flame.param("length").asInt());
        assertEquals(2.0, flame.param("period").asDouble());
        JsonValue emitter = flame.components().get("Emitter");
        assertEquals("fire", emitter.getString("kind"));
        assertEquals("right", emitter.getString("dir"));
        assertEquals(5, emitter.get("shape").get(0).asInt());
        assertEquals(1, emitter.get("shape").get(1).asInt());
        assertEquals(2.0, emitter.get("timing").getDouble("period"));
        // Копии независимы: изменение одной не влияет на следующую.
        emitter.get("shape").get(0).set(99, null);
        assertEquals(5, flame.components().get("Emitter").get("shape").get(0).asInt());
    }

    @Test
    void optionalParamWithNullDefault() {
        EntitySpec stairs =
                parse(level("[0, 0]", MAP, "{ \"prefab\": \"stairs\", \"at\": [3, 0] }"))
                        .entities()
                        .get(0);
        assertTrue(stairs.param("next").isNull());
        assertTrue(JsonValues.get(stairs.components().get("LevelExit"), "next").isNull());
    }

    @Test
    void idsTagsEnabledAndHandlers() {
        LevelDefinition level =
                parse(
                        level(
                                "[0, 0]",
                                MAP,
                                "{ \"prefab\": \"button\", \"id\": \"b1\", \"at\": [1, 0], \"tags\": [\"red\", \"red\", \"big\"], \"enabled\": false, \"onUse\": [\"a = 1\", \"b = 2\"] }"));
        EntitySpec button = level.entity("b1");
        assertEquals(List.of("red", "big"), button.tags());
        assertFalse(button.enabled());
        assertEquals(Map.of("use", "a = 1\nb = 2"), button.handlers());
        assertNull(level.entity("nope"));
    }

    @Test
    void errorsHavePathsAndAreAllReported() {
        String message =
                errors(
                        level(
                                "[0, 0]",
                                MAP,
                                "{ \"prefab\": \"flamethrower\", \"at\": [0, 0], \"lenght\": 2 }",
                                "{ \"prefab\": \"flamethrower\", \"at\": [0, 0], \"dir\": \"up_left\" }",
                                "{ \"prefab\": \"flamethrower\", \"at\": [0, 0], \"length\": 10 }",
                                "{ \"prefab\": \"flamethrower\", \"at\": [0, 0], \"period\": \"fast\" }",
                                "{ \"prefab\": \"teleport\", \"at\": [0, 0] }",
                                "{ \"prefab\": \"flamethrowr\", \"at\": [0, 0] }"));
        assertTrue(
                message.contains(
                        "entities[0].lenght: ошибка: у префаба 'flamethrower' нет параметра 'lenght', возможно, имелось в виду 'length'"),
                message);
        assertTrue(
                message.contains(
                        "entities[1].dir: ошибка: ожидалось направление (right, down, left, up)"),
                message);
        assertTrue(
                message.contains("entities[2].length: ошибка: значение 10 больше максимума 9"),
                message);
        assertTrue(
                message.contains(
                        "entities[3].period: ошибка: ожидалось число, получено строка \"fast\""),
                message);
        assertTrue(
                message.contains("entities[4]: ошибка: не задан обязательный параметр 'target'"),
                message);
        assertTrue(
                message.contains(
                        "entities[5].prefab: ошибка: неизвестный префаб 'flamethrowr', возможно, имелось в виду 'flamethrower'"),
                message);
    }

    @Test
    void duplicateIdsAndUnknownEvents() {
        String message =
                errors(
                        level(
                                "[0, 0]",
                                MAP,
                                "{ \"prefab\": \"stairs\", \"id\": \"a\", \"at\": [0, 0] }",
                                "{ \"prefab\": \"stairs\", \"id\": \"a\", \"at\": [1, 0] }",
                                "{ \"prefab\": \"stairs\", \"at\": [2, 0], \"onUse\": \"x = 1\" }"));
        assertTrue(
                message.contains("entities[1].id: ошибка: id 'a' уже занят сущностью entities[0]"),
                message);
        assertTrue(
                message.contains(
                        "entities[2].onUse: ошибка: префаб 'stairs' не порождает событие 'use'"),
                message);
    }

    @Test
    void keysAreCaseSensitive() {
        String message = errors(level("[0, 0]", MAP, "{ \"prefab\": \"stairs\", \"At\": [0, 0] }"));
        assertTrue(message.contains("entities[0]: ошибка: нет поля 'at'"), message);
        assertTrue(
                message.contains(
                        "entities[0].At: ошибка: у префаба 'stairs' нет параметра 'At', возможно, имелось в виду 'at'"),
                message);
    }

    @Test
    void mapErrors() {
        assertTrue(
                errors(level("[0, 0]", List.of("...", "..")))
                        .contains("tiles[1]: ошибка: длина строки 2, а у первой строки 3"));
        assertTrue(
                errors(level("[0, 0]", List.of("..x")))
                        .contains("tiles[0]: ошибка: символ 'x' (столбец 2) не описан в легенде"));
        assertTrue(
                errors(level("[1, 1]", MAP))
                        .contains("spawn: ошибка: точка появления [1, 1] не на полу"));
        assertTrue(errors(level("[0]", MAP)).contains("spawn: ошибка: ожидалась клетка [x, y]"));
    }

    @Test
    void topLevelErrors() {
        assertTrue(
                errors(
                                "{ \"formatVersion\": 2, \"name\": \"n\", \"spawn\": [0, 0], \"tiles\": [\".\"] }")
                        .contains("formatVersion: ошибка: поддерживается только версия 1"));
        assertTrue(
                errors("{ \"name\": \"n\", \"spawn\": [0, 0], \"tiles\": [\".\"], \"dakr\": true }")
                        .contains(
                                "dakr: ошибка: неизвестное поле, возможно, имелось в виду 'dark'"));
        assertTrue(errors("{ \"formatVersion\": 1, ").contains("некорректный JSON"));
    }

    @Test
    void itemStacks() {
        LevelDefinition level =
                parse(
                        level(
                                "[0, 0]",
                                MAP,
                                "{ \"prefab\": \"item\", \"at\": [0, 0], \"item\": { \"id\": \"key\", \"key\": \"gold\" } }"));
        assertEquals("gold", level.entities().get(0).param("item").getString("key"));
        String message =
                errors(
                        level(
                                "[0, 0]",
                                MAP,
                                "{ \"prefab\": \"item\", \"at\": [0, 0], \"item\": { \"id\": \"coin\" } }",
                                "{ \"prefab\": \"item\", \"at\": [0, 0], \"item\": { \"id\": \"key\", \"count\": 2, \"colour\": \"#FF0000\" } }",
                                "{ \"prefab\": \"item\", \"at\": [0, 0], \"item\": { \"id\": \"key\", \"key\": \"k\", \"color\": \"red\" } }"));
        assertTrue(
                message.contains(
                        "entities[0].item.id: ошибка: неизвестный предмет 'coin', возможно, имелось в виду 'coins'"),
                message);
        assertTrue(
                message.contains(
                        "entities[1].item.count: ошибка: количество должно быть целым от 1 до 1"),
                message);
        assertTrue(
                message.contains(
                        "entities[1].item.colour: ошибка: у предмета 'key' нет поля 'colour', возможно, имелось в виду 'color'"),
                message);
        assertTrue(
                message.contains(
                        "entities[1].item: ошибка: у предмета 'key' обязательно поле 'key'"),
                message);
        assertTrue(
                message.contains("entities[2].item.color: ошибка: ожидался цвет \"#RRGGBB\""),
                message);
    }

    @Test
    void scriptPathIsRelativeToLevelFile() {
        LevelDefinition level =
                new LevelParser(TestData.prefabs())
                        .parse(
                                "{ \"formatVersion\": 1, \"name\": \"n\", \"spawn\": [0, 0], \"tiles\": [\".\"], \"script\": \"s.dgs\" }",
                                "shops/shop_05");
        assertEquals("shops/s.dgs", level.scriptPath());
    }
}
