package com.zaig100.dargon.tools.migrator;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.JsonPrinter;
import com.zaig100.dargon.level.LevelFormatException;
import com.zaig100.dargon.level.Problem;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Одноразовый конвертер уровней DarGon3 в новый формат.
 *
 * <pre>
 * level-migrator &lt;старые уровни&gt; &lt;assets&gt; [отчёт.md]
 * </pre>
 *
 * Пишет во встроенный пак {@code packs/dargon/}: {@code levels/*.json} (и {@code *.dgs} для длинных
 * обработчиков кнопок) и тексты {@code i18n/pack*.properties}, затем проверяет результат
 * загрузчиком. Список уровней в {@code pack.json} ведётся вручную: мигратор только проверяет, что
 * все уровни в нём есть. Внимание: тексты пака перезаписываются целиком (названия достижений —
 * тоже).
 */
public final class LevelMigrator {

    private static final Pattern NUMBERED = Pattern.compile("(\\d\\d)\\.json");
    private static final Pattern SHOP = Pattern.compile("shops/(shop_\\d\\d)\\.json");
    private static final Pattern TEST = Pattern.compile("Test(\\d?)\\.json");

    private LevelMigrator() {}

    @SuppressWarnings("checkstyle:RegexpSinglelineJava") // CLI-утилита пишет в консоль
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(
                    "usage: level-migrator <legacy levels dir> <assets dir> [report.md]");
            System.exit(2);
        }
        Path legacy = Path.of(args[0]);
        Path assets = Path.of(args[1]);

        Map<String, String> ids = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(legacy)) {
            files.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> legacy.relativize(p).toString().replace('\\', '/'))
                    .sorted(LevelMigrator::compareLegacy)
                    .forEach(path -> ids.put(path, newId(path)));
        }

        Report report = new Report();
        TextBundle texts = new TextBundle();
        Path packDir = assets.resolve(GameData.builtinDir(GameData.BUILTIN_PACK));
        Path levelsDir = packDir.resolve(GameData.LEVELS_DIR);
        for (Map.Entry<String, String> entry : ids.entrySet()) {
            JsonValue root =
                    new JsonReader()
                            .parse(
                                    Files.readString(
                                            legacy.resolve(entry.getKey()),
                                            StandardCharsets.UTF_8));
            String id = entry.getValue();
            LegacyConverter.Result result =
                    new LegacyConverter(
                                    root,
                                    id,
                                    path -> ids.get(path.replaceFirst("^levels/", "")),
                                    report,
                                    texts)
                            .convert();
            Path out = levelsDir.resolve(id + ".json");
            Files.createDirectories(out.getParent());
            Files.writeString(out, JsonPrinter.write(result.level()), StandardCharsets.UTF_8);
            if (result.script() != null) {
                Files.writeString(
                        out.resolveSibling(out.getFileName().toString().replace(".json", ".dgs")),
                        result.script(),
                        StandardCharsets.UTF_8);
            }
        }
        for (String key : texts.english.keySet()) {
            if (!key.startsWith("level.dev.") && !Texts.hasRussian(texts.english.get(key))) {
                report.note(
                        "локализация",
                        "нет перевода для `"
                                + key
                                + "`: "
                                + texts.english.get(key).replace("\n", " / "));
            }
        }
        Path i18n = packDir.resolve("i18n");
        Files.createDirectories(i18n);
        Files.writeString(
                i18n.resolve("pack.properties"),
                TextBundle.toProperties(texts.english, "Тексты уровней (English)."),
                StandardCharsets.UTF_8);
        Files.writeString(
                i18n.resolve("pack_ru.properties"),
                TextBundle.toProperties(texts.russian, "Тексты уровней (русский)."),
                StandardCharsets.UTF_8);

        int errors = verify(assets, new ArrayList<>(ids.values()));
        String markdown = report.toMarkdown();
        if (args.length > 2) {
            Files.writeString(Path.of(args[2]), markdown, StandardCharsets.UTF_8);
        } else {
            System.out.println(markdown);
        }
        System.out.println("Смигрировано уровней: " + ids.size() + ", ошибок проверки: " + errors);
        if (errors > 0) {
            System.exit(1);
        }
    }

    /** 01…27, затем лавки, затем общий уровень, затем тестовые. */
    private static int compareLegacy(String a, String b) {
        return (rank(a) + a).compareTo(rank(b) + b);
    }

    private static String rank(String path) {
        if (NUMBERED.matcher(path).matches()) {
            return "0";
        }
        if (SHOP.matcher(path).matches()) {
            return "1";
        }
        return path.startsWith("AllLevels") ? "2" : "3";
    }

    static String newId(String legacyPath) {
        Matcher m = NUMBERED.matcher(legacyPath);
        if (m.matches()) {
            return m.group(1);
        }
        m = SHOP.matcher(legacyPath);
        if (m.matches()) {
            return "shops/" + m.group(1);
        }
        if (legacyPath.equals("AllLevels.json")) {
            return "hub";
        }
        m = TEST.matcher(legacyPath);
        if (m.matches()) {
            return "dev/test" + (m.group(1).isEmpty() ? "1" : m.group(1));
        }
        throw new IllegalArgumentException("Неизвестный старый уровень: " + legacyPath);
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava") // CLI-утилита пишет в консоль
    private static int verify(Path assets, List<String> levels) {
        GameData data =
                GameData.loadBuiltin(
                        path -> {
                            try {
                                Path file = assets.resolve(path);
                                return Files.exists(file)
                                        ? Files.readString(file, StandardCharsets.UTF_8)
                                        : null;
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
        int errors = 0;
        for (String id : levels) {
            if (!data.levels().contains(id)) {
                System.out.println("уровня '" + id + "' нет в списке levels pack.json");
                errors++;
                continue;
            }
            try {
                data.loadLevel(id);
            } catch (LevelFormatException e) {
                for (Problem problem : e.problems()) {
                    System.out.println(problem);
                }
                errors += (int) e.problems().stream().filter(Problem::isError).count();
            }
        }
        return errors;
    }
}
