package com.zaig100.dargon.game.achievement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.game.GameMode;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.game.Progress;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.save.AchievementStore;
import com.zaig100.dargon.save.MemoryStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Достижения встроенного пака по событиям. */
class AchievementTrackerTest {

    private static GameData data() {
        Path assets = Path.of(System.getProperty("dargon.assets", "../assets"));
        return GameData.loadBuiltin(
                path -> {
                    try {
                        Path f = assets.resolve(path);
                        return Files.isRegularFile(f)
                                ? Files.readString(f, StandardCharsets.UTF_8)
                                : null;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private final GameData data = data();
    private final AchievementStore store = new AchievementStore(new MemoryStorage(), "dargon");
    private final List<String> got = new ArrayList<>();
    private final AchievementTracker tracker =
            new AchievementTracker(data.pack(), store, a -> got.add(a.id()), () -> 1L);

    private Progress run(GameMode mode, boolean fromStart) {
        Progress p = Progress.newGame(mode, "dargon", "01", PlayerState.newGame(data.items()), 0);
        p.run().fromStart = fromStart;
        tracker.bind(p);
        return p;
    }

    private void pass(String level, String... used) {
        tracker.levelStarted();
        for (String u : used) {
            switch (u) {
                case "damage" -> tracker.damaged(5);
                case "potion" -> tracker.itemUsed("potion");
                case "torch" -> tracker.itemUsed("torch");
                default -> throw new IllegalArgumentException(u);
            }
        }
        tracker.levelCompleted(level);
    }

    @Test
    void levelAchievementsAndRewards() {
        run(GameMode.NORMAL, true);
        pass("01", "damage");
        assertTrue(got.contains("first_step"));
        assertFalse(got.contains("no_scratch"), "был урон");
        pass("02");
        assertTrue(got.contains("no_scratch"));
        pass("05");
        assertTrue(got.contains("act1"));
    }

    @Test
    void segmentNeedsWholeRangeCleanAndRetriesDoNotCount() {
        run(GameMode.NORMAL, true);
        pass("06");
        // Попытка с зельем, закончившаяся смертью, не портит отрезок.
        tracker.levelStarted();
        tracker.itemUsed("potion");
        tracker.died("trap");
        pass("07");
        pass("08");
        pass("09");
        pass("10");
        assertTrue(got.contains("pocket_potion"));

        got.clear();
        run(GameMode.NORMAL, true);
        pass("06");
        pass("08", "potion");
        pass("10");
        assertFalse(got.contains("pocket_potion"), "зелье на пройденном уровне отрезка");

        got.clear();
        run(GameMode.NORMAL, false);
        pass("08");
        pass("10");
        assertFalse(got.contains("pocket_potion"), "отрезок не начат с 06");
    }

    @Test
    void gameAchievementsNeedRunFromStart() {
        Progress p = run(GameMode.HARDCORE, true);
        pass("27");
        tracker.gameCompleted();
        assertTrue(got.contains("finale"));
        assertTrue(got.contains("hardcore"));
        assertTrue(got.contains("iron"), "без смертей");
        assertTrue(got.contains("ascetic"), "без зелий и покупок");
        assertTrue(got.contains("runner"), "время " + p.playTime());

        got.clear();
        run(GameMode.NORMAL, false);
        tracker.gameCompleted();
        assertEquals(List.of(), got, "с открытого отрезка — не считается");
    }

    @Test
    void casualAndCheatRunsGiveNothing() {
        run(GameMode.CASUAL, true);
        pass("01");
        tracker.died("explosion");
        Progress cheats = run(GameMode.NORMAL, true);
        cheats.markCheats();
        pass("01");
        assertEquals(List.of(), got);
        assertEquals(0, store.counter("deaths"));
    }

    @Test
    void deathsCountersAndHiddenOnes() {
        run(GameMode.NORMAL, true);
        tracker.died("explosion");
        assertTrue(got.contains("boom"));
        for (int i = 0; i < 99; i++) {
            tracker.died("trap");
        }
        assertTrue(got.contains("stubborn"));
        tracker.purchased();
        assertTrue(got.contains("shopper"));
    }
}
