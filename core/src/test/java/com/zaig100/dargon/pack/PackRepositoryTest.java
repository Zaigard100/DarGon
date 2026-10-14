package com.zaig100.dargon.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.PackDefinition;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackRepositoryTest {

    private static final Path ASSETS = Path.of(System.getProperty("dargon.assets", "../assets"));

    private static final AssetReader ENGINE =
            path -> {
                Path file = ASSETS.resolve(path);
                try {
                    return Files.isRegularFile(file)
                            ? Files.readString(file, StandardCharsets.UTF_8)
                            : null;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };

    @TempDir Path dir;

    private static String packJson(String id, String game) {
        return "{ \"format\": 1, \"id\": \""
                + id
                + "\", \"name\": \"@pack.name\","
                + " \"author\": \"тест\", \"version\": \"0.1\", \"game\": \""
                + game
                + "\","
                + " \"start\": \"one\", \"levels\": [\"one\"] }";
    }

    /** Уровень 01 кампании — заведомо корректный. */
    private static String level() throws IOException {
        return Files.readString(
                ASSETS.resolve("packs/dargon/levels/01.json"), StandardCharsets.UTF_8);
    }

    private void zip(String name, Map<String, String> files) throws IOException {
        try (OutputStream out = Files.newOutputStream(dir.resolve(name));
                ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> f : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(f.getKey()));
                zip.write(f.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    private PackRepository repository() {
        return new PackRepository(
                new FileHandle(ASSETS.resolve("packs").toFile()), new FileHandle(dir.toFile()));
    }

    @Test
    void listsBuiltinZipsAndFolders() throws IOException {
        zip(
                "a.dgpack",
                Map.of(
                        "pack.json", packJson("zipped", "4.0"),
                        "levels/one.json", level(),
                        "i18n/pack_ru.properties", "pack.name = Архив"));
        // Архив с папкой внутри (заархивировали папку пака целиком).
        zip(
                "b.zip",
                Map.of(
                        "inner/pack.json",
                        packJson("nested", "4.0"),
                        "inner/levels/one.json",
                        level()));
        Path folder = Files.createDirectories(dir.resolve("c/levels"));
        Files.writeString(dir.resolve("c/pack.json"), packJson("folder", "4.0"));
        Files.writeString(folder.resolve("one.json"), level());
        Files.writeString(dir.resolve("readme.txt"), "не пак");

        List<PackRepository.Entry> list = repository().list();
        assertEquals(4, list.size());
        assertEquals(GameData.BUILTIN_PACK, list.get(0).id());
        assertTrue(list.get(0).builtin());
        assertEquals(
                List.of("zipped", "nested", "folder"),
                list.subList(1, 4).stream().map(PackRepository.Entry::id).toList());
        assertNotNull(list.get(2).files().read("levels/one.json"), "файлы из вложенной папки");

        for (PackRepository.Entry e : list) {
            GameData data = assertDoesNotThrow(e);
            assertEquals(e.id(), data.pack().id());
        }
    }

    private static GameData assertDoesNotThrow(PackRepository.Entry e) {
        try {
            return PackLoader.load(ENGINE, e, "4.0.0");
        } catch (PackLoader.PackException ex) {
            throw new AssertionError(e.id() + ": " + ex.getMessage(), ex);
        }
    }

    @Test
    void brokenAndDuplicatePacksAreListedWithReason() throws IOException {
        Files.writeString(dir.resolve("broken.dgpack"), "это не zip");
        zip("copy.dgpack", Map.of("pack.json", packJson("dargon", "4.0")));
        zip("nopack.dgpack", Map.of("levels/one.json", "{}"));
        List<PackRepository.Entry> list = repository().list();
        assertEquals(4, list.size());
        for (PackRepository.Entry e : list.subList(1, 4)) {
            assertFalse(e.readable(), e.files().location());
        }
        assertTrue(list.get(2).error().contains("уже занят"), list.get(2).error());
        assertTrue(repository().find("dargon").builtin(), "встроенный не подменяется");
    }

    @Test
    void incompatibleAndBrokenLevelsAreRefusedBeforeSwitching() throws IOException {
        zip(
                "future.dgpack",
                Map.of("pack.json", packJson("future", "99.0"), "levels/one.json", level()));
        zip(
                "bad.dgpack",
                Map.of("pack.json", packJson("bad", "4.0"), "levels/one.json", "{ \"tiles\": 5 }"));
        PackRepository repo = repository();
        PackLoader.PackException future =
                assertThrows(
                        PackLoader.PackException.class,
                        () -> PackLoader.load(ENGINE, repo.find("future"), "4.0.0"));
        assertEquals(PackDefinition.Incompatibility.NEWER_GAME, future.incompatibility());
        PackLoader.PackException bad =
                assertThrows(
                        PackLoader.PackException.class,
                        () -> PackLoader.load(ENGINE, repo.find("bad"), "4.0.0"));
        assertNull(bad.incompatibility());
        assertTrue(bad.getMessage().contains("one"), bad.getMessage());
    }

    @Test
    void zipReadsBinaryFiles() throws IOException {
        zip("icon.dgpack", Map.of("pack.json", packJson("icons", "4.0"), "icon.png", "PNG"));
        File file = dir.resolve("icon.dgpack").toFile();
        assertEquals(3, new ZipPack(file).bytes("icon.png").length);
        assertNull(new ZipPack(file).bytes("missing.png"));
    }

    /** Свой префаб: лава с уроном из параметра и своей картинкой. */
    private static final String LAVA =
            "{ \"id\": \"lava\", \"description\": \"Лава\","
                    + " \"params\": { \"damage\": { \"type\": \"damage\", \"default\": [3, 5] } },"
                    + " \"components\": { \"SpriteRef\": { \"region\": \"lava\" },"
                    + " \"Hazard\": { \"damage\": \"$damage\", \"kind\": \"fire\" } } }";

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};

    private static String lavaPack(String id, String prefab) {
        return "{ \"format\": 1, \"id\": \""
                + id
                + "\", \"name\": \"Лава\", \"start\": \"one\","
                + " \"levels\": [\"one\"], \"prefabs\": [\""
                + prefab
                + "\"], \"textures\": [\"lava\"] }";
    }

    private static String lavaLevel(String prefab) {
        return "{ \"formatVersion\": 1, \"name\": \"L\", \"tiles\": [\"...\"], \"spawn\": [0, 0],"
                + " \"entities\": [ { \"prefab\": \""
                + prefab
                + "\", \"at\": [1, 0] } ] }";
    }

    private void zipBytes(String name, Map<String, Object> files) throws IOException {
        try (OutputStream out = Files.newOutputStream(dir.resolve(name));
                ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, Object> f : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(f.getKey()));
                zip.write(
                        f.getValue() instanceof byte[] b
                                ? b
                                : f.getValue().toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    @Test
    void customPrefabWithTextureLoadsAndHurts() throws IOException, PackLoader.PackException {
        zipBytes(
                "lava.dgpack",
                Map.of(
                        "pack.json",
                        lavaPack("lava_pack", "lava"),
                        "prefabs/lava.json",
                        LAVA,
                        "levels/one.json",
                        lavaLevel("lava"),
                        "textures/lava.png",
                        PNG));
        PackRepository.Entry entry = repository().find("lava_pack");
        GameData data = PackLoader.load(ENGINE, entry, "4.0.0");
        assertEquals(List.of("lava"), data.packPrefabs());
        assertEquals(
                List.of("lava"), List.copyOf(PackLoader.textures(entry.files(), data).keySet()));
        com.zaig100.dargon.game.LevelRuntime level =
                com.zaig100.dargon.game.LevelRuntime.start(
                        data.loadLevel("one"),
                        com.zaig100.dargon.game.PlayerState.newGame(data.items()),
                        new com.zaig100.dargon.input.ActionState());
        int hp = level.health().hp;
        level.world()
                .get(level.player(), com.zaig100.dargon.game.component.GridPos.class)
                .set(1, 0);
        for (int i = 0; i < 10; i++) {
            level.tick();
        }
        assertTrue(level.health().hp < hp, "лава ранит");
    }

    @Test
    void brokenCustomPrefabsAreRefused() throws IOException {
        zipBytes(
                "clash.dgpack",
                Map.of(
                        "pack.json", lavaPack("clash", "spikes"),
                        "prefabs/spikes.json",
                                LAVA.replace(
                                        "\"lava\", \"description", "\"spikes\", \"description"),
                        "levels/one.json", lavaLevel("spikes"),
                        "textures/lava.png", PNG));
        zipBytes(
                "badfield.dgpack",
                Map.of(
                        "pack.json", lavaPack("badfield", "lava"),
                        "prefabs/lava.json", LAVA.replace("\"kind\"", "\"sort\""),
                        "levels/one.json", lavaLevel("lava"),
                        "textures/lava.png", PNG));
        zipBytes(
                "notpng.dgpack",
                Map.of(
                        "pack.json",
                        lavaPack("notpng", "lava"),
                        "prefabs/lava.json",
                        LAVA,
                        "levels/one.json",
                        lavaLevel("lava"),
                        "textures/lava.png",
                        "GIF89a"));
        PackRepository repo = repository();
        assertTrue(message(repo, "clash").contains("уже есть в игре"), message(repo, "clash"));
        assertTrue(message(repo, "badfield").contains("sort"), message(repo, "badfield"));
        assertTrue(message(repo, "notpng").contains("не PNG"), message(repo, "notpng"));
    }

    private static String message(PackRepository repo, String id) {
        return assertThrows(
                        PackLoader.PackException.class,
                        () -> PackLoader.load(ENGINE, repo.find(id), "4.0.0"))
                .getMessage();
    }
}
