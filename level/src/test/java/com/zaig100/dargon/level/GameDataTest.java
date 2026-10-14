package com.zaig100.dargon.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Проверяет настоящие данные игры из каталога assets. */
class GameDataTest {

    static GameData load() {
        Path assets = Path.of(System.getProperty("dargon.assets", "../assets"));
        AssetReader reader =
                path -> {
                    Path file = assets.resolve(path);
                    try {
                        return Files.isRegularFile(file)
                                ? Files.readString(file, StandardCharsets.UTF_8)
                                : null;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                };
        return GameData.loadBuiltin(reader);
    }

    @Test
    void allLevelsLoad() {
        GameData data = load();
        assertTrue(data.levels().size() >= 32, "уровней: " + data.levels().size());
        assertEquals("01", data.start());
        for (String id : data.levels()) {
            LevelDefinition level = data.loadLevel(id);
            assertNotNull(level, id);
            assertFalse(level.entities().isEmpty(), id);
        }
    }

    @Test
    void migratedLevelKeepsLegacyLayout() {
        // Уровень 05 DarGon3: игрок появлялся в правом верхнем углу, лестница дальше — в правом
        // нижнем.
        LevelDefinition level = load().loadLevel("05");
        assertEquals(11, level.tiles().width());
        assertEquals(10, level.tiles().height());
        assertEquals(new GridPoint(9, 0), level.spawn());
        assertTrue(level.dark());
        assertTrue(level.checkpoint());
        EntitySpec next =
                level.entities().stream()
                        .filter(
                                e ->
                                        e.prefab().id().equals("stairs")
                                                && "06".equals(e.param("next").asString()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(new GridPoint(10, 9), next.at());
    }

    @Test
    void schemaIsDeterministic() {
        GameData data = load();
        assertEquals(
                JsonPrinter.write(LevelSchema.build(data)),
                JsonPrinter.write(LevelSchema.build(data)));
    }

    @Test
    void everyGamePrefabMatchesComponentCatalog() {
        GameData data = load();
        for (Prefab prefab : data.prefabs().all()) {
            Problems problems = new Problems(prefab.id());
            data.components().validate(prefab, prefab.id(), problems, data.items());
            assertTrue(problems.list().isEmpty(), prefab.id() + ": " + problems.list());
        }
    }
}
