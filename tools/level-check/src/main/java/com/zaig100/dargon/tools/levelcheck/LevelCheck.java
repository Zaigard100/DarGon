package com.zaig100.dargon.tools.levelcheck;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.zaig100.dargon.level.ComponentCatalog;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.JsonPrinter;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.level.LevelFormatException;
import com.zaig100.dargon.level.LevelSchema;
import com.zaig100.dargon.level.LevelValidator;
import com.zaig100.dargon.level.PackDefinition;
import com.zaig100.dargon.level.Problem;
import com.zaig100.dargon.level.Problems;
import com.zaig100.dargon.script.ScriptEngine;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Проверяет данные игры в каталоге {@code assets} и пак (по умолчанию встроенный {@code
 * assets/packs/dargon}, или любой каталог пака через {@code --pack}):
 *
 * <ul>
 *   <li>манифест, предметы, префабы, {@code pack.json} и уровни — по JSON Schema и загрузчиком
 *       игры;
 *   <li>схема уровня соответствует префабам ({@code --write-schema} перегенерирует её);
 *   <li>манифест и список уровней пака совпадают с файлами на диске;
 *   <li>ссылки на уровни и сущности, скрипты DGScript, достижимость выхода;
 *   <li>ключи локализации есть во всех языках и используются.
 * </ul>
 *
 * Код выхода 1, если есть ошибки. Предупреждения выводятся, но сборку не роняют.
 */
public final class LevelCheck {

    /** Языки: язык → суффикс файла в {@code i18n/} ({@code pack_ru.properties}). */
    private static final Map<String, String> LOCALES = Map.of("en", "", "ru", "_ru");

    /** Наборы текстов: пак (ключи проверяются по уровням) и интерфейс (ключи — в коде). */
    private static final String PACK_TEXTS = "pack";

    private static final String UI_TEXTS = "ui";

    private static final Pattern TEXT_KEY = Pattern.compile("\"@([A-Za-z0-9_.]+)\"");

    private static final String SCHEMA_BASE = "https://dargon.invalid/schemas/";

    private final Path assets;
    private final Path pack;

    /** Как показывать пути файлов пака в сообщениях: {@code packs/dargon/} или путь каталога. */
    private final String packPrefix;

    private final List<Problem> problems = new ArrayList<>();

    /**
     * Ошибки схемы по файлам. Показываются, только если загрузчик в файле ошибок не нашёл: его
     * сообщения понятнее, а схема нужна, чтобы поймать расхождение схемы и загрузчика.
     */
    private final Map<String, List<Problem>> schemaProblems = new LinkedHashMap<>();

    private LevelCheck(Path assets, Path pack) {
        this.assets = assets;
        this.pack = pack;
        String relative = assets.relativize(pack).toString().replace('\\', '/');
        this.packPrefix =
                (pack.startsWith(assets) ? relative : pack.toString().replace('\\', '/')) + "/";
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava") // CLI-утилита пишет в консоль
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println(
                    "usage: level-check <assets dir> [--pack <pack dir>] [--write-schema]");
            System.exit(2);
        }
        Path assets = Path.of(args[0]).toAbsolutePath().normalize();
        Path pack = assets.resolve(GameData.builtinDir(GameData.BUILTIN_PACK));
        boolean writeSchema = false;
        Iterator<String> rest = Arrays.asList(args).subList(1, args.length).iterator();
        while (rest.hasNext()) {
            String arg = rest.next();
            if (arg.equals("--write-schema")) {
                writeSchema = true;
            } else if (arg.equals("--pack") && rest.hasNext()) {
                pack = Path.of(rest.next()).toAbsolutePath().normalize();
            } else {
                System.err.println("неизвестный аргумент: " + arg);
                System.exit(2);
            }
        }
        LevelCheck check = new LevelCheck(assets, pack);
        check.run(writeSchema);
        int errors = 0;
        int warnings = 0;
        for (Problem problem : check.problems) {
            System.out.println(problem);
            if (problem.isError()) {
                errors++;
            } else {
                warnings++;
            }
        }
        System.out.println("level-check: ошибок " + errors + ", предупреждений " + warnings);
        if (errors > 0) {
            System.exit(1);
        }
    }

    private void run(boolean writeSchema) throws IOException {
        GameData data;
        try {
            data = GameData.load(this::read, this::readPack);
        } catch (LevelFormatException e) {
            problems.addAll(e.problems());
            return;
        }
        checkSchemaFile(data, writeSchema);
        checkAgainstSchemas(data);
        checkManifestMatchesDisk(data);

        Map<String, Properties> texts = loadTexts(pack, packPrefix, PACK_TEXTS);
        Set<String> usedKeys = new HashSet<>();
        Set<String> levels = new HashSet<>(data.levels());
        LevelValidator validator =
                new LevelValidator(
                        new LevelValidator.Context() {
                            @Override
                            public boolean levelExists(String id) {
                                return levels.contains(id);
                            }

                            @Override
                            public String scriptSource(String path) {
                                return data.readScript(path);
                            }

                            @Override
                            public Collection<String> locales() {
                                return texts.keySet();
                            }

                            @Override
                            public boolean hasText(String locale, String key) {
                                return texts.get(locale).containsKey(key);
                            }
                        });
        for (String id : data.levels()) {
            Problems levelProblems = new Problems(packPrefix + GameData.levelPath(id));
            String source = readPack(GameData.levelPath(id));
            LevelDefinition level =
                    source == null ? null : data.parser().parse(source, id, levelProblems);
            if (level != null) {
                validator.validate(level, levelProblems);
            }
            if (source != null) {
                Matcher matcher = TEXT_KEY.matcher(source);
                while (matcher.find()) {
                    usedKeys.add(matcher.group(1));
                }
            }
            problems.addAll(levelProblems.list());
            if (levelProblems.hasErrors()) {
                schemaProblems.remove(packPrefix + GameData.levelPath(id));
            }
        }
        // pack.json (название, описание, достижения) — тоже используемые ключи.
        Matcher matcher = TEXT_KEY.matcher(readPack(PackDefinition.FILE));
        while (matcher.find()) {
            usedKeys.add(matcher.group(1));
        }
        for (List<Problem> list : schemaProblems.values()) {
            problems.addAll(list);
        }
        checkTexts(packPrefix, PACK_TEXTS, texts, usedKeys);
        checkTexts("", UI_TEXTS, loadTexts(assets, "", UI_TEXTS), null);
    }

    // ------------------------------------------------------------------ схемы

    private void checkSchemaFile(GameData data, boolean write) throws IOException {
        String expected = JsonPrinter.write(LevelSchema.build(data));
        Path file = assets.resolve("data/level.schema.json");
        if (write) {
            Files.writeString(file, expected, StandardCharsets.UTF_8);
            return;
        }
        String actual = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : null;
        if (!expected.equals(actual)) {
            error(
                    "data/level.schema.json",
                    "схема устарела или отсутствует: запустите ./gradlew :tools:level-check:run --args=\"assets --write-schema\"");
        }
    }

    private void checkAgainstSchemas(GameData data) {
        // Схемы ссылаются друг на друга относительными $id; отдаём их из assets/data по
        // условному базовому адресу, без обращений к сети.
        SchemaRegistry registry =
                SchemaRegistry.withDefaultDialect(
                        SpecificationVersion.DRAFT_2020_12,
                        builder ->
                                builder.schemas(
                                        (String iri) ->
                                                iri.startsWith(SCHEMA_BASE)
                                                        ? read(
                                                                "data/"
                                                                        + iri.substring(
                                                                                SCHEMA_BASE
                                                                                        .length()))
                                                        : null));
        Schema manifest = schema(registry, "manifest.schema.json");
        Schema items = schema(registry, "items.schema.json");
        Schema prefab = schema(registry, "prefab.schema.json");
        Schema level = schema(registry, "level.schema.json");
        validate(
                schema(registry, "pack.schema.json"),
                packPrefix + PackDefinition.FILE,
                readPack(PackDefinition.FILE));
        validate(manifest, GameData.MANIFEST, read(GameData.MANIFEST));
        validate(items, GameData.ITEMS, read(GameData.ITEMS));
        validate(
                schema(registry, "components.schema.json"),
                ComponentCatalog.FILE,
                read(ComponentCatalog.FILE));
        for (String id : data.prefabs().ids()) {
            if (data.packPrefabs().contains(id)) {
                String path = GameData.PACK_PREFABS_DIR + id + ".json";
                validate(prefab, packPrefix + path, readPack(path));
            } else {
                String path = GameData.PREFABS_DIR + id + ".json";
                validate(prefab, path, read(path));
            }
        }
        for (String id : data.levels()) {
            validate(level, packPrefix + GameData.levelPath(id), readPack(GameData.levelPath(id)));
        }
    }

    private static Schema schema(SchemaRegistry registry, String name) {
        return registry.getSchema(SchemaLocation.of(SCHEMA_BASE + name));
    }

    private void validate(Schema schema, String path, String source) {
        if (source == null) {
            return;
        }
        for (Error error : schema.validate(source, InputFormat.JSON)) {
            String location = path(error.getInstanceLocation().toString());
            schemaProblems
                    .computeIfAbsent(path, p -> new ArrayList<>())
                    .add(
                            new Problem(
                                    Problem.Severity.ERROR,
                                    path,
                                    location,
                                    "схема: " + error.getMessage()));
        }
    }

    /** JSON Pointer {@code /entities/3/at} → путь в стиле загрузчика {@code entities[3].at}. */
    static String path(String pointer) {
        StringBuilder sb = new StringBuilder();
        for (String part : pointer.replaceFirst("^[$#]?/?", "").split("/")) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.matches("\\d+")) {
                sb.append('[').append(part).append(']');
            } else {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(part.replace("~1", "/").replace("~0", "~"));
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ манифест и пак

    private void checkManifestMatchesDisk(GameData data) throws IOException {
        Set<String> onDisk = new TreeSet<>();
        Set<String> scripts = new TreeSet<>();
        Path levelsDir = pack.resolve(GameData.LEVELS_DIR);
        String packFile = packPrefix + PackDefinition.FILE;
        try (Stream<Path> files = Files.walk(levelsDir)) {
            files.filter(Files::isRegularFile)
                    .forEach(
                            file -> {
                                String relative =
                                        levelsDir.relativize(file).toString().replace('\\', '/');
                                if (relative.endsWith(".json")) {
                                    onDisk.add(
                                            relative.substring(
                                                    0, relative.length() - ".json".length()));
                                } else if (relative.endsWith(".dgs")) {
                                    scripts.add(relative);
                                } else {
                                    warning(
                                            packPrefix + GameData.LEVELS_DIR + relative,
                                            "посторонний файл в каталоге уровней");
                                }
                            });
        }
        for (String id : onDisk) {
            if (!data.levels().contains(id)) {
                error(
                        packFile,
                        "уровень "
                                + GameData.levelPath(id)
                                + " есть на диске, но не указан в levels");
            }
        }
        for (String id : data.levels()) {
            if (!onDisk.contains(id)) {
                error(
                        packFile,
                        "уровень '"
                                + id
                                + "' указан в levels, но файла "
                                + GameData.levelPath(id)
                                + " нет");
            }
        }
        // Общие скрипты подключаются через use "имя" из уровней (обработчики) и других скриптов.
        Set<String> used = new TreeSet<>();
        List<String> sources = new ArrayList<>();
        for (String id : onDisk) {
            String level = readPack(GameData.levelPath(id));
            if (level != null) {
                try {
                    collectStrings(new JsonReader().parse(level), sources);
                } catch (RuntimeException broken) {
                    // битый JSON уже отмечен проверкой уровня
                }
            }
        }
        for (String script : scripts) {
            sources.add(readPack(GameData.LEVELS_DIR + script));
        }
        for (String source : sources) {
            for (String module : ScriptEngine.uses(source == null ? "" : source, "")) {
                used.add(module + ".dgs");
            }
        }
        for (String script : scripts) {
            String owner = script.substring(0, script.length() - ".dgs".length());
            String level = readPack(GameData.levelPath(owner));
            String name = script.substring(script.lastIndexOf('/') + 1);
            if (used.contains(script)) {
                continue;
            }
            if (level == null || !level.contains("\"" + name + "\"")) {
                warning(
                        packPrefix + GameData.LEVELS_DIR + script,
                        "скрипт не подключён ни к одному уровню");
            }
        }
        try (Stream<Path> files = Files.list(assets.resolve(GameData.PREFABS_DIR))) {
            files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .forEach(
                            n -> {
                                String id = n.substring(0, n.length() - ".json".length());
                                if (data.prefabs().get(id) == null) {
                                    error(
                                            GameData.MANIFEST,
                                            "префаб "
                                                    + GameData.PREFABS_DIR
                                                    + n
                                                    + " есть на диске, но не указан в prefabs");
                                }
                            });
        }
    }

    // ------------------------------------------------------------------ локализация

    private static String textFile(String bundle, String language) {
        return "i18n/" + bundle + LOCALES.get(language) + ".properties";
    }

    private Map<String, Properties> loadTexts(Path root, String prefix, String bundle) {
        Map<String, Properties> texts = new LinkedHashMap<>();
        for (String language : new TreeMap<>(LOCALES).keySet()) {
            Properties properties = new Properties();
            Path file = root.resolve(textFile(bundle, language));
            if (!Files.exists(file)) {
                error(prefix + textFile(bundle, language), "файл локализации не найден");
            } else {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            texts.put(language, properties);
        }
        return texts;
    }

    /**
     * Ключи есть во всех языках; если известны используемые ключи — лишние ключи дают
     * предупреждение.
     */
    private void checkTexts(
            String prefix, String bundle, Map<String, Properties> texts, Set<String> usedKeys) {
        Set<String> all = new TreeSet<>();
        for (Properties properties : texts.values()) {
            all.addAll(properties.stringPropertyNames());
        }
        for (String key : all) {
            for (Map.Entry<String, Properties> locale : texts.entrySet()) {
                if (!locale.getValue().containsKey(key)) {
                    error(
                            prefix + textFile(bundle, locale.getKey()),
                            "нет ключа '" + key + "', который есть в других языках");
                }
            }
            if (usedKeys != null && !usedKeys.contains(key)) {
                warning(
                        prefix + "i18n",
                        "ключ '"
                                + key
                                + "' не используется ни уровнями, ни "
                                + PackDefinition.FILE);
            }
        }
    }

    // ------------------------------------------------------------------ общее

    private String read(String path) {
        return read(assets, path);
    }

    /** Все строки JSON; массив строк (многострочный обработчик) — одной строкой. */
    private static void collectStrings(JsonValue value, List<String> out) {
        if (value.isString()) {
            out.add(value.asString());
            return;
        }
        if (value.isArray() && value.child != null && value.child.isString()) {
            StringBuilder sb = new StringBuilder();
            for (JsonValue line = value.child; line != null; line = line.next) {
                sb.append(line.isString() ? line.asString() : "").append('\n');
            }
            out.add(sb.toString());
            return;
        }
        for (JsonValue child = value.child; child != null; child = child.next) {
            collectStrings(child, out);
        }
    }

    private String readPack(String path) {
        return read(pack, path);
    }

    private static String read(Path root, String path) {
        Path file = root.resolve(path);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void error(String file, String message) {
        problems.add(new Problem(Problem.Severity.ERROR, file, "", message));
    }

    private void warning(String file, String message) {
        problems.add(new Problem(Problem.Severity.WARNING, file, "", message));
    }
}
