package com.zaig100.dargon.save;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import com.zaig100.dargon.game.Balance;
import com.zaig100.dargon.game.GameMode;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.game.Progress;
import com.zaig100.dargon.game.item.Inventory;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.JsonPrinter;
import com.zaig100.dargon.level.JsonValues;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Файл сохранения — читаемый JSON:
 *
 * <pre>{@code
 * { "saveVersion": 1, "pack": "dargon", "mode": "normal", "created": ..., "updated": ...,
 *   "playTime": 812.5, "deathsInRow": 1,
 *   "entry":      { "level": "07", "player": { "hp": 3, "coins": 25, "inventory": [ ... ] } },
 *   "checkpoint": { "level": "05", "player": { ... } } }
 * }</pre>
 *
 * Защиты от ручной правки нет: игра офлайн, правка сейва никому не вредит. Но при загрузке всё
 * проверяется, и ошибка объясняется словами, а не падением.
 */
public final class SaveCodec {

    /** Текущая версия формата. Старые версии переводятся в новую при чтении. */
    public static final int VERSION = 2;

    private SaveCodec() {}

    public static String write(Progress progress, long updated) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("saveVersion", VERSION);
        root.put("pack", progress.pack());
        root.put("mode", progress.mode().id());
        root.put("created", progress.created());
        root.put("updated", updated);
        root.put("playTime", progress.playTime());
        root.put("deathsInRow", progress.deathsInRow());
        if (progress.cheats()) {
            root.put("cheats", true);
        }
        Progress.Run run = progress.run();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("fromStart", run.fromStart);
        r.put("deaths", run.deaths);
        r.put("purchases", run.purchases);
        r.put("used", new ArrayList<>(run.used));
        r.put("segments", new LinkedHashMap<>(run.segments));
        root.put("run", r);
        root.put("entry", snapshot(progress.entry()));
        root.put("checkpoint", snapshot(progress.checkpoint()));
        return JsonPrinter.write(root);
    }

    private static Map<String, Object> snapshot(Progress.Snapshot snapshot) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("level", snapshot.level());
        PlayerState player = snapshot.player();
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("hp", player.hp);
        p.put("coins", player.coins);
        if (player.shield > 0) {
            p.put("shield", player.shield);
        }
        List<Object> slots = new ArrayList<>();
        for (int i = 0; i < Inventory.SIZE; i++) {
            ItemStack stack = player.inventory.get(i);
            if (stack == null) {
                slots.add(null);
            } else {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("id", stack.id());
                s.put("count", stack.count());
                s.putAll(stack.fields());
                slots.add(s);
            }
        }
        p.put("inventory", slots);
        map.put("player", p);
        return map;
    }

    /**
     * Читает и проверяет сохранение.
     *
     * @throws SaveException с объяснением, что не так
     */
    public static Progress read(String text, GameData data) throws SaveException {
        JsonValue root;
        try {
            root = new JsonReader().parse(text);
        } catch (SerializationException e) {
            throw new SaveException("файл повреждён (не JSON)", e);
        }
        if (root == null || !root.isObject()) {
            throw new SaveException("файл повреждён");
        }
        int version = root.getInt("saveVersion", -1);
        if (version > VERSION) {
            throw new SaveException("сохранение от более новой версии игры (" + version + ")");
        }
        if (version < 1) {
            throw new SaveException("неизвестная версия сохранения");
        }
        if (version == 1) {
            migrateFrom1(root);
        }
        GameMode mode = GameMode.byId(root.getString("mode", ""));
        if (mode == null) {
            throw new SaveException("неизвестный режим '" + root.getString("mode", "") + "'");
        }
        // Сейвы без поля — от встроенной кампании (до паков его не было).
        String pack = root.getString("pack", GameData.BUILTIN_PACK);
        if (!pack.equals(data.pack().id())) {
            throw new SaveException("сохранение другого пака ('" + pack + "')");
        }
        try {
            Progress progress =
                    new Progress(
                            mode,
                            pack,
                            snapshot(root, "entry", data),
                            snapshot(root, "checkpoint", data),
                            Math.max(0, root.getInt("deathsInRow", 0)),
                            Math.max(0, root.getDouble("playTime", 0)),
                            root.getLong("created", 0));
            if (root.getBoolean("cheats", false)) {
                progress.markCheats();
            }
            JsonValue r = root.get("run");
            if (r != null) {
                Progress.Run run = progress.run();
                run.fromStart = r.getBoolean("fromStart", false);
                run.deaths = r.getInt("deaths", 0);
                run.purchases = r.getInt("purchases", 0);
                for (JsonValue u = r.get("used") == null ? null : r.get("used").child;
                        u != null;
                        u = u.next) {
                    run.used.add(u.asString());
                }
                for (JsonValue sg = r.get("segments") == null ? null : r.get("segments").child;
                        sg != null;
                        sg = sg.next) {
                    run.segments.put(sg.name, sg.asBoolean());
                }
            }
            return progress;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new SaveException("файл повреждён: " + e.getMessage(), e);
        }
    }

    /** Версия 1 → 2: здоровье было из 4, стало из 40. */
    private static void migrateFrom1(JsonValue root) {
        for (String name : new String[] {"entry", "checkpoint"}) {
            JsonValue player = root.get(name) == null ? null : root.get(name).get("player");
            if (player != null && player.has("hp")) {
                player.get("hp").set(player.getLong("hp") * 10, null);
            }
        }
    }

    /** Время последнего сохранения (мс) или 0. */
    public static long updated(String text) {
        try {
            return new JsonReader().parse(text).getLong("updated", 0);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static Progress.Snapshot snapshot(JsonValue root, String name, GameData data)
            throws SaveException {
        JsonValue json = JsonValues.get(root, name);
        if (json == null || !json.isObject()) {
            throw new SaveException("нет раздела '" + name + "'");
        }
        String level = json.getString("level", null);
        if (level == null || !data.levels().contains(level)) {
            throw new SaveException("уровень '" + level + "' не найден в игре");
        }
        JsonValue p = JsonValues.get(json, "player");
        if (p == null) {
            throw new SaveException("нет данных игрока в '" + name + "'");
        }
        int hp = p.getInt("hp", 0);
        if (hp < 1 || hp > PlayerState.MAX_HP) {
            throw new SaveException("здоровье вне диапазона: " + hp);
        }
        Inventory inventory = new Inventory(data.items());
        JsonValue slots = JsonValues.get(p, "inventory");
        int i = 0;
        for (JsonValue slot = slots == null ? null : slots.child;
                slot != null && i < Inventory.SIZE;
                slot = slot.next, i++) {
            if (slot.isNull()) {
                continue;
            }
            ItemStack stack = ItemStack.fromJson(slot);
            if (data.items().get(stack.id()) == null) {
                throw new SaveException("неизвестный предмет '" + stack.id() + "'");
            }
            inventory.set(i, stack);
        }
        PlayerState player = new PlayerState(hp, Math.max(0, p.getInt("coins", 0)), inventory);
        player.shield = Math.max(0, Math.min(Balance.SHIELD_MAX, p.getInt("shield", 0)));
        return new Progress.Snapshot(level, player);
    }
}
