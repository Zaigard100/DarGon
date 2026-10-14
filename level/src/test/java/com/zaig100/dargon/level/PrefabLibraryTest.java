package com.zaig100.dargon.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PrefabLibraryTest {

    private static String error(String source, String file) {
        PrefabLibrary library = new PrefabLibrary(ItemCatalog.empty());
        return assertThrows(LevelFormatException.class, () -> library.add(source, file))
                .getMessage();
    }

    @Test
    void parsesPrefab() {
        Prefab prefab = TestData.prefabs().get("flamethrower");
        assertEquals(Placement.WALL, prefab.placement());
        assertEquals(3, prefab.params().size());
        assertTrue(prefab.hasComponent("Emitter"));
        assertTrue(prefab.params().get("dir").defaultValue().isString());
        assertEquals("onUse", Prefab.handlerKey("use"));
    }

    @Test
    void reportsBrokenPrefabs() {
        assertTrue(
                error(
                                "{ \"id\": \"a\", \"components\": { \"X\": { \"v\": \"$speed\" } }, \"params\": { \"sped\": { \"type\": \"number\" } } }",
                                "a.json")
                        .contains(
                                "components.X.v: ошибка: ссылка на необъявленный параметр 'speed', возможно, имелось в виду 'sped'"));
        assertTrue(
                error("{ \"id\": \"a\", \"components\": {} }", "a.json")
                        .contains("у префаба должен быть хотя бы один компонент"));
        assertTrue(
                error("{ \"id\": \"a\", \"components\": { \"X\": {} } }", "prefabs/b.json")
                        .contains("id 'a' не совпадает с именем файла b.json"));
        assertTrue(
                error(
                                "{ \"id\": \"a\", \"params\": { \"p\": { \"type\": \"float\" } }, \"components\": { \"X\": { \"p\": \"$p\" } } }",
                                "a.json")
                        .contains("params.p.type: ошибка: неизвестный тип параметра 'float'"));
        assertTrue(
                error(
                                "{ \"id\": \"a\", \"params\": { \"n\": { \"type\": \"int\", \"default\": 1.5 } }, \"components\": { \"X\": { \"n\": \"$n\" } } }",
                                "a.json")
                        .contains("params.n.default: ошибка: ожидалось целое число"));
        assertTrue(
                error(
                                "{ \"id\": \"a\", \"params\": { \"at\": { \"type\": \"int\" } }, \"components\": { \"X\": { \"n\": \"$at\" } } }",
                                "a.json")
                        .contains("params.at: ошибка: имя параметра зарезервировано"));
    }

    @Test
    void duplicatePrefab() {
        PrefabLibrary library = TestData.prefabs();
        assertThrows(LevelFormatException.class, () -> library.add(TestData.BUTTON, "button.json"));
    }
}
