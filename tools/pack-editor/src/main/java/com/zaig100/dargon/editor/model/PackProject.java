package com.zaig100.dargon.editor.model;

import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.JsonPrinter;
import com.zaig100.dargon.level.PackDefinition;
import com.zaig100.dargon.level.ParamSpec;
import com.zaig100.dargon.level.ParamType;
import com.zaig100.dargon.level.Prefab;
import com.zaig100.dargon.level.PrefabLibrary;
import com.zaig100.dargon.script.ScriptEngine;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Пак, открытый в редакторе: каталог с {@code pack.json}, уровнями {@code levels/} и текстами
 * {@code i18n/pack*.properties} (en — базовый файл, ru — {@code _ru}). Изменения живут в памяти до
 * {@link #save()}; несохранённые уровни видны проверке и пробному запуску через {@link #reader()}.
 */
public final class PackProject {

    /** Языки текстов: код → суффикс файла. */
    public static final Map<String, String> LANGUAGES = Map.of("en", "", "ru", "_ru");

    private final Path dir;
    private Map<String, Object> pack;
    private final Map<String, LevelDoc> levels = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> prefabs = new LinkedHashMap<>();
    private boolean prefabsDirty;
    private final Map<String, TextTable> texts = new LinkedHashMap<>();
    private final List<Path> deleted = new ArrayList<>();
    private boolean packDirty;
    private boolean textsDirty;

    /** Скрипты уровней ({@code .dgs}): путь относительно каталога уровней → текст. */
    private final Map<String, String> scripts = new LinkedHashMap<>();

    private final java.util.Set<String> dirtyScripts = new java.util.LinkedHashSet<>();

    private PackProject(Path dir) {
        this.dir = dir;
    }

    public Path dir() {
        return dir;
    }

    // ------------------------------------------------------------------ открыть и создать

    public static PackProject open(Path dir) throws IOException {
        PackProject p = new PackProject(dir);
        String source = read(dir.resolve(PackDefinition.FILE));
        if (source == null) {
            throw new IOException("в " + dir + " нет " + PackDefinition.FILE);
        }
        p.pack = Json.parseObject(source);
        for (String id : p.levelIds()) {
            String level = read(dir.resolve(GameData.levelPath(id)));
            p.levels.put(id, level == null ? null : new LevelDoc(id, Json.parseObject(level)));
        }
        for (String lang : LANGUAGES.keySet()) {
            p.texts.put(lang, TextTable.parse(read(dir.resolve(textFile(lang)))));
        }
        for (String id : p.names("prefabs")) {
            String prefab = read(dir.resolve(prefabPath(id)));
            if (prefab != null) {
                p.prefabs.put(id, Json.parseObject(prefab));
            }
        }
        return p;
    }

    /** Новый пак в пустом каталоге: один уровень, тексты на двух языках. */
    public static PackProject create(Path dir, String id, String name) throws IOException {
        Files.createDirectories(dir);
        PackProject p = new PackProject(dir);
        p.pack = new LinkedHashMap<>();
        p.pack.put("format", (long) PackDefinition.FORMAT);
        p.pack.put("id", id);
        p.pack.put("name", "@pack.name");
        p.pack.put("description", "@pack.description");
        p.pack.put("author", System.getProperty("user.name", ""));
        p.pack.put("version", "1.0");
        p.pack.put("game", "4.0.0");
        p.pack.put("start", "01");
        p.pack.put("levels", new ArrayList<>(List.of("01")));
        Map<String, Object> modes = new LinkedHashMap<>();
        for (String mode : PackDefinition.MODES) {
            modes.put(mode, new LinkedHashMap<>(Map.of("start", new ArrayList<>())));
        }
        p.pack.put("modes", modes);
        p.pack.put("achievements", new ArrayList<>());
        for (String lang : LANGUAGES.keySet()) {
            p.texts.put(lang, new TextTable());
        }
        p.text("en", "pack.name", name);
        p.text("ru", "pack.name", name);
        p.text("en", "pack.description", "");
        p.text("ru", "pack.description", "");
        p.levels.put("01", LevelDoc.create("01", 7, 5, p.schemaPath("01")));
        p.text("en", "level.01.name", "Level 1");
        p.text("ru", "level.01.name", "Уровень 1");
        p.packDirty = true;
        p.textsDirty = true;
        p.save();
        return p;
    }

    // ------------------------------------------------------------------ пак

    public Map<String, Object> pack() {
        return pack;
    }

    public String id() {
        return String.valueOf(pack.getOrDefault("id", ""));
    }

    public void packChanged() {
        packDirty = true;
    }

    @SuppressWarnings("unchecked")
    public List<String> levelIds() {
        List<String> ids = new ArrayList<>();
        if (pack.get("levels") instanceof List<?> list) {
            for (Object o : list) {
                ids.add(String.valueOf(o));
            }
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private List<Object> levelList() {
        return (List<Object>) pack.computeIfAbsent("levels", k -> new ArrayList<>());
    }

    /** Уровень по id; {@code null}, если файла нет. */
    public LevelDoc level(String id) {
        return levels.get(id);
    }

    /** Новый уровень после {@code after} (или в конце). */
    public LevelDoc addLevel(String id, String after) {
        if (levels.containsKey(id)) {
            throw new IllegalArgumentException("уровень '" + id + "' уже есть");
        }
        LevelDoc doc = LevelDoc.create(id, 7, 5, schemaPath(id));
        levels.put(id, doc);
        int index = after == null ? -1 : levelList().indexOf(after);
        levelList().add(index < 0 ? levelList().size() : index + 1, id);
        text("en", "level." + id + ".name", id);
        text("ru", "level." + id + ".name", id);
        packDirty = true;
        return doc;
    }

    /** Удаляет уровень из пака (файл — при сохранении). */
    public void removeLevel(String id) {
        LevelDoc doc = levels.remove(id);
        levelList().remove(id);
        if (doc != null) {
            dropScript(levelScript(doc));
        }
        deleted.add(dir.resolve(GameData.levelPath(id)));
        packDirty = true;
    }

    /** Сдвигает уровень в списке на {@code delta}. */
    public void moveLevel(String id, int delta) {
        List<Object> list = levelList();
        int i = list.indexOf(id);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= list.size()) {
            return;
        }
        list.set(i, list.get(j));
        list.set(j, id);
        packDirty = true;
    }

    /**
     * Переименовывает уровень и все ссылки на него: список и старт пака, параметры-уровни
     * (лестницы) во всех уровнях, достижения.
     */
    public void renameLevel(String oldId, String newId, PrefabLibrary prefabs) {
        if (levels.containsKey(newId)) {
            throw new IllegalArgumentException("уровень '" + newId + "' уже есть");
        }
        LevelDoc doc = levels.remove(oldId);
        String oldScript = levelScript(doc);
        doc.rename(newId);
        Map<String, LevelDoc> reordered = new LinkedHashMap<>();
        levels.put(newId, doc);
        List<Object> list = levelList();
        list.set(list.indexOf(oldId), newId);
        for (String id : levelIds()) {
            reordered.put(id, levels.get(id));
        }
        levels.clear();
        levels.putAll(reordered);
        if (oldId.equals(pack.get("start"))) {
            pack.put("start", newId);
        }
        String schema = schemaPath(newId);
        if (schema != null && doc.root().containsKey("$schema")) {
            doc.root().put("$schema", schema);
        }
        deleted.add(dir.resolve(GameData.levelPath(oldId)));
        for (LevelDoc level : levels.values()) {
            if (level != null && replaceLevelRefs(level, oldId, newId, prefabs)) {
                level.changed();
            }
        }
        replaceInPack(pack.get("achievements"), oldId, newId);
        moveScript(doc, oldScript);
        packDirty = true;
    }

    /**
     * Уровень переехал в другой каталог ({@code 18} → {@code shops/18}): путь скрипта считается от
     * каталога уровня — файл переезжает следом (если имя занято — по имени уровня).
     */
    private void moveScript(LevelDoc doc, String oldPath) {
        String text = oldPath == null ? null : script(oldPath);
        if (text == null || oldPath.equals(levelScript(doc))) {
            return;
        }
        String name = (String) doc.root().get("script");
        String leaf = doc.id().substring(doc.id().lastIndexOf('/') + 1);
        for (int i = 1; script(scriptPath(doc.id(), name)) != null; i++) {
            name = leaf + (i == 1 ? "" : "_" + i) + ".dgs";
        }
        doc.root().put("script", name);
        doc.changed();
        script(scriptPath(doc.id(), name), text);
        dropScript(oldPath);
    }

    /** Путь скрипта уровня относительно каталога уровней или {@code null}. */
    private static String levelScript(LevelDoc doc) {
        return doc.root().get("script") instanceof String name && !name.isBlank()
                ? scriptPath(doc.id(), name)
                : null;
    }

    /**
     * Скрипт больше не нужен уровню: удалить файл при сохранении, если он не скрипт другого уровня
     * и его не подключают через {@code use}.
     */
    private void dropScript(String path) {
        if (path == null || script(path) == null) {
            return;
        }
        for (LevelDoc level : levels.values()) {
            if (level != null && path.equals(levelScript(level))) {
                return;
            }
        }
        if (usedModules().contains(path)) {
            return;
        }
        scripts.remove(path);
        dirtyScripts.remove(path);
        deleted.add(dir.resolve(GameData.LEVELS_DIR + path));
    }

    /** Файлы .dgs, подключённые через {@code use} в скриптах и обработчиках пака. */
    private java.util.Set<String> usedModules() {
        List<String> sources = new ArrayList<>();
        for (LevelDoc level : levels.values()) {
            if (level == null) {
                continue;
            }
            String own = levelScript(level);
            if (own != null && script(own) != null) {
                sources.add(script(own));
            }
            for (Map<String, Object> e : level.entities()) {
                for (Object value : e.values()) {
                    if (value instanceof String text) {
                        sources.add(text);
                    } else if (value instanceof List<?> lines) {
                        StringBuilder sb = new StringBuilder();
                        for (Object line : lines) {
                            sb.append(line).append('\n');
                        }
                        sources.add(sb.toString());
                    }
                }
            }
        }
        for (String shared : sharedScripts()) {
            sources.add(script(shared));
        }
        java.util.Set<String> used = new java.util.HashSet<>();
        for (String source : sources) {
            for (String module : ScriptEngine.uses(source == null ? "" : source, "")) {
                used.add(module + ".dgs");
            }
        }
        return used;
    }

    private static boolean replaceLevelRefs(
            LevelDoc level, String oldId, String newId, PrefabLibrary prefabs) {
        boolean changed = false;
        for (Map<String, Object> e : level.entities()) {
            Prefab prefab = prefabs.get(String.valueOf(e.get("prefab")));
            if (prefab == null) {
                continue;
            }
            for (ParamSpec spec : prefab.params().values()) {
                if (spec.type() == ParamType.LEVEL && oldId.equals(e.get(spec.name()))) {
                    e.put(spec.name(), newId);
                    changed = true;
                }
            }
        }
        return changed;
    }

    @SuppressWarnings("unchecked")
    private static void replaceInPack(Object node, String oldId, String newId) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) map).entrySet()) {
                if (oldId.equals(e.getValue())
                        && List.of("level", "since", "unlockStart").contains(e.getKey())) {
                    e.setValue(newId);
                } else {
                    replaceInPack(e.getValue(), oldId, newId);
                }
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                replaceInPack(item, oldId, newId);
            }
        }
    }

    /**
     * {@code $schema} для уровня — по образцу других уровней пака с поправкой на вложенность
     * ({@code shops/…}); {@code null}, если образца нет.
     */
    public String schemaPath(String levelId) {
        for (LevelDoc level : levels.values()) {
            if (level != null && level.root().get("$schema") instanceof String schema) {
                int depth = level.id().split("/").length;
                String prefix = "../".repeat(depth);
                if (schema.startsWith(prefix)) {
                    return "../".repeat(levelId.split("/").length)
                            + schema.substring(prefix.length());
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ префабы и текстуры

    @SuppressWarnings("unchecked")
    private List<Object> list(String key) {
        return (List<Object>) pack.computeIfAbsent(key, k -> new ArrayList<>());
    }

    private List<String> names(String key) {
        List<String> result = new ArrayList<>();
        if (pack.get(key) instanceof List<?> l) {
            for (Object o : l) {
                result.add(String.valueOf(o));
            }
        }
        return result;
    }

    public static String prefabPath(String id) {
        return GameData.PACK_PREFABS_DIR + id + ".json";
    }

    public static String texturePath(String name) {
        return GameData.PACK_TEXTURES_DIR + name + ".png";
    }

    /** Свои префабы пака: id → JSON префаба. */
    public Map<String, Map<String, Object>> prefabs() {
        return prefabs;
    }

    /** Заменяет все префабы пака (диалог префабов: «Применить»); список в pack.json — тоже. */
    public void setPrefabs(Map<String, Map<String, Object>> next) {
        for (String id : prefabs.keySet()) {
            if (!next.containsKey(id)) {
                deleted.add(dir.resolve(prefabPath(id)));
            }
        }
        prefabs.clear();
        prefabs.putAll(next);
        List<Object> ids = list("prefabs");
        ids.clear();
        ids.addAll(next.keySet());
        if (ids.isEmpty()) {
            pack.remove("prefabs");
        }
        prefabsDirty = true;
        packDirty = true;
    }

    /** Переименование префаба в уровнях: сущности с {@code "prefab": old} получают новый id. */
    public void renamePrefabInLevels(String old, String next) {
        for (LevelDoc level : levels.values()) {
            boolean changed = false;
            if (level == null) {
                continue;
            }
            for (Map<String, Object> e : level.entities()) {
                if (old.equals(e.get("prefab"))) {
                    e.put("prefab", next);
                    changed = true;
                }
            }
            if (changed) {
                level.changed();
            }
        }
    }

    /** Свои текстуры пака (имена без .png). */
    public List<String> textures() {
        return names("textures");
    }

    /** Копирует PNG в textures/ пака под именем {@code name} (заменяет, если было). */
    public void importTexture(Path png, String name) throws IOException {
        Path target = dir.resolve(texturePath(name));
        Files.createDirectories(target.getParent());
        Files.copy(png, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        List<Object> names = list("textures");
        if (!names.contains(name)) {
            names.add(name);
        }
        packDirty = true;
    }

    /** Убирает текстуру из пака (файл удалится при сохранении). */
    public void removeTexture(String name) {
        list("textures").remove(name);
        if (list("textures").isEmpty()) {
            pack.remove("textures");
        }
        deleted.add(dir.resolve(texturePath(name)));
        packDirty = true;
    }

    /** PNG текстуры пака или {@code null}. */
    public byte[] textureBytes(String name) {
        Path file = dir.resolve(texturePath(name));
        try {
            return Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Текст файла префаба: поля в привычном порядке. */
    public static String prefabJson(Map<String, Object> prefab) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key :
                List.of(
                        "$schema",
                        "id",
                        "description",
                        "placement",
                        "events",
                        "params",
                        "components")) {
            if (prefab.containsKey(key)) {
                out.put(key, prefab.get(key));
            }
        }
        for (Map.Entry<String, Object> e : prefab.entrySet()) {
            out.putIfAbsent(e.getKey(), e.getValue());
        }
        return JsonPrinter.write(out);
    }

    // ------------------------------------------------------------------ тексты

    public static String textFile(String lang) {
        return "i18n/pack" + LANGUAGES.get(lang) + ".properties";
    }

    public TextTable texts(String lang) {
        return texts.get(lang);
    }

    /** Текст по ключу (без {@code @}); {@code null}, если нет. */
    public String text(String lang, String key) {
        return texts.get(lang).get(key);
    }

    public void text(String lang, String key, String value) {
        texts.get(lang).put(key, value);
        textsDirty = true;
    }

    public void removeText(String lang, String key) {
        texts.get(lang).remove(key);
        textsDirty = true;
    }

    /** Свободный ключ {@code base}, {@code base_2}, … */
    public String freeKey(String base) {
        String key = base;
        for (int i = 2; texts.get("en").contains(key) || texts.get("ru").contains(key); i++) {
            key = base + "_" + i;
        }
        return key;
    }

    // ------------------------------------------------------------------ скрипты

    /**
     * Путь скрипта относительно каталога уровней, как у игры: скрипт {@code shop.dgs} уровня {@code
     * shops/shop_05} — {@code shops/shop.dgs}.
     */
    public static String scriptPath(String levelId, String script) {
        int slash = levelId.lastIndexOf('/');
        return slash < 0 ? script : levelId.substring(0, slash + 1) + script;
    }

    /** Текст скрипта по пути относительно каталога уровней; файла нет — {@code null}. */
    public String script(String path) {
        String text = scripts.get(path);
        if (text == null) {
            text = read(dir.resolve(GameData.LEVELS_DIR + path));
            if (text != null) {
                scripts.put(path, text);
            }
        }
        return text;
    }

    /**
     * Общие скрипты пака — файлы .dgs, которые не служат скриптом ни одного уровня (их подключают
     * через {@code use}): пути относительно каталога уровней, по алфавиту.
     */
    public List<String> sharedScripts() {
        java.util.Set<String> all = new java.util.TreeSet<>(scripts.keySet());
        Path levelsDir = dir.resolve(GameData.LEVELS_DIR);
        if (Files.isDirectory(levelsDir)) {
            try (java.util.stream.Stream<Path> files = Files.walk(levelsDir)) {
                files.filter(f -> f.toString().endsWith(".dgs") && Files.isRegularFile(f))
                        .forEach(
                                f ->
                                        all.add(
                                                levelsDir
                                                        .relativize(f)
                                                        .toString()
                                                        .replace('\\', '/')));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        for (LevelDoc level : levels.values()) {
            if (level != null && level.root().get("script") instanceof String name) {
                all.remove(scriptPath(level.id(), name));
            }
        }
        return new ArrayList<>(all);
    }

    /** Новый текст скрипта (файл создаётся при сохранении). */
    public void script(String path, String text) {
        if (!text.equals(scripts.get(path))) {
            scripts.put(path, text);
            dirtyScripts.add(path);
        }
    }

    // ------------------------------------------------------------------ сохранение

    public boolean isDirty() {
        if (packDirty
                || textsDirty
                || prefabsDirty
                || !deleted.isEmpty()
                || !dirtyScripts.isEmpty()) {
            return true;
        }
        for (LevelDoc level : levels.values()) {
            if (level != null && level.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public void save() throws IOException {
        for (Path path : deleted) {
            boolean stillUsed = false;
            for (String id : levelIds()) {
                stillUsed |= dir.resolve(GameData.levelPath(id)).equals(path);
            }
            for (String id : prefabs.keySet()) {
                stillUsed |= dir.resolve(prefabPath(id)).equals(path);
            }
            for (String name : textures()) {
                stillUsed |= dir.resolve(texturePath(name)).equals(path);
            }
            for (String script : scripts.keySet()) {
                stillUsed |= dir.resolve(GameData.LEVELS_DIR + script).equals(path);
            }
            if (!stillUsed) {
                Files.deleteIfExists(path);
            }
        }
        deleted.clear();
        write(dir.resolve(PackDefinition.FILE), packJson());
        for (LevelDoc level : levels.values()) {
            if (level != null && level.isDirty()) {
                write(dir.resolve(GameData.levelPath(level.id())), level.toJson());
                level.markSaved();
            }
        }
        for (Map.Entry<String, TextTable> e : texts.entrySet()) {
            write(dir.resolve(textFile(e.getKey())), e.getValue().write());
        }
        if (prefabsDirty) {
            for (Map.Entry<String, Map<String, Object>> e : prefabs.entrySet()) {
                write(dir.resolve(prefabPath(e.getKey())), prefabJson(e.getValue()));
            }
            prefabsDirty = false;
        }
        for (String path : dirtyScripts) {
            write(dir.resolve(GameData.LEVELS_DIR + path), scripts.get(path));
        }
        dirtyScripts.clear();
        packDirty = false;
        textsDirty = false;
    }

    /** Текст {@code pack.json}: метаданные, уровни по одному на строку. */
    public String packJson() {
        Map<String, Object> out = new LinkedHashMap<>(pack);
        if (out.get("levels") instanceof List<?> list) {
            out.put("levels", new JsonPrinter.Multiline(list));
        }
        if (out.get("achievements") instanceof List<?> list && !list.isEmpty()) {
            out.put("achievements", new JsonPrinter.Multiline(list));
        }
        return JsonPrinter.write(out);
    }

    /**
     * Файлы пака с несохранёнными правками: проверка и пробный запуск видят то, что в редакторе.
     */
    public AssetReader reader() {
        return path -> {
            if (path.equals(PackDefinition.FILE)) {
                return packJson();
            }
            for (LevelDoc level : levels.values()) {
                if (level != null && path.equals(GameData.levelPath(level.id()))) {
                    return level.toJson();
                }
            }
            for (String lang : LANGUAGES.keySet()) {
                if (path.equals(textFile(lang))) {
                    return texts.get(lang).write();
                }
            }
            for (Map.Entry<String, Map<String, Object>> e : prefabs.entrySet()) {
                if (path.equals(prefabPath(e.getKey()))) {
                    return prefabJson(e.getValue());
                }
            }
            if (path.startsWith(GameData.LEVELS_DIR)
                    && scripts.containsKey(path.substring(GameData.LEVELS_DIR.length()))) {
                return scripts.get(path.substring(GameData.LEVELS_DIR.length()));
            }
            return read(dir.resolve(path));
        };
    }

    private static String read(Path file) {
        try {
            return Files.isRegularFile(file)
                    ? Files.readString(file, StandardCharsets.UTF_8)
                    : null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        Files.move(
                temp,
                file,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }
}
