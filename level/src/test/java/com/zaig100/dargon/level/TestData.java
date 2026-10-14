package com.zaig100.dargon.level;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;

/** Небольшой набор префабов и предметов для тестов (не зависит от assets). */
final class TestData {

    static final String ITEMS =
            """
            { "items": {
                "coins": { "icon": "money", "effect": "coins", "maxStack": 999 },
                "key": { "icon": "key", "effect": "key", "maxStack": 1,
                         "fields": { "key": { "type": "string" },
                                     "color": { "type": "color", "default": "#FFFFFF" } } }
            } }
            """;

    static final String STAIRS =
            """
            { "id": "stairs",
              "params": { "next": { "type": "level", "default": null } },
              "components": { "LevelExit": { "next": "$next" } } }
            """;

    static final String TELEPORT =
            """
            { "id": "teleport",
              "params": { "target": { "type": "point" } },
              "components": { "Teleporter": { "target": "$target" } } }
            """;

    static final String FLAMETHROWER =
            """
            { "id": "flamethrower", "placement": "wall",
              "params": {
                "dir": { "type": "direction", "default": "right" },
                "length": { "type": "int", "default": 3, "min": 0, "max": 9 },
                "period": { "type": "number", "default": 2.0 }
              },
              "components": {
                "Emitter": { "kind": "fire", "dir": "$dir", "shape": ["$length", 1], "timing": { "period": "$period" } }
              } }
            """;

    static final String BUTTON =
            """
            { "id": "button", "events": ["use"], "components": { "ScriptHandlers": {} } }
            """;

    static final String WALL =
            """
            { "id": "wall_block", "components": { "Blocking": {} } }
            """;

    static final String DOOR =
            """
            { "id": "door", "params": { "key": { "type": "string" } },
              "components": { "Blocking": {}, "Lock": { "key": "$key" } } }
            """;

    static final String ITEM =
            """
            { "id": "item", "params": { "item": { "type": "item" } }, "components": { "Pickup": { "item": "$item" } } }
            """;

    static final String TABLET =
            """
            { "id": "tablet", "placement": "any", "params": { "text": { "type": "text" } },
              "components": { "Text": { "text": "$text" } } }
            """;

    private TestData() {}

    static PrefabLibrary prefabs() {
        PrefabLibrary library = new PrefabLibrary(ItemCatalog.parse(ITEMS, "items.json"));
        for (String source :
                List.of(STAIRS, TELEPORT, FLAMETHROWER, BUTTON, WALL, DOOR, ITEM, TABLET)) {
            library.add(source, "prefab");
        }
        return library;
    }

    static LevelDefinition parse(String json) {
        return new LevelParser(prefabs()).parse(json, "test");
    }

    /** Уровень из карты и сущностей (JSON-строки через запятую). */
    static String level(String spawn, List<String> rows, String... entities) {
        String tiles = rows.stream().map(r -> "\"" + r + "\"").collect(Collectors.joining(", "));
        return "{ \"formatVersion\": 1, \"name\": \"@test\", \"spawn\": "
                + spawn
                + ", \"tiles\": ["
                + tiles
                + "], \"entities\": ["
                + String.join(", ", entities)
                + "] }";
    }

    /** Сообщения ошибок разбора (по одному на строку). */
    static String errors(String json) {
        return assertThrows(LevelFormatException.class, () -> parse(json)).getMessage();
    }
}
