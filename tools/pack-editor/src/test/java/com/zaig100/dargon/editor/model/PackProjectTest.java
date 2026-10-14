package com.zaig100.dargon.editor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.level.GameData;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackProjectTest {

    private static final Path ASSETS = Path.of(System.getProperty("dargon.assets", "../../assets"));

    @TempDir Path dir;

    private Path copyBuiltin() throws IOException {
        Path source = ASSETS.resolve("packs/dargon");
        try (Stream<Path> files = Files.walk(source)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                Path target = dir.resolve(source.relativize(f).toString());
                if (Files.isDirectory(f)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(f, target);
                }
            }
        }
        return source;
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void openAndSaveKeepsLevelsAndTextsByteForByte() throws IOException {
        Path source = copyBuiltin();
        PackProject project = PackProject.open(dir);
        for (String id : project.levelIds()) {
            project.level(id).changed();
        }
        project.packChanged();
        project.save();
        try (Stream<Path> files = Files.walk(source)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                if (!Files.isRegularFile(f) || f.toString().endsWith(".png")) {
                    continue;
                }
                String name = source.relativize(f).toString();
                if (name.equals("pack.json")) {
                    // Оформлен вручную: после сохранения — тот же JSON в машинном оформлении.
                    assertEquals(Json.parse(read(f)), Json.parse(read(dir.resolve(name))));
                } else {
                    assertEquals(read(f), read(dir.resolve(name)), name);
                }
            }
        }
    }

    @Test
    void newPackLoadsInGame() throws IOException {
        PackProject project = PackProject.create(dir.resolve("my"), "my_pack", "Мой пак");
        LevelDoc level = project.level("01");
        level.checkpoint();
        assertTrue(level.setFloor(1, 1, false));
        level.addEntity("stairs", 6, 4);
        level.changed();
        project.addLevel("02", "01");
        project.level("01").entities().get(0).put("next", "02");
        project.save();
        AssetReader engine =
                path -> {
                    Path file = ASSETS.resolve(path);
                    return Files.isRegularFile(file) ? read(file) : null;
                };
        GameData data = GameData.load(engine, project.reader());
        assertEquals(List.of("01", "02"), data.levels());
        assertFalse(data.loadLevel("01").tiles().isFloor(1, 1));

        project.renameLevel("02", "finale", data.prefabs());
        project.save();
        assertEquals("finale", project.level("01").entities().get(0).get("next"));
        assertFalse(Files.exists(dir.resolve("my/levels/02.json")));
        assertTrue(Files.exists(dir.resolve("my/levels/finale.json")));
        assertTrue(level.undo());
    }

    @Test
    void exportedPackOpensInGame() throws IOException {
        copyBuiltin();
        Path target = dir.getParent().resolve(dir.getFileName() + "-export.dgpack");
        PackExporter.export(dir, target);
        com.zaig100.dargon.pack.ZipPack zip = new com.zaig100.dargon.pack.ZipPack(target.toFile());
        AssetReader engine =
                path -> {
                    Path file = ASSETS.resolve(path);
                    return Files.isRegularFile(file) ? read(file) : null;
                };
        GameData data = GameData.load(engine, zip);
        assertEquals("dargon", data.pack().id());
        for (String id : data.levels()) {
            data.loadLevel(id);
        }
        assertTrue(zip.bytes("icon.png").length > 0);
        Files.delete(target);
    }

    @Test
    void customPrefabAndTextureAreSavedAndLoaded() throws IOException {
        PackProject project = PackProject.create(dir.resolve("custom"), "custom", "C");
        Path png = dir.resolve("lava.png");
        Files.write(png, new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        project.importTexture(png, "lava");
        Map<String, Map<String, Object>> prefabs = new java.util.LinkedHashMap<>();
        prefabs.put(
                "lava",
                Json.parseObject(
                        "{ \"id\": \"lava\", \"components\": { \"SpriteRef\": { \"region\": \"lava\" },"
                                + " \"Hazard\": { \"damage\": 3, \"kind\": \"fire\" } } }"));
        project.setPrefabs(prefabs);
        project.level("01").addEntity("lava", 1, 1);
        project.level("01").changed();
        project.save();
        AssetReader engine =
                path -> {
                    Path file = ASSETS.resolve(path);
                    return Files.isRegularFile(file) ? read(file) : null;
                };
        GameData data = GameData.load(engine, PackProject.open(dir.resolve("custom")).reader());
        assertEquals(List.of("lava"), data.packPrefabs());
        assertEquals(List.of("lava"), data.pack().textures());
        data.loadLevel("01");
        assertTrue(Files.exists(dir.resolve("custom/prefabs/lava.json")));

        project.setPrefabs(new java.util.LinkedHashMap<>());
        project.removeTexture("lava");
        project.save();
        assertFalse(Files.exists(dir.resolve("custom/prefabs/lava.json")));
        assertFalse(Files.exists(dir.resolve("custom/textures/lava.png")));
    }

    @Test
    void levelScriptsAreReadEditedAndSavedLikeOtherFiles() throws IOException {
        copyBuiltin();
        PackProject project = PackProject.open(dir);
        assertTrue(project.script("18.dgs").contains("def but1Pressed()"));
        assertEquals(null, project.script("nope.dgs"));
        assertEquals("shops/shop.dgs", PackProject.scriptPath("shops/shop_05", "shop.dgs"));
        assertFalse(project.isDirty());

        project.script("18.dgs", project.script("18.dgs"));
        assertFalse(project.isDirty(), "тот же текст — не правка");
        project.script("new.dgs", "print(1)\n");
        assertTrue(project.isDirty());
        assertEquals(List.of("new.dgs"), project.sharedScripts(), "18.dgs — скрипт уровня");
        assertEquals(
                "print(1)\n", project.reader().read("levels/new.dgs"), "проверка видит правку");
        assertFalse(Files.exists(dir.resolve("levels/new.dgs")));

        project.save();
        assertEquals("print(1)\n", read(dir.resolve("levels/new.dgs")));
        assertFalse(project.isDirty());
    }

    private GameData data(PackProject project) {
        AssetReader engine =
                path -> {
                    Path file = ASSETS.resolve(path);
                    return Files.isRegularFile(file) ? read(file) : null;
                };
        return GameData.load(engine, project.reader());
    }

    @Test
    void levelScriptFollowsLevelToAnotherDirectory() throws IOException {
        copyBuiltin();
        PackProject project = PackProject.open(dir);
        String text = project.script("18.dgs");
        project.renameLevel("18", "shops/18", data(project).prefabs());
        assertEquals(text, project.script("shops/18.dgs"));
        project.save();
        assertFalse(Files.exists(dir.resolve("levels/18.dgs")));
        assertEquals(text, read(dir.resolve("levels/shops/18.dgs")));
        GameData data = data(PackProject.open(dir));
        assertEquals(text, data.readScript(data.loadLevel("shops/18").scriptPath()));
    }

    @Test
    void removedLevelTakesItsScriptUnlessItIsUsedElsewhere() throws IOException {
        copyBuiltin();
        PackProject project = PackProject.open(dir);
        project.script("01.dgs", "use '18'\n");
        project.level("01").root().put("script", "01.dgs");
        project.removeLevel("18");
        project.save();
        assertTrue(Files.exists(dir.resolve("levels/18.dgs")), "подключён через use");

        project.removeLevel("01");
        project.save();
        assertFalse(Files.exists(dir.resolve("levels/01.dgs")));
    }
}
