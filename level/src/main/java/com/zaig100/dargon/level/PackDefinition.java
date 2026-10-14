package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Пак уровней ({@code pack.json} в корне пака): метаданные, совместимость, список уровней, первый
 * уровень, стартовые наборы по режимам и достижения. Пак — папка или zip-архив {@code .dgpack} с
 * {@code pack.json}, {@code levels/}, {@code i18n/pack*.properties} и иконкой. Встроенная кампания
 * DarGon — тоже пак ({@code packs/dargon/}).
 *
 * <pre>{@code
 * { "format": 1, "id": "dargon", "name": "@pack.dargon.name", "author": "zaig100",
 *   "version": "4.0", "game": "4.0.0", "icon": "icon.png",
 *   "start": "01", "levels": ["01", "02", "shops/shop_05"],
 *   "modes": { "normal": { "start": [ { "id": "potion", "count": 3 } ] }, ... },
 *   "achievements": [ { "id": "act1", "name": "@ach.act1.name", "description": "@ach.act1.desc",
 *                       "difficulty": "easy",
 *                       "when": { "type": "level", "level": "05", "modes": ["normal", "hardcore"] },
 *                       "reward": { "unlockStart": "06" } } ] }
 * }</pre>
 */
public final class PackDefinition {

    /** Режимы прохождения. */
    public static final List<String> MODES = List.of("casual", "normal", "hardcore");

    /** Сложность достижения (для подписи и сортировки). */
    public static final List<String> DIFFICULTIES = List.of("easy", "medium", "hard", "hardcore");

    /** Чего не должно быть при прохождении (условие {@code without}). */
    public static final List<String> RESTRICTIONS =
            List.of("damage", "potion", "torch", "trapSight", "shieldBreak", "purchase", "death");

    /** Типы условий достижений. */
    public static final List<String> CONDITIONS =
            List.of("level", "game", "count", "death", "script");

    /** Счётчики для условия {@code count} (за всё время, по всем прохождениям). */
    public static final List<String> COUNTERS = List.of("deaths", "coins", "purchases", "levels");

    /** Версия формата пака, которую понимает игра. */
    public static final int FORMAT = 1;

    /** Файл пака в его корне. */
    public static final String FILE = "pack.json";

    private static final Set<String> KEYS =
            Set.of(
                    "$schema",
                    "format",
                    "id",
                    "name",
                    "description",
                    "author",
                    "version",
                    "game",
                    "icon",
                    "start",
                    "levels",
                    "prefabs",
                    "textures",
                    "modes",
                    "achievements");

    /**
     * «Шапка» пака для списка паков — без уровней и достижений. Отсутствующие поля — пустые строки,
     * {@code format} — 0.
     */
    public record Info(
            int format,
            String id,
            String name,
            String description,
            String author,
            String version,
            String game,
            String icon) {

        /**
         * Почему пак не запустится на игре версии {@code gameVersion}; {@code null} — запустится.
         */
        public Incompatibility check(String gameVersion) {
            if (format > FORMAT) {
                return Incompatibility.NEWER_FORMAT;
            }
            if (format < 1) {
                return Incompatibility.BAD_FORMAT;
            }
            if (!game.isEmpty() && compareVersions(game, gameVersion) > 0) {
                return Incompatibility.NEWER_GAME;
            }
            return null;
        }
    }

    /** Причина, по которой пак не подходит этой версии игры. */
    public enum Incompatibility {
        /** Пак сделан для более нового формата. */
        NEWER_FORMAT,
        /** Нет или неверное поле {@code format}. */
        BAD_FORMAT,
        /** Пак требует более новую версию игры (поле {@code game}). */
        NEWER_GAME
    }

    /**
     * Условие достижения. Поля, не относящиеся к типу, — {@code null}/пустые. У условия {@code
     * level} без {@code level} подходит любой уровень.
     */
    public record Condition(
            String type,
            String level,
            String since,
            Set<String> without,
            Set<String> modes,
            Double maxTime,
            Integer maxDeaths,
            String counter,
            Integer atLeast,
            String kind) {

        /** Подходит ли режим (пустой список — любой, кроме казуала). */
        public boolean allows(String mode) {
            return modes.isEmpty() ? !mode.equals("casual") : modes.contains(mode);
        }
    }

    /** Награда: открыть старт с уровня и/или предмет в стартовом наборе. */
    public record Reward(String unlockStart, JsonValue startItem, Set<String> modes) {}

    /** Достижение. */
    public record Achievement(
            String id,
            String name,
            String description,
            String difficulty,
            boolean hidden,
            Condition when,
            Reward reward) {}

    private final Info info;
    private final List<String> levels;
    private final List<String> prefabs;
    private final List<String> textures;
    private final String start;
    private final Map<String, List<JsonValue>> startKits;
    private final List<Achievement> achievements;

    private PackDefinition(
            Info info,
            List<String> levels,
            List<String> prefabs,
            List<String> textures,
            String start,
            Map<String, List<JsonValue>> startKits,
            List<Achievement> achievements) {
        this.info = info;
        this.levels = levels;
        this.prefabs = prefabs;
        this.textures = textures;
        this.start = start;
        this.startKits = startKits;
        this.achievements = achievements;
    }

    public Info info() {
        return info;
    }

    public String id() {
        return info.id();
    }

    public String name() {
        return info.name();
    }

    /** Свои префабы пака: файлы {@code prefabs/<id>.json}. */
    public List<String> prefabs() {
        return prefabs;
    }

    /**
     * Свои текстуры пака: файлы {@code textures/<имя>.png}. Имя игровой картинки заменяет её на
     * время игры в паке.
     */
    public List<String> textures() {
        return textures;
    }

    /** Идентификаторы уровней пака в порядке {@code pack.json}. */
    public List<String> levels() {
        return levels;
    }

    public String start() {
        return start;
    }

    /** Стартовый набор режима (стопки предметов в формате уровня). */
    public List<JsonValue> startKit(String mode) {
        return startKits.getOrDefault(mode, List.of());
    }

    public List<Achievement> achievements() {
        return achievements;
    }

    public Achievement achievement(String id) {
        for (Achievement a : achievements) {
            if (a.id().equals(id)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Читает только «шапку» пака (для списка паков): без проверки уровней и достижений.
     *
     * @throws LevelFormatException если это не JSON-объект
     */
    public static Info header(String source, String file) {
        JsonValue root = JsonValues.parse(source, file);
        if (!root.isObject()) {
            throw new LevelFormatException(file, "", "ожидался объект");
        }
        JsonValue format = JsonValues.get(root, "format");
        return new Info(
                format != null && format.isNumber() ? format.asInt() : 0,
                JsonValues.getString(root, "id", ""),
                JsonValues.getString(root, "name", ""),
                JsonValues.getString(root, "description", ""),
                JsonValues.getString(root, "author", ""),
                JsonValues.getString(root, "version", ""),
                JsonValues.getString(root, "game", ""),
                JsonValues.getString(root, "icon", ""));
    }

    /**
     * Сравнивает версии вида {@code 4.0.1} по числам; хвост вроде {@code -beta1} не учитывается.
     */
    public static int compareVersions(String a, String b) {
        int[] x = versionParts(a);
        int[] y = versionParts(b);
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int dx = i < x.length ? x[i] : 0;
            int dy = i < y.length ? y[i] : 0;
            if (dx != dy) {
                return Integer.compare(dx, dy);
            }
        }
        return 0;
    }

    private static int[] versionParts(String version) {
        String numeric = version.split("[^0-9.]", 2)[0];
        if (numeric.isEmpty()) {
            return new int[0];
        }
        String[] parts = numeric.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
        }
        return result;
    }

    /**
     * Разбирает пак и проверяет ссылки: уровни, предметы, условия. Файлы уровней не читаются — это
     * делает {@link GameData}.
     *
     * @throws LevelFormatException со всеми найденными ошибками
     */
    public static PackDefinition parse(String source, String file, ItemCatalog items) {
        JsonValue root = JsonValues.parse(source, file);
        Problems problems = new Problems(file);
        if (!root.isObject()) {
            problems.error("", "ожидался объект");
            problems.throwIfErrors();
        }
        JsonValues.checkKeys(root, "", KEYS, problems);
        Info info = header(source, file);
        if (info.format() != FORMAT) {
            problems.error(
                    "format",
                    info.format() == 0
                            ? "нет поля 'format' — версия формата пака (" + FORMAT + ")"
                            : "формат "
                                    + info.format()
                                    + " не поддерживается (нужен "
                                    + FORMAT
                                    + ")");
        }
        String id = required(root, "id", "", problems);
        if (id != null && !id.matches("[a-z][a-z0-9_]*")) {
            problems.error("id", "только строчные латинские буквы, цифры и _, с буквы");
        }
        required(root, "name", "", problems);
        List<String> levels = strings(root, "levels", problems);
        List<String> prefabs = optionalNames(root, "prefabs", "[a-z][a-z0-9_]*", problems);
        List<String> textures = optionalNames(root, "textures", "[A-Za-z0-9_]+", problems);
        String start = required(root, "start", "", problems);
        if (start != null && !levels.contains(start)) {
            problems.error("start", "уровня '" + start + "' нет в списке levels");
        }
        Map<String, List<JsonValue>> kits = new LinkedHashMap<>();
        JsonValue modes = JsonValues.get(root, "modes");
        if (modes != null) {
            for (JsonValue mode = modes.child; mode != null; mode = mode.next) {
                String path = JsonValues.child("modes", mode.name);
                if (!MODES.contains(mode.name)) {
                    problems.error(path, "неизвестный режим; допустимы " + MODES);
                    continue;
                }
                JsonValues.checkKeys(mode, path, Set.of("start"), problems);
                List<JsonValue> kit = new ArrayList<>();
                JsonValue stacks = JsonValues.get(mode, "start");
                int i = 0;
                for (JsonValue s = stacks == null ? null : stacks.child;
                        s != null;
                        s = s.next, i++) {
                    items.validateStack(
                            s, JsonValues.index(JsonValues.child(path, "start"), i), problems);
                    kit.add(s);
                }
                kits.put(mode.name, Collections.unmodifiableList(kit));
            }
        }
        List<Achievement> achievements = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        JsonValue list = JsonValues.get(root, "achievements");
        int index = 0;
        for (JsonValue a = list == null ? null : list.child; a != null; a = a.next, index++) {
            String path = JsonValues.index("achievements", index);
            Achievement achievement = achievement(a, path, levels, items, problems);
            if (achievement != null && !ids.add(achievement.id())) {
                problems.error(
                        JsonValues.child(path, "id"), "id '" + achievement.id() + "' уже есть");
            } else if (achievement != null) {
                achievements.add(achievement);
            }
        }
        problems.throwIfErrors();
        return new PackDefinition(
                info,
                Collections.unmodifiableList(levels),
                Collections.unmodifiableList(prefabs),
                Collections.unmodifiableList(textures),
                start,
                kits,
                Collections.unmodifiableList(achievements));
    }

    /** Необязательный список имён по шаблону. */
    private static List<String> optionalNames(
            JsonValue root, String key, String pattern, Problems problems) {
        if (JsonValues.get(root, key) == null) {
            return new ArrayList<>();
        }
        List<String> names = strings(root, key, problems);
        for (int i = 0; i < names.size(); i++) {
            if (!names.get(i).matches(pattern)) {
                problems.error(
                        JsonValues.index(key, i),
                        "'" + names.get(i) + "': только латиница, цифры и _");
            }
        }
        return names;
    }

    private static List<String> strings(JsonValue root, String key, Problems problems) {
        List<String> result = new ArrayList<>();
        JsonValue array = JsonValues.get(root, key);
        if (array == null || !array.isArray()) {
            problems.error(key, "ожидался массив строк");
            return result;
        }
        Set<String> seen = new LinkedHashSet<>();
        int i = 0;
        for (JsonValue value = array.child; value != null; value = value.next, i++) {
            if (!value.isString() || value.asString().isEmpty()) {
                problems.error(JsonValues.index(key, i), "ожидалась непустая строка");
            } else if (!seen.add(value.asString())) {
                problems.error(
                        JsonValues.index(key, i), "'" + value.asString() + "' указан дважды");
            } else {
                result.add(value.asString());
            }
        }
        return result;
    }

    private static Achievement achievement(
            JsonValue a, String path, List<String> levels, ItemCatalog items, Problems problems) {
        JsonValues.checkKeys(
                a,
                path,
                Set.of("id", "name", "description", "difficulty", "hidden", "when", "reward"),
                problems);
        String id = required(a, "id", path, problems);
        String name = required(a, "name", path, problems);
        String description = required(a, "description", path, problems);
        String difficulty = JsonValues.getString(a, "difficulty", "medium");
        if (!DIFFICULTIES.contains(difficulty)) {
            problems.error(JsonValues.child(path, "difficulty"), "допустимы " + DIFFICULTIES);
        }
        JsonValue when = JsonValues.get(a, "when");
        if (when == null || !when.isObject()) {
            problems.error(path, "нет условия 'when'");
            return null;
        }
        Condition condition = condition(when, JsonValues.child(path, "when"), levels, problems);
        Reward reward = null;
        JsonValue r = JsonValues.get(a, "reward");
        if (r != null) {
            String rp = JsonValues.child(path, "reward");
            JsonValues.checkKeys(r, rp, Set.of("unlockStart", "startItem", "modes"), problems);
            String unlock = JsonValues.getString(r, "unlockStart", null);
            if (unlock != null && !levels.contains(unlock)) {
                problems.error(JsonValues.child(rp, "unlockStart"), "уровня '" + unlock + "' нет");
            }
            JsonValue item = JsonValues.get(r, "startItem");
            if (item != null) {
                items.validateStack(item, JsonValues.child(rp, "startItem"), problems);
            }
            reward = new Reward(unlock, item, set(r, "modes", rp, MODES, problems));
        }
        if (id == null || name == null || description == null || condition == null) {
            return null;
        }
        return new Achievement(
                id,
                name,
                description,
                difficulty,
                a.getBoolean("hidden", false),
                condition,
                reward);
    }

    private static Condition condition(
            JsonValue w, String path, List<String> levels, Problems problems) {
        JsonValues.checkKeys(
                w,
                path,
                Set.of(
                        "type",
                        "level",
                        "since",
                        "without",
                        "modes",
                        "maxTime",
                        "maxDeaths",
                        "counter",
                        "atLeast",
                        "kind"),
                problems);
        String type = required(w, "type", path, problems);
        if (type != null && !CONDITIONS.contains(type)) {
            problems.error(JsonValues.child(path, "type"), "допустимы " + CONDITIONS);
            return null;
        }
        String level = JsonValues.getString(w, "level", null);
        String since = JsonValues.getString(w, "since", null);
        for (String[] ref : new String[][] {{"level", level}, {"since", since}}) {
            if (ref[1] != null && !levels.contains(ref[1])) {
                problems.error(JsonValues.child(path, ref[0]), "уровня '" + ref[1] + "' нет");
            }
        }
        String counter = JsonValues.getString(w, "counter", null);
        if ("count".equals(type) && (counter == null || !COUNTERS.contains(counter))) {
            problems.error(JsonValues.child(path, "counter"), "допустимы " + COUNTERS);
        }
        JsonValue maxTime = JsonValues.get(w, "maxTime");
        JsonValue maxDeaths = JsonValues.get(w, "maxDeaths");
        JsonValue atLeast = JsonValues.get(w, "atLeast");
        return new Condition(
                type,
                level,
                since,
                set(w, "without", path, RESTRICTIONS, problems),
                set(w, "modes", path, MODES, problems),
                maxTime == null ? null : maxTime.asDouble(),
                maxDeaths == null ? null : maxDeaths.asInt(),
                counter,
                atLeast == null ? null : atLeast.asInt(),
                JsonValues.getString(w, "kind", null));
    }

    private static Set<String> set(
            JsonValue parent, String key, String path, List<String> allowed, Problems problems) {
        Set<String> result = new LinkedHashSet<>();
        JsonValue array = JsonValues.get(parent, key);
        if (array == null) {
            return result;
        }
        int i = 0;
        for (JsonValue v = array.child; v != null; v = v.next, i++) {
            if (!v.isString() || !allowed.contains(v.asString())) {
                problems.error(
                        JsonValues.index(JsonValues.child(path, key), i), "допустимы " + allowed);
            } else {
                result.add(v.asString());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static String required(JsonValue o, String key, String path, Problems problems) {
        String value = JsonValues.getString(o, key, null);
        if (value == null || value.isEmpty()) {
            problems.error(path, "нет поля '" + key + "'");
        }
        return value;
    }
}
