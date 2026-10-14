package com.zaig100.dargon.save;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.level.JsonPrinter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Полученные достижения и общие счётчики (смерти, монеты, покупки, уровни) — общие для всех слотов:
 * это прогресс игрока, а не прохождения. Файл {@code achievements-<пак>.json}.
 */
public final class AchievementStore {

    private final Storage storage;
    private final String file;
    private final Map<String, Long> unlocked = new TreeMap<>();
    private final Map<String, Integer> counters = new TreeMap<>();

    public AchievementStore(Storage storage, String pack) {
        this.storage = storage;
        this.file = "achievements-" + pack + ".json";
        load();
    }

    private void load() {
        String text = storage.read(file);
        if (text == null) {
            return;
        }
        try {
            JsonValue root = new JsonReader().parse(text);
            for (JsonValue u = root.get("unlocked").child; u != null; u = u.next) {
                unlocked.put(u.name, u.asLong());
            }
            for (JsonValue c = root.get("counters").child; c != null; c = c.next) {
                counters.put(c.name, c.asInt());
            }
        } catch (RuntimeException e) {
            unlocked.clear();
            counters.clear();
        }
    }

    private void save() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("unlocked", new LinkedHashMap<>(unlocked));
        root.put("counters", new LinkedHashMap<>(counters));
        storage.write(file, JsonPrinter.write(root));
    }

    public boolean has(String id) {
        return unlocked.containsKey(id);
    }

    /** Отметить полученным; {@code true}, если только что получено. */
    public boolean unlock(String id, long now) {
        if (unlocked.putIfAbsent(id, now) != null) {
            return false;
        }
        save();
        return true;
    }

    public int counter(String name) {
        return counters.getOrDefault(name, 0);
    }

    /** Прибавить к счётчику; возвращает новое значение. */
    public int add(String name, int amount) {
        int value = counters.merge(name, amount, Integer::sum);
        save();
        return value;
    }

    public Map<String, Long> unlocked() {
        return Collections.unmodifiableMap(unlocked);
    }

    /** Стереть всё (настройки → «Стереть всё»). */
    public void clear() {
        unlocked.clear();
        counters.clear();
        storage.delete(file);
    }
}
