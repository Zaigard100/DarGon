package com.zaig100.dargon.save;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaig100.dargon.game.PlayStats;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatsStoreTest {

    @Test
    void roundTripAndBrokenFile() {
        MemoryStorage storage = new MemoryStorage();
        StatsStore store = new StatsStore(storage);
        PlayStats stats = store.load();
        stats.entered("05");
        stats.died("05", "spikes", 12.5);
        stats.died("05", "spikes", 3);
        stats.used("05", "potion");
        stats.completed("05", 30);
        stats.completed("05", 20);
        store.save(stats);

        PlayStats loaded = store.load();
        PlayStats.Level level = loaded.level("05");
        assertEquals(1, level.entries);
        assertEquals(Map.of("spikes", 2), level.deaths);
        assertEquals(Map.of("potion", 1), level.itemsUsed);
        assertEquals(2, level.completions);
        assertEquals(20, level.bestTime);
        assertEquals(65.5, level.totalTime);

        storage.files().put("stats.json", "{ oops");
        assertEquals(0, store.load().totalDeaths(), "испорченная статистика — начинаем заново");
    }
}
