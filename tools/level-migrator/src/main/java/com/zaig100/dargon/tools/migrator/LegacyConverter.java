package com.zaig100.dargon.tools.migrator;

import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.level.Autotiler;
import com.zaig100.dargon.level.Direction;
import com.zaig100.dargon.level.JsonPrinter;
import com.zaig100.dargon.level.TileMap;
import com.zaig100.dargon.level.TileType;
import com.zaig100.dargon.level.WallEdge;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Переводит один уровень DarGon3 в новый формат.
 *
 * <p>В старом формате Y рос вверх (строка 0 массива {@code map} — верхняя на экране, но имела
 * координату {@code height - 1}). В новом Y растёт вниз, поэтому все координаты переворачиваются:
 * {@code y' = height - 1 - y}.
 *
 * <p>Команды кнопок {@code "Tag:Cmd>Arg"} переводятся в DGScript. Старый приём «спрятать объект за
 * картой и выдвинуть кнопкой» превращается в выключенную сущность на её будущем месте и вызов
 * {@code enable()}.
 */
final class LegacyConverter {

    /** Старые тайлы: 11 — пол, остальное (10, декор 12–19, метки-линейки 20–90) — стена. */
    private static final int LEGACY_FLOOR = 11;

    /** Обработчики длиннее этого переносятся в файл скрипта уровня. */
    private static final int INLINE_HANDLER_LINES = 6;

    /** Старые типы объектов в порядке чтения DarGon3 и префиксы автоматических тегов. */
    private static final String[][] KINDS = {
        {"HideTrap", "HT"},
        // Опечатка в уровнях: DarGon3 это поле не читал, но ловушки задуманы — переносим их.
        {"HideTraps", "HT"},
        {"Teleport", "Tp"},
        {"Stair", "St"},
        {"Item", "I"},
        {"FlimsyTile", "FT"},
        {"Flamefrower", "Ff"},
        {"Crossbow", "Cb"},
        {"Spinney", "Sn"},
        {"Spike", "Sk"},
        {"Button", "Bt"},
        {"Door", "Bt"},
        {"Tablet", "Tb"},
        {"Chest", null},
        {"Shop", "Sp"},
        {"Zona", "Sp"},
        {"Kamikaze", "Kmz"},
        {"CrossbowAI", "Cb"}
    };

    /** Ключи старого уровня, которые DarGon3 читал. Остальные отбрасываются с пометкой в отчёте. */
    /**
     * Уровни, где нарисованные вручную края стен — часть замысла (08 «Crashed» выглядит сломанным):
     * края переносятся в поле {@code edges} как есть.
     */
    private static final Set<String> MANUAL_EDGES = Set.of("08");

    private static final List<String> KNOWN_KEYS =
            List.of("LevelName", "Spawn", "map", "isDark", "save", "ReMap");

    private final JsonValue root;
    private final String levelId;
    private final Function<String, String> levelIds;
    private final Report report;
    private final TextBundle texts;
    private final String textPrefix;

    private int width;
    private int height;
    private final List<LegacyObject> objects = new ArrayList<>();
    private final StringBuilder script = new StringBuilder();

    /**
     * @param levelIds переводит старый путь {@code "levels/06.json"} в новый id; {@code null} — нет
     *     такого уровня
     */
    LegacyConverter(
            JsonValue root,
            String levelId,
            Function<String, String> levelIds,
            Report report,
            TextBundle texts) {
        this.root = root;
        this.levelId = levelId;
        this.levelIds = levelIds;
        this.report = report;
        this.texts = texts;
        this.textPrefix = "level." + levelId.replace('/', '.');
    }

    /** Объект старого уровня. */
    private static final class LegacyObject {
        final String kind;
        final JsonValue json;
        final String legacyTag;
        final boolean explicitTag;
        int x;
        int y;
        String id;
        boolean enabled = true;

        /** Группа команд, которая выдвигает спрятанный объект на карту. */
        Group parkedBy;

        final Map<String, Object> params = new LinkedHashMap<>();
        String prefab;
        Map<String, Object> handlers = new LinkedHashMap<>();

        LegacyObject(String kind, JsonValue json, String legacyTag, boolean explicitTag) {
            this.kind = kind;
            this.json = json;
            this.legacyTag = legacyTag;
            this.explicitTag = explicitTag;
            this.x = json.getInt("X");
            this.y = json.getInt("Y");
        }
    }

    /** Команды одной кнопки, обращённые к одному объекту. */
    private static final class Group {
        final LegacyObject button;
        final String target;
        final List<String[]> commands = new ArrayList<>();

        Group(LegacyObject button, String target) {
            this.button = button;
            this.target = target;
        }

        Integer value(String op) {
            Integer result = null;
            for (String[] command : commands) {
                if (command[0].equals(op)) {
                    result = Integer.parseInt(command[1]);
                }
            }
            return result;
        }
    }

    /** Результат: JSON уровня и (если нужен) текст скрипта. */
    record Result(Map<String, Object> level, String script) {}

    Result convert() {
        for (JsonValue field = root.child; field != null; field = field.next) {
            boolean known = KNOWN_KEYS.contains(field.name);
            for (String[] kind : KINDS) {
                known |= kind[0].equals(field.name);
            }
            if (!known) {
                report.note(levelId, "поле `" + field.name + "` DarGon3 не читал — отброшено");
            }
            if (field.name.equals("HideTraps")) {
                report.note(
                        levelId,
                        "поле `HideTraps` (опечатка, DarGon3 его не читал) перенесено как скрытые"
                                + " ловушки");
            }
        }
        TileMap tiles = readTiles();
        readObjects();
        List<Group> groups = readButtonGroups();
        resolveTargets(groups);
        park(groups);
        for (LegacyObject object : objects) {
            convertObject(object);
        }
        translateButtons(groups);

        Map<String, Object> level = new LinkedHashMap<>();
        level.put("$schema", "../".repeat(levelId.split("/").length) + "data/level.schema.json");
        level.put("formatVersion", 1);
        level.put("name", texts.add(textPrefix + ".name", Texts.english(levelName())));
        if (root.getBoolean("isDark", false)) {
            level.put("dark", true);
        }
        if (root.getBoolean("save", false)) {
            level.put("checkpoint", true);
        }
        level.put("tiles", new JsonPrinter.Multiline(List.of(tiles.rows())));
        if (MANUAL_EDGES.contains(levelId)) {
            level.put("edges", new JsonPrinter.Multiline(manualEdges(tiles)));
            report.note(levelId, "края стен перенесены вручную (поле `edges`)");
        }
        JsonValue spawn = root.get("Spawn");
        level.put("spawn", point(spawn.getInt(0), spawn.getInt(1)));
        List<Object> entities = new ArrayList<>();
        for (LegacyObject object : objects) {
            if (object.prefab != null) {
                entities.add(entity(object));
            }
        }
        level.put("entities", entities);
        String scriptText = null;
        if (script.length() > 0) {
            String file = levelId.substring(levelId.lastIndexOf('/') + 1) + ".dgs";
            level.put("script", file);
            scriptText =
                    "// Скрипт уровня "
                            + levelId
                            + " (переведён из функций кнопок DarGon3).\n"
                            + script;
        }
        return new Result(level, scriptText);
    }

    private String levelName() {
        String name = root.getString("LevelName");
        return levelId.equals("hub") ? "All levels" : name;
    }

    // ------------------------------------------------------------------ карта

    private TileMap readTiles() {
        JsonValue map = root.get("map");
        height = map.size;
        width = map.child.size;
        TileMap tiles = new TileMap(width, height);
        int y = 0;
        int legacyDecor = 0;
        int mismatches = 0;
        for (JsonValue row = map.child; row != null; row = row.next, y++) {
            int x = 0;
            for (JsonValue cell = row.child; cell != null; cell = cell.next, x++) {
                tiles.set(x, y, cell.asInt() == LEGACY_FLOOR ? TileType.FLOOR : TileType.WALL);
            }
        }
        boolean remap = root.getBoolean("ReMap", false);
        y = 0;
        for (JsonValue row = map.child; row != null; row = row.next, y++) {
            int x = 0;
            for (JsonValue cell = row.child; cell != null; cell = cell.next, x++) {
                int legacy = cell.asInt();
                if (legacy >= 12 && legacy <= 19) {
                    legacyDecor++;
                }
                if (!remap && legacyEdge(legacy) != Autotiler.edge(tiles, x, y)) {
                    mismatches++;
                }
            }
        }
        if (mismatches > 0 && !MANUAL_EDGES.contains(levelId)) {
            report.note(
                    levelId,
                    "автотайлинг отличается от нарисованных вручную краёв стен в "
                            + mismatches
                            + " клетках (из "
                            + legacyDecor
                            + " декоративных)");
        }
        return tiles;
    }

    /** Края стен так, как они нарисованы в старой карте. */
    private List<String> manualEdges(TileMap tiles) {
        List<String> rows = new ArrayList<>();
        int y = 0;
        for (JsonValue row = root.get("map").child; row != null; row = row.next, y++) {
            StringBuilder line = new StringBuilder();
            int x = 0;
            for (JsonValue cell = row.child; cell != null; cell = cell.next, x++) {
                line.append(tiles.isFloor(x, y) ? '.' : legacyEdge(cell.asInt()).symbol());
            }
            rows.add(line.toString());
        }
        return rows;
    }

    private static WallEdge legacyEdge(int tile) {
        return switch (tile) {
            case 12 -> WallEdge.LEFT_DOWN;
            case 13 -> WallEdge.UP_RIGHT;
            case 14 -> WallEdge.LEFT_UP;
            case 15 -> WallEdge.DOWN_RIGHT;
            case 16 -> WallEdge.LEFT_UP_RIGHT;
            case 17 -> WallEdge.LEFT_DOWN_RIGHT;
            case 18 -> WallEdge.UP_DOWN_RIGHT;
            case 19 -> WallEdge.LEFT_UP_DOWN;
            default -> WallEdge.NONE;
        };
    }

    private boolean inMap(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /** Старая координата → новая клетка {@code [x, y']}. */
    private List<Integer> point(int x, int y) {
        return List.of(x, height - 1 - y);
    }

    // ------------------------------------------------------------------ объекты

    private void readObjects() {
        for (String[] kind : KINDS) {
            JsonValue array = root.get(kind[0]);
            if (array == null) {
                continue;
            }
            int i = 0;
            for (JsonValue json = array.child; json != null; json = json.next, i++) {
                String tag = json.getString("Tag", null);
                boolean explicit = tag != null;
                if (!explicit && kind[1] != null) {
                    tag = kind[1] + i;
                }
                objects.add(new LegacyObject(kind[0], json, tag, explicit));
            }
        }
    }

    private List<Group> readButtonGroups() {
        List<Group> groups = new ArrayList<>();
        for (LegacyObject button : objects) {
            if (!button.kind.equals("Button")) {
                continue;
            }
            Map<String, Group> byTarget = new LinkedHashMap<>();
            for (JsonValue function = button.json.get("Function").child;
                    function != null;
                    function = function.next) {
                String raw = function.asString();
                int colon = raw.indexOf(':');
                String target = raw.substring(0, colon);
                String rest = raw.substring(colon + 1);
                String[] command;
                if (target.equals("Map")) {
                    command = new String[] {rest, null, raw};
                    target = "Map#" + byTarget.size();
                } else {
                    int arrow = rest.indexOf('>');
                    command =
                            arrow < 0
                                    ? new String[] {rest, null, raw}
                                    : new String[] {
                                        rest.substring(0, arrow), rest.substring(arrow + 1), raw
                                    };
                }
                byTarget.computeIfAbsent(target, t -> new Group(button, t)).commands.add(command);
            }
            groups.addAll(byTarget.values());
        }
        return groups;
    }

    /** Объекты, на которые ссылаются команды, получают id по старому тегу. */
    private void resolveTargets(List<Group> groups) {
        for (LegacyObject object : objects) {
            if (object.explicitTag) {
                object.id = object.legacyTag;
            }
        }
        for (Group group : groups) {
            if (group.target.startsWith("Map#")) {
                continue;
            }
            List<LegacyObject> found = targets(group.target);
            if (found.isEmpty()) {
                report.note(
                        levelId,
                        "команды кнопки ссылаются на несуществующий объект `"
                                + group.target
                                + "` — в DarGon3 они ничего не делали, пропущены");
            } else if (found.size() > 1) {
                report.note(
                        levelId,
                        "тег `"
                                + group.target
                                + "` носят "
                                + found.size()
                                + " объекта (коллизия тегов DarGon3) — команды применяются к первому");
            }
            if (!found.isEmpty()) {
                found.get(0).id = group.target;
            }
        }
    }

    private List<LegacyObject> targets(String tag) {
        List<LegacyObject> found = new ArrayList<>();
        for (LegacyObject object : objects) {
            if (tag.equals(object.legacyTag)) {
                found.add(object);
            }
        }
        return found;
    }

    private LegacyObject target(Group group) {
        List<LegacyObject> found = targets(group.target);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Спрятанные за картой объекты встают на место, куда их выдвигает кнопка, и выключаются. */
    private void park(List<Group> groups) {
        for (Group group : groups) {
            LegacyObject object = group.target.startsWith("Map#") ? null : target(group);
            if (object == null
                    || inMap(object.x, object.y)
                    || object.parkedBy != null
                    || isWallMounted(object)) {
                continue;
            }
            Integer nx = group.value("X");
            Integer ny = group.value("Y");
            int x = nx != null ? nx : object.x;
            int y = ny != null ? ny : object.y;
            if ((nx != null || ny != null) && inMap(x, y)) {
                object.x = x;
                object.y = y;
                object.enabled = false;
                object.parkedBy = group;
            }
        }
    }

    private static boolean isWallMounted(LegacyObject object) {
        return object.kind.equals("Flamefrower") || object.kind.startsWith("Crossbow");
    }

    private void convertObject(LegacyObject o) {
        JsonValue j = o.json;
        Map<String, Object> p = o.params;
        switch (o.kind) {
            case "HideTrap", "HideTraps" -> o.prefab = "hidden_trap";
            case "Teleport" -> {
                o.prefab = "teleport";
                p.put("target", point(j.getInt("TX"), j.getInt("TY")));
            }
            case "Stair" -> {
                o.prefab = "stairs";
                String next = j.getString("Next", null);
                if (next != null && !j.getBoolean("End", false) && !next.equals("END")) {
                    String id = levelIds.apply(next);
                    if (id == null) {
                        report.note(
                                levelId, "лестница ведёт на неизвестный уровень `" + next + "`");
                    }
                    p.put("next", id);
                } else {
                    report.note(levelId, "лестница " + point(o.x, o.y) + " — конец игры");
                }
                if (j.getBoolean("FlipX", false)) {
                    p.put("flip", true);
                }
            }
            case "Item" -> {
                if (!j.getBoolean("Active", true)) {
                    report.note(levelId, "неактивный предмет " + point(o.x, o.y) + " пропущен");
                    return;
                }
                o.prefab = "item";
                p.put("item", item(j.get("Item")));
            }
            case "FlimsyTile" -> {
                o.prefab = "fragile_floor";
                putIfNot(p, "cracks", j.getInt("Stage", 0), 0);
                putNumber(p, "period", j, 1.0);
            }
            case "Flamefrower" -> {
                o.prefab = "flamethrower";
                p.put("dir", rotation(j.getInt("Rot")).id());
                p.put("length", j.getInt("Max"));
                p.put("stage", j.getInt("Stage"));
                putNumber(p, "period", j, 2.0);
            }
            case "Crossbow", "CrossbowAI" -> {
                o.prefab = o.kind.equals("Crossbow") ? "crossbow" : "smart_crossbow";
                // Стрела летела на (DX, DY) в координатах «Y вверх».
                Direction dir = Direction.of(j.getInt("DX"), -j.getInt("DY"));
                p.put("dir", dir.id());
                putNumber(p, "period", j, 4.0);
                if (o.kind.equals("CrossbowAI")) {
                    p.put("range", j.getInt("Distance"));
                    p.put("diagonal", j.getBoolean("Diagonal"));
                }
            }
            case "Spinney" -> {
                o.prefab = "bushes";
                int w = Math.abs(j.getInt("Width"));
                int h = Math.abs(j.getInt("Height"));
                // Кусты рисовались от (x, y) вверх: верхний левый угол — (x, y + h - 1).
                o.y = o.y + h - 1;
                p.put("width", w);
                p.put("height", h);
            }
            case "Spike" -> {
                o.prefab = "spikes";
                if (j.getBoolean("Active", false)) {
                    p.put("active", true);
                }
                putNumber(p, "period", j, 1.5);
            }
            case "Button" -> o.prefab = "button";
            case "Door" -> {
                o.prefab = "door";
                p.put("key", j.getString("KeyTag"));
                if (j.getBoolean("IsOpen", false)) {
                    p.put("open", true);
                }
                putIfNot(p, "facing", j.getInt("Facing", 0), 0);
            }
            case "Tablet" -> {
                o.prefab = "tablet";
                p.put(
                        "text",
                        texts.add(
                                textPrefix + ".tablet" + (tabletCount++ + 1),
                                Texts.english(String.join("\n", j.get("Text").asStringArray()))));
            }
            case "Chest" -> {
                o.prefab = "chest";
                List<Object> items = new ArrayList<>();
                for (JsonValue item = j.get("Items").child; item != null; item = item.next) {
                    items.add(item(item));
                }
                p.put("items", items);
                // DarGon3 учитывал замок, только если заданы и Locked, и Open.
                if (hasExact(j, "Locked") && hasExact(j, "Open")) {
                    p.put("locked", j.getBoolean("Locked"));
                    p.put("key", j.getString("KeyTag"));
                    p.put("open", j.getBoolean("Open"));
                } else if (hasExact(j, "Loked")) {
                    report.note(
                            levelId,
                            "у сундука опечатка `Loked` — в DarGon3 замок не работал, сундук открыт");
                }
            }
            case "Shop" -> {
                o.prefab = "shop";
                List<Object> offers = new ArrayList<>();
                int i = 0;
                for (JsonValue slot = j.get("Slots").child;
                        slot != null && i < 3;
                        slot = slot.next, i++) {
                    Map<String, Object> offer = new LinkedHashMap<>();
                    offer.put("item", item(slot.get("Item")));
                    offer.put("price", slot.getInt("Cost"));
                    offers.add(offer);
                }
                p.put("offers", offers);
            }
            case "Zona" -> convertZone(o);
            case "Kamikaze" -> {
                o.prefab = "kamikaze";
                p.put("radius", j.getInt("Radius"));
                putIfNot(p, "depth", j.getInt("Iters", 15), 15);
                p.put("speed", j.getFloat("Speed"));
            }
            default -> throw new IllegalStateException(o.kind);
        }
    }

    private int tabletCount;

    /** Зона DarGon3 включала и правую/нижнюю границу: x..x+W, y..y+H. Сохраняем покрытие. */
    private void convertZone(LegacyObject o) {
        JsonValue j = o.json;
        o.prefab = "zone";
        int x0 = Math.max(0, o.x);
        int x1 = Math.min(width - 1, o.x + j.getInt("Wight"));
        int y0 = Math.max(0, o.y);
        int y1 = Math.min(height - 1, o.y + j.getInt("Height"));
        o.x = x0;
        o.y = y1;
        o.params.put("width", x1 - x0 + 1);
        o.params.put("height", y1 - y0 + 1);
        o.params.put("effect", j.getString("Type").equals("hp+") ? "heal" : "damage");
        o.params.put("period", j.getDouble("Tick"));
        report.note(
                levelId,
                "зона: размер пересчитан с учётом старой ошибки «на 1 больше» и обрезан по карте ("
                        + (x1 - x0 + 1)
                        + "×"
                        + (y1 - y0 + 1)
                        + ")");
    }

    private static boolean hasExact(JsonValue object, String key) {
        for (JsonValue child = object.child; child != null; child = child.next) {
            if (key.equals(child.name)) {
                return true;
            }
        }
        return false;
    }

    private static void putIfNot(Map<String, Object> params, String key, int value, int fallback) {
        if (value != fallback) {
            params.put(key, value);
        }
    }

    private static void putNumber(
            Map<String, Object> params, String key, JsonValue json, double fallback) {
        if (hasExact(json, "Tick") && json.getDouble("Tick") != fallback) {
            params.put(key, json.getDouble("Tick"));
        }
    }

    /** Поворот огнемёта DarGon3: 0 — вправо, 1 — вверх, 2 — влево, 3 — вниз. */
    private static Direction rotation(int rot) {
        return switch (rot) {
            case 0 -> Direction.RIGHT;
            case 1 -> Direction.UP;
            case 2 -> Direction.LEFT;
            case 3 -> Direction.DOWN;
            default -> throw new IllegalArgumentException("Rot " + rot);
        };
    }

    private Map<String, Object> item(JsonValue item) {
        Map<String, Object> stack = new LinkedHashMap<>();
        String type = item.getString("Type");
        int count = item.getInt("Count", 1);
        switch (type) {
            case "money" -> {
                stack.put("id", "coins");
                stack.put("count", count);
            }
            case "potion", "sheld", "torch", "show_trap" -> {
                stack.put(
                        "id",
                        switch (type) {
                            case "sheld" -> "shield";
                            case "show_trap" -> "trap_sight";
                            default -> type;
                        });
                if (count != 1) {
                    stack.put("count", count);
                }
            }
            case "speed", "slowdown" -> {
                stack.put("id", type.equals("speed") ? "speed_potion" : "slow_potion");
                if (count != 1) {
                    stack.put("count", count);
                }
                stack.put("power", item.getDouble("Power"));
            }
            case "key" -> {
                stack.put("id", "key");
                stack.put("key", item.getString("KeyTag"));
                stack.put(
                        "color",
                        String.format(
                                Locale.ROOT,
                                "#%02X%02X%02X",
                                channel(item, "R"),
                                channel(item, "G"),
                                channel(item, "B")));
            }
            case "egg" -> {
                stack.put("id", "egg");
                stack.put("code", item.getString("Code"));
            }
            default -> throw new IllegalArgumentException("Неизвестный тип предмета " + type);
        }
        return stack;
    }

    private static int channel(JsonValue item, String key) {
        return Math.round(item.getFloat(key) * 255);
    }

    private Map<String, Object> entity(LegacyObject o) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("prefab", o.prefab);
        if (o.id != null) {
            entity.put("id", o.id);
        }
        entity.put("at", point(o.x, o.y));
        if (!o.enabled) {
            entity.put("enabled", false);
        }
        entity.putAll(o.params);
        entity.putAll(o.handlers);
        return entity;
    }

    // ------------------------------------------------------------------ кнопки

    private void translateButtons(List<Group> groups) {
        Map<LegacyObject, List<String>> code = new LinkedHashMap<>();
        for (Group group : groups) {
            List<String> lines = code.computeIfAbsent(group.button, b -> new ArrayList<>());
            if (group.target.startsWith("Map#")) {
                for (String[] command : group.commands) {
                    lines.add(setTile(command[0], command[2]));
                }
                continue;
            }
            LegacyObject object = target(group);
            if (object == null) {
                continue;
            }
            translateGroup(group, object, lines);
        }
        int unnamed = 0;
        for (Map.Entry<LegacyObject, List<String>> entry : code.entrySet()) {
            LegacyObject button = entry.getKey();
            List<String> lines = entry.getValue();
            if (lines.isEmpty()) {
                report.note(levelId, "кнопка " + point(button.x, button.y) + " ничего не делает");
                continue;
            }
            if (lines.size() <= INLINE_HANDLER_LINES) {
                List<String> rendered = new ArrayList<>();
                for (String line : lines) {
                    rendered.add(render(line, button, true));
                }
                button.handlers.put("onUse", rendered.size() == 1 ? rendered.get(0) : rendered);
            } else {
                if (button.id == null) {
                    button.id = "button" + ++unnamed;
                }
                String name = button.id + "Pressed";
                script.append("\ndef ").append(name).append("() {\n");
                for (String line : lines) {
                    script.append("    ").append(render(line, button, false)).append('\n');
                }
                script.append("}\n");
                button.handlers.put("onUse", name + "()");
            }
        }
    }

    /** Метки ссылки на объект в строке кода: между ними id, пусто — сама кнопка. */
    private static final char SELF_START = '\u0001';

    private static final char SELF_END = '\u0002';

    /** Метка кавычки: в JSON удобнее одинарные, в файле .dgs — двойные. */
    private static final char QUOTE = '\u0003';

    /**
     * Подставляет ссылки: в обработчике прямо в уровне кнопка — {@code self}, в функции файла
     * скрипта {@code self} нет, поэтому {@code entity("id")}.
     */
    private static String render(String line, LegacyObject button, boolean inline) {
        String quote = inline ? "'" : "\"";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == SELF_START) {
                int end = line.indexOf(SELF_END, i);
                String id = line.substring(i + 1, end);
                if (id.isEmpty() && inline) {
                    sb.append("self");
                } else {
                    sb.append("entity(")
                            .append(quote)
                            .append(id.isEmpty() ? button.id : id)
                            .append(quote)
                            .append(')');
                }
                i = end + 1;
                continue;
            }
            sb.append(c == QUOTE ? quote : String.valueOf(c));
            i++;
        }
        return sb.toString();
    }

    private void translateGroup(Group group, LegacyObject object, List<String> lines) {
        String ref = SELF_START + (object == group.button ? "" : object.id) + SELF_END;
        if (object.parkedBy == group) {
            lines.add(ref + ".enable()");
            for (String[] command : group.commands) {
                if (command[0].equals("Stage")) {
                    object.params.put("cracks", Integer.parseInt(command[1]));
                }
            }
            return;
        }
        Integer nx = group.value("X");
        Integer ny = group.value("Y");
        boolean moves = nx != null || ny != null;
        int x = nx != null ? nx : object.x;
        int y = ny != null ? ny : object.y;
        boolean wasIn = inMap(object.x, object.y);
        boolean nowIn = inMap(x, y);
        if (moves && wasIn && !nowIn) {
            lines.add(ref + ".disable()");
            return;
        }
        if (moves && nowIn && (x != object.x || y != object.y)) {
            List<Integer> p = point(x, y);
            lines.add(ref + ".moveTo(" + p.get(0) + ", " + p.get(1) + ")");
            if (!object.enabled) {
                lines.add(ref + ".enable()");
            }
        }
        for (String[] command : group.commands) {
            switch (command[0]) {
                case "X", "Y", "cordN" -> {}
                case "Stage" -> {
                    if (object.kind.equals("FlimsyTile")) {
                        lines.add(ref + ".cracks = " + command[1]);
                    } else {
                        report.note(
                                levelId,
                                "команда `"
                                        + command[2]
                                        + "` не переведена: Stage поддерживалась только у FlimsyTile");
                    }
                }
                case "del" -> lines.add(ref + ".remove()");
                default ->
                        report.note(
                                levelId,
                                "команда `"
                                        + command[2]
                                        + "` в DarGon3 не работала (неизвестная операция) — пропущена");
            }
        }
    }

    /** {@code map-x,y>tile} → {@code setTile(x, y', "floor")}. */
    private String setTile(String command, String raw) {
        int arrow = command.indexOf('>');
        String[] xy = command.substring("map-".length(), arrow).split(",");
        int tile = Integer.parseInt(command.substring(arrow + 1));
        List<Integer> p = point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
        if (!inMap(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]))) {
            report.note(levelId, "команда `" + raw + "` меняет клетку вне карты");
        }
        return "setTile("
                + p.get(0)
                + ", "
                + p.get(1)
                + ", "
                + QUOTE
                + (tile == LEGACY_FLOOR ? "floor" : "wall")
                + QUOTE
                + ")";
    }
}
