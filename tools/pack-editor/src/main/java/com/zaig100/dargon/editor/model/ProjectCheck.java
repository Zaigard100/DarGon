package com.zaig100.dargon.editor.model;

import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.level.LevelFormatException;
import com.zaig100.dargon.level.LevelValidator;
import com.zaig100.dargon.level.Problem;
import com.zaig100.dargon.level.Problems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Проверка пака, как при выборе в игре: {@code pack.json}, каждый уровень загрузчиком и {@link
 * LevelValidator} (ссылки, скрипты, достижимость выхода), тексты на обоих языках. Видит
 * несохранённые правки.
 */
public final class ProjectCheck {

    /** Проблема с привязкой: уровень ({@code null} — пак) и номер сущности ({@code -1} — нет). */
    public record Item(Problem problem, String level, int entity) {

        public boolean isError() {
            return problem.isError();
        }
    }

    private static final Pattern ENTITY = Pattern.compile("^entities\\[(\\d+)]");
    private static final Pattern TEXT_KEY = Pattern.compile("\"@([A-Za-z0-9_.]+)\"");

    private ProjectCheck() {}

    /**
     * @param engine файлы игры (предметы, префабы)
     */
    public static List<Item> run(AssetReader engine, PackProject project) {
        return run(engine, project, null);
    }

    /**
     * @param regionExists есть ли картинка с таким именем в игре ({@code null} — не проверять)
     */
    public static List<Item> run(
            AssetReader engine, PackProject project, Predicate<String> regionExists) {
        List<Item> result = new ArrayList<>();
        GameData data;
        try {
            data = GameData.load(engine, project.reader());
        } catch (LevelFormatException e) {
            for (Problem p : e.problems()) {
                result.add(new Item(p, null, -1));
            }
            return result;
        }
        Set<String> levels = new HashSet<>(data.levels());
        Set<String> usedKeys = new HashSet<>();
        collectKeys(project.packJson(), usedKeys);
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
                                return PackProject.LANGUAGES.keySet();
                            }

                            @Override
                            public boolean hasText(String locale, String key) {
                                return project.texts(locale).contains(key);
                            }
                        });
        for (String id : data.levels()) {
            LevelDoc doc = project.level(id);
            if (doc == null) {
                result.add(
                        new Item(
                                new Problem(
                                        Problem.Severity.ERROR,
                                        GameData.levelPath(id),
                                        "",
                                        "файла уровня нет"),
                                id,
                                -1));
                continue;
            }
            String source = doc.toJson();
            collectKeys(source, usedKeys);
            result.addAll(level(data, validator, id, source));
        }
        boolean levelErrors = hasErrors(result);
        for (String name : project.textures()) {
            byte[] png = project.textureBytes(name);
            if (png == null || png.length < 4 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
                result.add(
                        new Item(
                                new Problem(
                                        Problem.Severity.ERROR,
                                        PackProject.texturePath(name),
                                        "",
                                        png == null ? "файла нет" : "не PNG"),
                                null,
                                -1));
            }
        }
        if (regionExists != null) {
            for (String id : data.packPrefabs()) {
                JsonValue sprite = data.prefabs().get(id).components().get("SpriteRef");
                String region = sprite == null ? null : sprite.getString("region", null);
                if (region != null
                        && !region.startsWith("$")
                        && !regionExists.test(region)
                        && !project.textures().contains(region)) {
                    result.add(
                            new Item(
                                    new Problem(
                                            Problem.Severity.WARNING,
                                            PackProject.prefabPath(id),
                                            "components.SpriteRef.region",
                                            "нет картинки '" + region + "' — в игре будет «?»"),
                                    null,
                                    -1));
                }
            }
        }
        // Как при выборе пака в игре: каждый уровень пробно запускается.
        if (!levelErrors) {
            for (String id : data.levels()) {
                try {
                    LevelRuntime.start(
                                    data.loadLevel(id),
                                    PlayerState.newGame(data.items()),
                                    new ActionState())
                            .dispose();
                } catch (RuntimeException e) {
                    result.add(
                            new Item(
                                    new Problem(
                                            Problem.Severity.ERROR,
                                            GameData.levelPath(id),
                                            "",
                                            "уровень не запускается: " + e.getMessage()),
                                    id,
                                    -1));
                }
            }
        }
        for (String key : usedKeys) {
            for (String lang : PackProject.LANGUAGES.keySet()) {
                if (!project.texts(lang).contains(key)) {
                    result.add(
                            new Item(
                                    new Problem(
                                            Problem.Severity.WARNING,
                                            PackProject.textFile(lang),
                                            "",
                                            "нет текста '" + key + "' (" + lang + ")"),
                                    null,
                                    -1));
                }
            }
        }
        result.sort((a, b) -> Boolean.compare(b.isError(), a.isError()));
        return result;
    }

    /** Один уровень: разбор и проверки (для живой подсветки — без остального пака). */
    public static List<Item> level(
            GameData data, LevelValidator validator, String id, String source) {
        Problems problems = new Problems(GameData.levelPath(id));
        LevelDefinition level = data.parser().parse(source, id, problems);
        if (level != null && validator != null) {
            validator.validate(level, problems);
        }
        List<Item> result = new ArrayList<>();
        for (Problem p : problems.list()) {
            result.add(new Item(p, id, entityIndex(p.path())));
        }
        return result;
    }

    /** {@code entities[3].at} → 3; иначе {@code -1}. */
    static int entityIndex(String path) {
        Matcher m = ENTITY.matcher(path == null ? "" : path);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static void collectKeys(String source, Set<String> into) {
        Matcher m = TEXT_KEY.matcher(source);
        while (m.find()) {
            into.add(m.group(1));
        }
    }

    /** Есть ли ошибки (пак не выберется в игре). */
    public static boolean hasErrors(List<Item> items) {
        for (Item i : items) {
            if (i.isError()) {
                return true;
            }
        }
        return false;
    }
}
