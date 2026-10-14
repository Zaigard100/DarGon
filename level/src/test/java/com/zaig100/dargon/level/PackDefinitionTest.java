package com.zaig100.dargon.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PackDefinitionTest {

    private static final String ITEMS =
            "{ \"items\": { \"potion\": { \"icon\": \"hp_potion\", \"effect\": \"heal\" } } }";

    private static String pack(String extra) {
        return "{ \"format\": 1, \"id\": \"my_pack\", \"name\": \"Мой пак\", \"author\": \"я\","
                + " \"version\": \"1.2\", \"game\": \"4.0.0\", \"icon\": \"icon.png\","
                + " \"start\": \"a\", \"levels\": [\"a\", \"b\"]"
                + extra
                + " }";
    }

    @Test
    void headerAndLevels() {
        PackDefinition pack =
                PackDefinition.parse(pack(""), "pack.json", ItemCatalog.parse(ITEMS, "items.json"));
        assertEquals("my_pack", pack.id());
        assertEquals(java.util.List.of("a", "b"), pack.levels());
        PackDefinition.Info info = pack.info();
        assertEquals("я", info.author());
        assertEquals("1.2", info.version());
        assertEquals("icon.png", info.icon());
        assertNull(info.check("4.0.0-beta1"));
    }

    @Test
    void compatibility() {
        PackDefinition.Info newer =
                PackDefinition.header(pack("").replace("\"4.0.0\"", "\"4.2\""), "pack.json");
        assertEquals(PackDefinition.Incompatibility.NEWER_GAME, newer.check("4.1.9"));
        assertNull(newer.check("4.2.0"));
        PackDefinition.Info future =
                PackDefinition.header(
                        pack("").replace("\"format\": 1", "\"format\": 2"), "pack.json");
        assertEquals(PackDefinition.Incompatibility.NEWER_FORMAT, future.check("9.0"));
        assertTrue(PackDefinition.compareVersions("4.0.0-beta1", "4.0") == 0);
        assertTrue(PackDefinition.compareVersions("4.10", "4.9") > 0);
    }

    @Test
    void errorsAreReported() {
        ItemCatalog items = ItemCatalog.parse(ITEMS, "items.json");
        String message =
                assertThrows(
                                LevelFormatException.class,
                                () ->
                                        PackDefinition.parse(
                                                pack("").replace(
                                                                "\"start\": \"a\"",
                                                                "\"start\": \"x\""),
                                                "pack.json",
                                                items))
                        .getMessage();
        assertTrue(message.contains("уровня 'x' нет в списке levels"), message);
        assertTrue(
                assertThrows(
                                LevelFormatException.class,
                                () ->
                                        PackDefinition.parse(
                                                pack("").replace("\"format\": 1, ", ""),
                                                "pack.json",
                                                items))
                        .getMessage()
                        .contains("format"));
    }
}
