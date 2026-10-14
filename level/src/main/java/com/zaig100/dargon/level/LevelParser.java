package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Разбирает файл уровня в {@link LevelDefinition}. Проверяет структуру, типы и значения параметров
 * по префабам, уникальность {@code id}. Ошибки собираются все сразу и выдаются с путём ({@code
 * entities[3].period: ожидалось число}).
 *
 * <p>Проверки, которым нужен контекст (ссылки на другие уровни, компиляция скриптов, достижимость),
 * выполняет {@link LevelValidator}.
 */
public final class LevelParser {

    /** Ключи сущности, которые не являются параметрами префаба. */
    public static final Set<String> RESERVED_ENTITY_KEYS =
            Set.of("prefab", "id", "tags", "at", "enabled", "comment");

    private static final Set<String> LEVEL_KEYS =
            Set.of(
                    "$schema",
                    "formatVersion",
                    "name",
                    "dark",
                    "checkpoint",
                    "legend",
                    "tiles",
                    "edges",
                    "spawn",
                    "entities",
                    "script",
                    "comment");

    private static final String NAME_PATTERN = "[A-Za-z][A-Za-z0-9_]*";

    private final PrefabLibrary prefabs;

    public PrefabLibrary prefabs() {
        return prefabs;
    }

    public LevelParser(PrefabLibrary prefabs) {
        this.prefabs = prefabs;
    }

    /**
     * Разбирает уровень.
     *
     * @param id идентификатор уровня (путь относительно каталога уровней без {@code .json})
     * @throws LevelFormatException при ошибках
     */
    public LevelDefinition parse(String source, String id) {
        Problems problems = new Problems(id);
        LevelDefinition level = parse(source, id, problems);
        problems.throwIfErrors();
        return level;
    }

    /**
     * Разбирает уровень, складывая проблемы в {@code problems}; при ошибках возвращает {@code
     * null}.
     */
    public LevelDefinition parse(String source, String id, Problems problems) {
        JsonValue root;
        try {
            root = JsonValues.parse(source, id);
        } catch (LevelFormatException e) {
            for (Problem problem : e.problems()) {
                problems.error(problem.path(), problem.message());
            }
            return null;
        }
        return parse(root, id, problems);
    }

    LevelDefinition parse(JsonValue root, String id, Problems problems) {
        if (!root.isObject()) {
            problems.error("", "ожидался объект уровня");
            return null;
        }
        int errors = problems.errorCount();
        JsonValues.checkKeys(root, "", LEVEL_KEYS, problems);

        JsonValue version = JsonValues.get(root, "formatVersion");
        if (version == null) {
            problems.error("", "нет поля 'formatVersion'");
        } else if (!JsonValues.isInteger(version)
                || version.asInt() != LevelDefinition.FORMAT_VERSION) {
            problems.error(
                    "formatVersion",
                    "поддерживается только версия "
                            + LevelDefinition.FORMAT_VERSION
                            + ", получено "
                            + JsonValues.describe(version));
            return null;
        }

        String name = string(root, "name", true, problems);
        boolean dark = bool(root, "dark", problems);
        boolean checkpoint = bool(root, "checkpoint", problems);
        TileMap tiles = parseTiles(root, problems);
        WallEdge[] edges = tiles == null ? null : parseEdges(root, tiles, problems);
        GridPoint spawn = point(JsonValues.get(root, "spawn"), "spawn", problems);
        if (JsonValues.get(root, "spawn") == null) {
            problems.error("", "нет поля 'spawn'");
        }
        if (tiles != null && spawn != null && !tiles.isFloor(spawn)) {
            problems.error("spawn", "точка появления " + spawn + " не на полу");
        }
        String script = string(root, "script", false, problems);
        if (script != null && !script.endsWith(".dgs")) {
            problems.error("script", "файл скрипта должен иметь расширение .dgs");
        }

        List<EntitySpec> entities = new ArrayList<>();
        Map<String, EntitySpec> byId = new HashMap<>();
        JsonValue entitiesJson = JsonValues.get(root, "entities");
        if (entitiesJson != null) {
            if (!entitiesJson.isArray()) {
                problems.error("entities", "ожидался массив");
            } else {
                int index = 0;
                for (JsonValue entity = entitiesJson.child;
                        entity != null;
                        entity = entity.next, index++) {
                    EntitySpec spec = parseEntity(entity, index, problems);
                    if (spec == null) {
                        continue;
                    }
                    if (spec.id() != null) {
                        EntitySpec other = byId.putIfAbsent(spec.id(), spec);
                        if (other != null) {
                            problems.error(
                                    JsonValues.child(spec.path(), "id"),
                                    "id '" + spec.id() + "' уже занят сущностью " + other.path());
                        }
                    }
                    entities.add(spec);
                }
            }
        }
        if (problems.errorCount() != errors) {
            return null;
        }
        return new LevelDefinition(
                id, name, dark, checkpoint, tiles, edges, spawn, entities, byId, script);
    }

    // ------------------------------------------------------------------ карта

    private static TileMap parseTiles(JsonValue root, Problems problems) {
        Map<Character, TileType> legend = new HashMap<>();
        JsonValue legendJson = JsonValues.get(root, "legend");
        if (legendJson == null) {
            for (TileType type : TileType.values()) {
                legend.put(type.symbol, type);
            }
        } else if (!legendJson.isObject()) {
            problems.error("legend", "ожидался объект {\"символ\": \"тип\"}");
            return null;
        } else {
            for (JsonValue entry = legendJson.child; entry != null; entry = entry.next) {
                String path = JsonValues.child("legend", entry.name);
                TileType type = entry.isString() ? TileType.byId(entry.asString()) : null;
                if (entry.name.length() != 1) {
                    problems.error(path, "ключ легенды — ровно один символ");
                } else if (type == null) {
                    problems.error(
                            path,
                            "неизвестный тип клетки "
                                    + JsonValues.describe(entry)
                                    + "; допустимы: floor, wall");
                } else {
                    legend.put(entry.name.charAt(0), type);
                }
            }
        }
        JsonValue rows = JsonValues.get(root, "tiles");
        if (rows == null || !rows.isArray() || rows.size == 0) {
            problems.error("tiles", "ожидался непустой массив строк карты");
            return null;
        }
        int height = rows.size;
        int width = -1;
        int y = 0;
        for (JsonValue row = rows.child; row != null; row = row.next, y++) {
            String path = JsonValues.index("tiles", y);
            if (!row.isString() || row.asString().isEmpty()) {
                problems.error(path, "ожидалась непустая строка");
                return null;
            }
            if (width < 0) {
                width = row.asString().length();
            } else if (row.asString().length() != width) {
                problems.error(
                        path,
                        "длина строки "
                                + row.asString().length()
                                + ", а у первой строки "
                                + width
                                + " — карта должна быть прямоугольной");
                return null;
            }
        }
        TileMap map = new TileMap(width, height);
        y = 0;
        boolean ok = true;
        for (JsonValue row = rows.child; row != null; row = row.next, y++) {
            String line = row.asString();
            for (int x = 0; x < width; x++) {
                TileType type = legend.get(line.charAt(x));
                if (type == null) {
                    problems.error(
                            JsonValues.index("tiles", y),
                            "символ '"
                                    + line.charAt(x)
                                    + "' (столбец "
                                    + x
                                    + ") не описан в легенде");
                    ok = false;
                    break;
                }
                map.set(x, y, type);
            }
        }
        return ok ? map : null;
    }

    /**
     * Ручные края стен: строки того же размера, что и {@code tiles}. {@code .} — край по
     * автотайлингу, {@code #} — без края, {@code 1}–{@code 8} — заданный край ({@link WallEdge}).
     */
    private static WallEdge[] parseEdges(JsonValue root, TileMap tiles, Problems problems) {
        JsonValue rows = JsonValues.get(root, "edges");
        if (rows == null) {
            return null;
        }
        if (!rows.isArray() || rows.size != tiles.height()) {
            problems.error(
                    "edges",
                    "ожидался массив из " + tiles.height() + " строк — по строке на строку карты");
            return null;
        }
        WallEdge[] edges = new WallEdge[tiles.width() * tiles.height()];
        int y = 0;
        for (JsonValue row = rows.child; row != null; row = row.next, y++) {
            String path = JsonValues.index("edges", y);
            if (!row.isString() || row.asString().length() != tiles.width()) {
                problems.error(path, "ожидалась строка длиной " + tiles.width() + ", как у карты");
                return null;
            }
            String line = row.asString();
            for (int x = 0; x < tiles.width(); x++) {
                char symbol = line.charAt(x);
                if (symbol == '.') {
                    continue;
                }
                WallEdge edge = WallEdge.bySymbol(symbol);
                if (edge == null) {
                    problems.error(
                            path,
                            "символ '"
                                    + symbol
                                    + "' (столбец "
                                    + x
                                    + "): допустимы '.' — авто, '#' — без края, 1–8 — край");
                    return null;
                }
                if (tiles.isFloor(x, y)) {
                    problems.warning(
                            path, "край '" + symbol + "' (столбец " + x + ") на полу не рисуется");
                }
                edges[y * tiles.width() + x] = edge;
            }
        }
        return edges;
    }

    // ------------------------------------------------------------------ сущности

    private EntitySpec parseEntity(JsonValue entity, int index, Problems problems) {
        String path = JsonValues.index("entities", index);
        if (!entity.isObject()) {
            problems.error(path, "ожидался объект сущности");
            return null;
        }
        int errors = problems.errorCount();
        String prefabId = JsonValues.getString(entity, "prefab", null);
        Prefab prefab = prefabId == null ? null : prefabs.get(prefabId);
        if (prefab == null) {
            problems.error(
                    JsonValues.child(path, "prefab"),
                    prefabId == null
                            ? "нет поля 'prefab'"
                            : "неизвестный префаб '"
                                    + prefabId
                                    + "'"
                                    + JsonValues.suggest(prefabId, prefabs.ids()));
            return null;
        }
        String id = null;
        JsonValue idJson = JsonValues.get(entity, "id");
        if (idJson != null) {
            if (!idJson.isString() || !idJson.asString().matches(NAME_PATTERN)) {
                problems.error(
                        JsonValues.child(path, "id"),
                        "id — латинская буква, затем буквы, цифры или '_'");
            } else {
                id = idJson.asString();
            }
        }
        List<String> tags = new ArrayList<>();
        JsonValue tagsJson = JsonValues.get(entity, "tags");
        if (tagsJson != null) {
            if (!tagsJson.isArray()) {
                problems.error(JsonValues.child(path, "tags"), "ожидался массив строк");
            } else {
                int i = 0;
                for (JsonValue tag = tagsJson.child; tag != null; tag = tag.next, i++) {
                    if (!tag.isString() || !tag.asString().matches(NAME_PATTERN)) {
                        problems.error(
                                JsonValues.index(JsonValues.child(path, "tags"), i),
                                "тег — латинская буква, затем буквы, цифры или '_'");
                    } else if (!tags.contains(tag.asString())) {
                        tags.add(tag.asString());
                    }
                }
            }
        }
        GridPoint at = point(JsonValues.get(entity, "at"), JsonValues.child(path, "at"), problems);
        if (JsonValues.get(entity, "at") == null) {
            problems.error(path, "нет поля 'at'");
        }
        boolean enabled = true;
        JsonValue enabledJson = JsonValues.get(entity, "enabled");
        if (enabledJson != null) {
            if (!enabledJson.isBoolean()) {
                problems.error(JsonValues.child(path, "enabled"), "ожидалось true или false");
            } else {
                enabled = enabledJson.asBoolean();
            }
        }

        Map<String, JsonValue> params = new LinkedHashMap<>();
        Map<String, String> handlers = new LinkedHashMap<>();
        for (JsonValue field = entity.child; field != null; field = field.next) {
            String key = field.name;
            String fieldPath = JsonValues.child(path, key);
            if (RESERVED_ENTITY_KEYS.contains(key)) {
                continue;
            }
            if (key.matches("on[A-Z].*")) {
                String event = Character.toLowerCase(key.charAt(2)) + key.substring(3);
                if (!prefab.events().contains(event)) {
                    problems.error(
                            fieldPath,
                            "префаб '"
                                    + prefab.id()
                                    + "' не порождает событие '"
                                    + event
                                    + "'"
                                    + (prefab.events().isEmpty()
                                            ? ""
                                            : "; доступны: " + handlerKeys(prefab)));
                } else {
                    String code = handlerCode(field);
                    if (code == null) {
                        problems.error(
                                fieldPath, "обработчик — строка с кодом DGScript или массив строк");
                    } else {
                        handlers.put(event, code);
                    }
                }
                continue;
            }
            ParamSpec spec = prefab.params().get(key);
            if (spec == null) {
                Set<String> known = new TreeSet<>(prefab.params().keySet());
                known.addAll(RESERVED_ENTITY_KEYS);
                problems.error(
                        fieldPath,
                        "у префаба '"
                                + prefab.id()
                                + "' нет параметра '"
                                + key
                                + "'"
                                + JsonValues.suggest(key, known));
                continue;
            }
            spec.validate(field, fieldPath, problems, prefabs.items());
            params.put(key, field);
        }
        Map<String, JsonValue> all = new LinkedHashMap<>();
        for (ParamSpec spec : prefab.params().values()) {
            JsonValue value = params.get(spec.name());
            if (value == null) {
                if (spec.isRequired()) {
                    problems.error(
                            path,
                            "не задан обязательный параметр '"
                                    + spec.name()
                                    + "' префаба '"
                                    + prefab.id()
                                    + "'");
                    continue;
                }
                value = spec.defaultValue();
            }
            all.put(spec.name(), value);
        }
        if (problems.errorCount() != errors) {
            return null;
        }
        return new EntitySpec(
                index,
                prefab,
                id,
                Collections.unmodifiableList(tags),
                at,
                enabled,
                Collections.unmodifiableMap(all),
                Collections.unmodifiableMap(handlers));
    }

    /** Код обработчика: строка или массив строк (по строке кода на элемент). */
    private static String handlerCode(JsonValue field) {
        if (field.isString()) {
            return field.asString();
        }
        if (!field.isArray()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (JsonValue line = field.child; line != null; line = line.next) {
            if (!line.isString()) {
                return null;
            }
            lines.add(line.asString());
        }
        return String.join("\n", lines);
    }

    private static String handlerKeys(Prefab prefab) {
        List<String> keys = new ArrayList<>();
        for (String event : prefab.events()) {
            keys.add(Prefab.handlerKey(event));
        }
        return String.join(", ", keys);
    }

    // ------------------------------------------------------------------ простые поля

    private static GridPoint point(JsonValue value, String path, Problems problems) {
        if (value == null) {
            return null;
        }
        int errors = problems.errorCount();
        ParamType.POINT.validate(value, path, problems, ItemCatalog.empty());
        if (problems.errorCount() != errors) {
            return null;
        }
        return new GridPoint(value.child.asInt(), value.child.next.asInt());
    }

    private static String string(JsonValue root, String key, boolean required, Problems problems) {
        JsonValue value = root.get(key);
        if (value == null) {
            if (required) {
                problems.error("", "нет поля '" + key + "'");
            }
            return null;
        }
        if (!value.isString() || value.asString().isEmpty()) {
            problems.error(
                    key, "ожидалась непустая строка, получено " + JsonValues.describe(value));
            return null;
        }
        return value.asString();
    }

    private static boolean bool(JsonValue root, String key, Problems problems) {
        JsonValue value = root.get(key);
        if (value == null) {
            return false;
        }
        if (!value.isBoolean()) {
            problems.error(key, "ожидалось true или false, получено " + JsonValues.describe(value));
            return false;
        }
        return value.asBoolean();
    }
}
