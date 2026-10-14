package com.zaig100.dargon.tools.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.zaig100.dargon.level.JsonPrinter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegacyConverterTest {

    /** Карта 4×3 в старом формате: строка 0 массива — верх экрана (старый y = 2). */
    private static final String MAP =
            "\"map\": [[11, 11, 11, 11], [11, 10, 10, 11], [11, 11, 11, 11]]";

    private final Report report = new Report();
    private final TextBundle texts = new TextBundle();

    private LegacyConverter.Result convert(String objects) {
        String json =
                "{ \"LevelName\": \"Dangerouse\", \"Spawn\": [0, 2], \"isDark\": true, "
                        + MAP
                        + (objects.isEmpty() ? "" : ", " + objects)
                        + " }";
        return new LegacyConverter(
                        new JsonReader().parse(json),
                        "07",
                        p -> p.equals("levels/08.json") ? "08" : null,
                        report,
                        texts)
                .convert();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entities(LegacyConverter.Result result) {
        return (List<Map<String, Object>>) result.level().get("entities");
    }

    @Test
    void flipsYAndConvertsTilesAndTexts() {
        LegacyConverter.Result result =
                convert(
                        "\"Stair\": [{ \"X\": 3, \"Y\": 0, \"FlipX\": true, \"Next\": \"levels/08.json\", \"End\": false }]");
        Map<String, Object> level = result.level();
        assertEquals(List.of(0, 0), level.get("spawn"));
        assertEquals(
                List.of("....", ".##.", "...."),
                ((JsonPrinter.Multiline) level.get("tiles")).items());
        assertEquals(true, level.get("dark"));
        assertFalse(level.containsKey("checkpoint"));
        assertEquals("@level.07.name", level.get("name"));
        assertEquals("Dangerous", texts.english.get("level.07.name"));
        assertEquals("Опасный", texts.russian.get("level.07.name"));
        assertEquals(
                Map.of("prefab", "stairs", "at", List.of(3, 2), "next", "08", "flip", true),
                entities(result).get(0));
        assertNull(result.script());
    }

    @Test
    void convertsHazardsAndDirections() {
        List<Map<String, Object>> entities =
                entities(
                        convert(
                                "\"Flamefrower\": [{ \"X\": 1, \"Y\": -1, \"Stage\": 1, \"Max\": 3, \"Rot\": 1, \"Tick\": 2.0 }],"
                                        + "\"Crossbow\": [{ \"X\": 4, \"Y\": 1, \"DX\": -1, \"DY\": 1, \"Angle\": 315, \"Tick\": 3 }],"
                                        + "\"FlimsyTile\": [{ \"X\": 0, \"Y\": 1, \"Stage\": 2 }],"
                                        + "\"Spike\": [{ \"X\": 2, \"Y\": 2, \"Active\": true, \"Tick\": 1.5 }]"));
        assertEquals(
                Map.of(
                        "prefab",
                        "flamethrower",
                        "at",
                        List.of(1, 3),
                        "dir",
                        "up",
                        "length",
                        3,
                        "stage",
                        1),
                entities.get(1));
        assertEquals(
                Map.of("prefab", "crossbow", "at", List.of(4, 1), "dir", "up_left", "period", 3.0),
                entities.get(2));
        assertEquals(
                Map.of("prefab", "fragile_floor", "at", List.of(0, 1), "cracks", 2),
                entities.get(0));
        assertEquals(
                Map.of("prefab", "spikes", "at", List.of(2, 0), "active", true), entities.get(3));
    }

    @Test
    void itemsKeysAndShops() {
        List<Map<String, Object>> entities =
                entities(
                        convert(
                                "\"Item\": [{ \"X\": 0, \"Y\": 0, \"Active\": true, \"Item\": { \"Type\": \"key\", \"KeyTag\": \"k1\", \"R\": 1.0, \"G\": 0.5, \"B\": 0.0 } },"
                                        + "{ \"X\": 1, \"Y\": 0, \"Active\": true, \"Item\": { \"Type\": \"money\", \"Count\": 15 } },"
                                        + "{ \"X\": 2, \"Y\": 0, \"Active\": false, \"Item\": { \"Type\": \"potion\", \"Count\": 1 } }],"
                                        + "\"Shop\": [{ \"X\": 0, \"Y\": 2, \"Slots\": [{ \"Item\": { \"Type\": \"sheld\", \"Count\": 1 }, \"Cost\": 10 }] }]"));
        assertEquals(
                Map.of("id", "key", "key", "k1", "color", "#FF8000"), entities.get(0).get("item"));
        assertEquals(Map.of("id", "coins", "count", 15), entities.get(1).get("item"));
        assertEquals(3, entities.size());
        assertEquals(
                List.of(Map.of("item", Map.of("id", "shield"), "price", 10)),
                entities.get(2).get("offers"));
    }

    @Test
    void zoneKeepsLegacyInclusiveCoverageClippedToMap() {
        Map<String, Object> zone =
                entities(
                                convert(
                                        "\"Zona\": [{ \"X\": 0, \"Y\": 0, \"Wight\": 2, \"Height\": 5, \"Type\": \"hp+\", \"Tick\": 30.0 }]"))
                        .get(0);
        assertEquals(List.of(0, 0), zone.get("at"));
        assertEquals(3, zone.get("width"));
        assertEquals(3, zone.get("height"));
        assertEquals("heal", zone.get("effect"));
    }

    @Test
    void parkedObjectsBecomeDisabledAndButtonsBecomeScript() {
        LegacyConverter.Result result =
                convert(
                        "\"Stair\": [{ \"X\": 15, \"Y\": 15, \"FlipX\": false, \"Next\": \"levels/08.json\", \"End\": false, \"Tag\": \"exit\" }],"
                                + "\"Button\": [{ \"X\": 0, \"Y\": 0, \"Tag\": \"b\", \"Function\": [\"exit:X>3\", \"exit:Y>2\", \"exit:cordN\", \"b:X>15\", \"b:cordN\", \"Map:map-1,1>11\", \"ghost:del\"] }]");
        List<Map<String, Object>> entities = entities(result);
        Map<String, Object> stairs = entities.get(0);
        assertEquals("exit", stairs.get("id"));
        assertEquals(List.of(3, 0), stairs.get("at"));
        assertEquals(false, stairs.get("enabled"));
        Map<String, Object> button = entities.get(1);
        assertEquals(
                List.of("entity('exit').enable()", "self.disable()", "setTile(1, 1, 'floor')"),
                button.get("onUse"));
        assertTrue(report.toMarkdown().contains("несуществующий объект `ghost`"));
    }

    @Test
    void longHandlersMoveToScriptFileWithoutSelf() {
        StringBuilder functions = new StringBuilder("\"b:X>15\"");
        for (int i = 0; i < 6; i++) {
            functions.append(", \"Map:map-").append(i % 4).append(",0>10\"");
        }
        LegacyConverter.Result result =
                convert(
                        "\"Button\": [{ \"X\": 0, \"Y\": 0, \"Tag\": \"b\", \"Function\": ["
                                + functions
                                + "] }]");
        assertEquals("bPressed()", entities(result).get(0).get("onUse"));
        assertEquals("07.dgs", result.level().get("script"));
        assertTrue(
                result.script()
                        .contains(
                                "def bPressed() {\n    entity(\"b\").disable()\n    setTile(0, 2, \"wall\")"),
                result.script());
    }

    @Test
    void ignoredLegacyFieldsAreReported() {
        convert("\"Win\": 5");
        assertTrue(report.toMarkdown().contains("`Win` DarGon3 не читал"));
    }

    @Test
    void misspelledHideTrapsBecomeHiddenTraps() {
        LegacyConverter.Result result =
                convert(
                        "\"HideTrap\": [{ \"X\": 1, \"Y\": 0, \"Active\": true }], "
                                + "\"HideTraps\": [{ \"X\": 0, \"Y\": 0, \"Active\": true }]");
        assertEquals(
                List.of(
                        Map.of("prefab", "hidden_trap", "at", List.of(1, 2)),
                        Map.of("prefab", "hidden_trap", "at", List.of(0, 2))),
                entities(result));
        assertFalse(report.toMarkdown().contains("не читал — отброшено"));
    }
}
