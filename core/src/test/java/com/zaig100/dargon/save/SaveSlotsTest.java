package com.zaig100.dargon.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.game.GameMode;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.game.Progress;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.level.GameData;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SaveSlotsTest {

    private static GameData data() {
        Path assets = Path.of(System.getProperty("dargon.assets", "../assets"));
        return GameData.loadBuiltin(
                path -> {
                    Path file = assets.resolve(path);
                    try {
                        return Files.isRegularFile(file)
                                ? Files.readString(file, StandardCharsets.UTF_8)
                                : null;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private static Progress progress(GameData data) {
        PlayerState player = PlayerState.newGame(data.items());
        player.coins = 17;
        player.inventory.add(new ItemStack("key", 1, Map.of("key", "red", "color", "#FF0000")));
        Progress p = Progress.newGame(GameMode.HARDCORE, "dargon", "01", player, 5);
        p.entered("07", false, player, true);
        p.addPlayTime(90.5);
        return p;
    }

    @Test
    void roundTripKeepsEverything() throws SaveException {
        GameData data = data();
        Progress p = progress(data);
        p.run().fromStart = true;
        p.run().deaths = 3;
        p.run().used.add("potion");
        p.run().segments.put("pocket_potion", false);
        Progress back = SaveCodec.read(SaveCodec.write(p, 99), data);
        assertTrue(back.run().fromStart);
        assertEquals(3, back.run().deaths);
        assertTrue(back.run().used.contains("potion"));
        assertEquals(Boolean.FALSE, back.run().segments.get("pocket_potion"));
        assertEquals(GameMode.HARDCORE, back.mode());
        assertEquals("07", back.entry().level());
        assertEquals("01", back.checkpoint().level());
        assertEquals(17, back.entry().player().coins);
        assertEquals(90.5, back.playTime());
        assertEquals(p.entry().player().inventory.items(), back.entry().player().inventory.items());
    }

    @Test
    void damagedFileFallsBackToBackupAndExplainsOtherwise() {
        GameData data = data();
        MemoryStorage storage = new MemoryStorage();
        SaveSlots slots = new SaveSlots(storage, data);
        slots.save(1, progress(data), 10);
        slots.save(1, progress(data), 20);
        storage.files().put("slot2.json", "{ broken");
        assertEquals("07", slots.slot(1).progress().entry().level(), "прочитана резервная копия");

        storage.files().put("slot2.json.bak", "{ \"saveVersion\": 99 }");
        SaveSlots.Slot slot = slots.slot(1);
        assertTrue(slot.isBroken());
        assertNull(slot.progress());
    }

    @Test
    void validationMessages() {
        GameData data = data();
        String good = SaveCodec.write(progress(data), 1);
        String unknownLevel = good.replace("\"07\"", "\"99\"");
        assertTrue(
                assertThrows(SaveException.class, () -> SaveCodec.read(unknownLevel, data))
                        .getMessage()
                        .contains("уровень '99'"));
        String future = good.replace("\"saveVersion\": 2", "\"saveVersion\": 7");
        assertTrue(
                assertThrows(SaveException.class, () -> SaveCodec.read(future, data))
                        .getMessage()
                        .contains("более новой версии"));
    }

    @Test
    void version1SavesAreMigrated() throws SaveException {
        GameData data = data();
        String v1 =
                SaveCodec.write(progress(data), 1)
                        .replace("\"saveVersion\": 2", "\"saveVersion\": 1")
                        .replace("\"hp\": 40", "\"hp\": 3");
        Progress p = SaveCodec.read(v1, data);
        assertEquals(30, p.entry().player().hp, "здоровье из 4 переводится в шкалу 40");
    }

    @Test
    void lastUsedSlotAndEmptySlots() {
        GameData data = data();
        SaveSlots slots = new SaveSlots(new MemoryStorage(), data);
        assertEquals(-1, slots.last());
        assertEquals(0, slots.firstEmpty());
        slots.save(2, progress(data), 5);
        slots.save(0, progress(data), 6);
        assertEquals(0, slots.last());
        slots.delete(0);
        assertEquals(2, slots.last());
        assertTrue(slots.slot(0).isEmpty());
    }
}
