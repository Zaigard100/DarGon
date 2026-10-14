package com.zaig100.dargon.save;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.game.PlayStats;
import com.zaig100.dargon.level.JsonPrinter;
import java.util.LinkedHashMap;
import java.util.Map;

/** {@link PlayStats} в файле {@code stats.json}. Испорченный файл — начинаем заново. */
public final class StatsStore {

    private static final String FILE = "stats.json";
    private static final int VERSION = 1;

    private final Storage storage;

    public StatsStore(Storage storage) {
        this.storage = storage;
    }

    public PlayStats load() {
        PlayStats stats = new PlayStats();
        String text = storage.read(FILE);
        if (text == null) {
            return stats;
        }
        try {
            JsonValue root = new JsonReader().parse(text);
            for (JsonValue l = root.get("levels").child; l != null; l = l.next) {
                PlayStats.Level level = stats.level(l.name);
                level.entries = l.getInt("entries", 0);
                level.completions = l.getInt("completions", 0);
                level.totalTime = l.getDouble("totalTime", 0);
                level.bestTime = l.getDouble("bestTime", 0);
                counts(l.get("deaths"), level.deaths);
                counts(l.get("itemsUsed"), level.itemsUsed);
            }
        } catch (RuntimeException e) {
            return new PlayStats();
        }
        return stats;
    }

    private static void counts(JsonValue json, Map<String, Integer> into) {
        for (JsonValue c = json == null ? null : json.child; c != null; c = c.next) {
            into.put(c.name, c.asInt());
        }
    }

    public void save(PlayStats stats) {
        Map<String, Object> levels = new LinkedHashMap<>();
        for (PlayStats.Level l : stats.levels()) {
            Map<String, Object> level = new LinkedHashMap<>();
            level.put("entries", l.entries);
            level.put("completions", l.completions);
            level.put("totalTime", Math.round(l.totalTime * 10) / 10.0);
            level.put("bestTime", Math.round(l.bestTime * 10) / 10.0);
            level.put("deaths", new LinkedHashMap<>(l.deaths));
            level.put("itemsUsed", new LinkedHashMap<>(l.itemsUsed));
            levels.put(l.id, level);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("statsVersion", VERSION);
        root.put("levels", levels);
        storage.write(FILE, JsonPrinter.write(root));
    }
}
