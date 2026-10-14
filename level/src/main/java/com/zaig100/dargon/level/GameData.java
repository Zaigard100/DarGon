package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Данные для игры: движок ({@code data/}: предметы и префабы из {@code data/manifest.json}) и один
 * пак ({@code pack.json}, уровни из его {@code levels/}). Движок и пак читаются разными {@link
 * AssetReader}: у пака корень — его папка или архив.
 */
public final class GameData {

    public static final String MANIFEST = "data/manifest.json";
    public static final String ITEMS = "data/items.json";
    public static final String PREFABS_DIR = "data/prefabs/";

    /** Встроенный пак — оригинальная кампания. */
    public static final String BUILTIN_PACK = "dargon";

    /** Каталог своих префабов внутри пака. */
    public static final String PACK_PREFABS_DIR = "prefabs/";

    /** Каталог своих текстур внутри пака. */
    public static final String PACK_TEXTURES_DIR = "textures/";

    /** Каталог уровней внутри пака. */
    public static final String LEVELS_DIR = "levels/";

    private static final Set<String> KEYS = Set.of("$schema", "prefabs");

    private final AssetReader pack;
    private final ItemCatalog items;
    private final PrefabLibrary prefabs;
    private final LevelParser parser;
    private final ComponentCatalog components;
    private final List<String> packPrefabs = new ArrayList<>();
    private PackDefinition definition;

    private GameData(
            AssetReader pack,
            ItemCatalog items,
            PrefabLibrary prefabs,
            ComponentCatalog components) {
        this.pack = pack;
        this.items = items;
        this.prefabs = prefabs;
        this.components = components;
        this.parser = new LevelParser(prefabs);
    }

    /**
     * Загружает движок (манифест, предметы, префабы) и пак (без уровней — они читаются по одному).
     *
     * @param engine файлы игры относительно {@code assets}
     * @param pack файлы пака относительно его корня
     * @throws LevelFormatException если что-то из этого некорректно
     */
    public static GameData load(AssetReader engine, AssetReader pack) {
        JsonValue manifest = JsonValues.parse(require(engine, MANIFEST), MANIFEST);
        Problems problems = new Problems(MANIFEST);
        if (!manifest.isObject()) {
            problems.error("", "ожидался объект");
            problems.throwIfErrors();
        }
        JsonValues.checkKeys(manifest, "", KEYS, problems);
        List<String> prefabIds = strings(manifest, "prefabs", problems);
        problems.throwIfErrors();

        ItemCatalog items = ItemCatalog.parse(require(engine, ITEMS), ITEMS);
        PrefabLibrary prefabs = new PrefabLibrary(items);
        for (String id : prefabIds) {
            String path = PREFABS_DIR + id + ".json";
            prefabs.add(require(engine, path), path);
        }
        ComponentCatalog components =
                ComponentCatalog.parse(
                        require(engine, ComponentCatalog.FILE), ComponentCatalog.FILE, items);
        GameData data = new GameData(pack, items, prefabs, components);
        data.definition =
                PackDefinition.parse(
                        require(pack, PackDefinition.FILE), PackDefinition.FILE, items);
        // Свои префабы пака: те же правила, что у игровых, плюс проверка компонентов по каталогу.
        for (String id : data.definition.prefabs()) {
            String path = PACK_PREFABS_DIR + id + ".json";
            if (prefabs.get(id) != null) {
                throw new LevelFormatException(
                        path, "id", "префаб '" + id + "' уже есть в игре — выберите другой id");
            }
            Prefab prefab = prefabs.add(require(pack, path), path);
            Problems prefabProblems = new Problems(path);
            components.validate(prefab, path, prefabProblems, items);
            prefabProblems.throwIfErrors();
            data.packPrefabs.add(id);
        }
        return data;
    }

    /** Движок и встроенный пак из одного каталога ассетов (тесты, утилиты). */
    public static GameData loadBuiltin(AssetReader assets) {
        return load(assets, assets.under(builtinDir(BUILTIN_PACK)));
    }

    private static String require(AssetReader reader, String path) {
        String source = reader.read(path);
        if (source == null) {
            throw new LevelFormatException(path, "", "файл не найден");
        }
        return source;
    }

    private static List<String> strings(JsonValue manifest, String key, Problems problems) {
        List<String> result = new ArrayList<>();
        JsonValue array = JsonValues.get(manifest, key);
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

    /** Компоненты, из которых собираются префабы. */
    public ComponentCatalog components() {
        return components;
    }

    /** Префабы, объявленные паком (а не игрой). */
    public List<String> packPrefabs() {
        return Collections.unmodifiableList(packPrefabs);
    }

    /** Загруженный пак. */
    public PackDefinition pack() {
        return definition;
    }

    public ItemCatalog items() {
        return items;
    }

    public PrefabLibrary prefabs() {
        return prefabs;
    }

    public LevelParser parser() {
        return parser;
    }

    /** Идентификаторы всех уровней пака в порядке {@code pack.json}. */
    public List<String> levels() {
        return definition.levels();
    }

    /** Первый уровень новой игры. */
    public String start() {
        return definition.start();
    }

    /** Каталог встроенного пака относительно {@code assets} (с {@code /} на конце). */
    public static String builtinDir(String id) {
        return "packs/" + id + "/";
    }

    /** Путь файла уровня относительно корня пака. */
    public static String levelPath(String id) {
        return LEVELS_DIR + id + ".json";
    }

    /**
     * Загружает и разбирает уровень.
     *
     * @throws LevelFormatException если файла нет или он некорректен
     */
    public LevelDefinition loadLevel(String id) {
        if (!levels().contains(id)) {
            throw new LevelFormatException(
                    PackDefinition.FILE, "levels", "уровня '" + id + "' нет в паке");
        }
        return parser.parse(require(pack, levelPath(id)), id);
    }

    /** Исходник скрипта уровня по пути относительно каталога уровней; {@code null}, если нет. */
    public String readScript(String path) {
        return pack.read(LEVELS_DIR + path);
    }
}
