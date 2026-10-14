package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.script.ScriptEngine;
import com.zaig100.dargon.script.ScriptException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Проверки уровня, которым нужен контекст: ссылки на другие уровни и сущности, ключи локализации,
 * компиляция скриптов, расстановка объектов и достижимость выхода.
 */
public final class LevelValidator {

    /** Имена компонентов, которые понимает проверка достижимости. */
    public static final String BLOCKING = "Blocking";

    public static final String LOCK = "Lock";
    public static final String TELEPORTER = "Teleporter";
    public static final String LEVEL_EXIT = "LevelExit";

    /** Вызовы {@code entity("id")} в скриптах — их id проверяются. */
    private static final Pattern ENTITY_CALL =
            Pattern.compile("\\bentity\\(\\s*[\"']([^\"']*)[\"']\\s*\\)");

    /** Что валидатору нужно знать об окружении уровня. */
    public interface Context {
        /** Существует ли уровень с таким идентификатором. */
        boolean levelExists(String id);

        /** Исходник скрипта по пути относительно каталога уровней; {@code null}, если файла нет. */
        String scriptSource(String path);

        /** Языки, для которых обязателен перевод: {@code en}, {@code ru}. */
        Collection<String> locales();

        /** Есть ли ключ локализации (без {@code @}) для языка. */
        boolean hasText(String locale, String key);
    }

    private final Context context;

    public LevelValidator(Context context) {
        this.context = context;
    }

    /** Проверяет уровень; найденное добавляет в {@code problems}. */
    public void validate(LevelDefinition level, Problems problems) {
        checkText(level.name(), "name", problems);
        for (EntitySpec entity : level.entities()) {
            checkPlacement(level, entity, problems);
            checkParams(level, entity, problems);
            for (Map.Entry<String, String> handler : entity.handlers().entrySet()) {
                String path = JsonValues.child(entity.path(), Prefab.handlerKey(handler.getKey()));
                checkScript(
                        level, handler.getValue(), level.id() + ".json:" + path, path, problems);
            }
        }
        if (level.script() != null) {
            String source = context.scriptSource(level.scriptPath());
            if (source == null) {
                problems.error("script", "файл скрипта не найден: " + level.scriptPath());
            } else {
                checkScript(level, source, level.scriptPath(), "script", problems);
            }
        }
        checkReachability(level, problems);
    }

    // ------------------------------------------------------------------ сущности

    private static void checkPlacement(
            LevelDefinition level, EntitySpec entity, Problems problems) {
        TileMap tiles = level.tiles();
        GridPoint at = entity.at();
        String path = JsonValues.child(entity.path(), "at");
        switch (entity.prefab().placement()) {
            case FLOOR:
                if (!tiles.inBounds(at)) {
                    problems.warning(path, entity.prefab().id() + " за пределами карты " + at);
                } else if (!tiles.isFloor(at)) {
                    problems.warning(path, entity.prefab().id() + " стоит на стене " + at);
                }
                break;
            case WALL:
                if (tiles.isFloor(at)) {
                    problems.warning(
                            path,
                            entity.prefab().id()
                                    + " стоит на полу "
                                    + at
                                    + ", обычно его ставят на стену или за край карты");
                }
                break;
            default:
                break;
        }
    }

    private void checkParams(LevelDefinition level, EntitySpec entity, Problems problems) {
        for (Map.Entry<String, JsonValue> param : entity.params().entrySet()) {
            JsonValue value = param.getValue();
            if (value == null || value.isNull()) {
                continue;
            }
            String path = JsonValues.child(entity.path(), param.getKey());
            switch (entity.prefab().params().get(param.getKey()).type()) {
                case LEVEL:
                    if (!context.levelExists(value.asString())) {
                        problems.error(path, "уровень '" + value.asString() + "' не найден");
                    }
                    break;
                case ENTITY:
                    if (level.entity(value.asString()) == null) {
                        problems.error(
                                path, "в уровне нет сущности с id '" + value.asString() + "'");
                    }
                    break;
                case TEXT:
                    checkText(value, path, problems);
                    break;
                case POINT:
                    GridPoint target = new GridPoint(value.child.asInt(), value.child.next.asInt());
                    if (!level.tiles().isFloor(target)) {
                        problems.warning(path, "клетка " + target + " не на полу");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void checkText(JsonValue value, String path, Problems problems) {
        if (value.isString()) {
            checkText(value.asString(), path, problems);
        } else {
            problems.warning(path, "текст не локализован: используйте ключ \"@...\"");
        }
    }

    private void checkText(String text, String path, Problems problems) {
        if (!text.startsWith(ParamType.TEXT_KEY_PREFIX)) {
            problems.warning(path, "текст не локализован: используйте ключ \"@...\"");
            return;
        }
        String key = text.substring(ParamType.TEXT_KEY_PREFIX.length());
        for (String locale : context.locales()) {
            if (!context.hasText(locale, key)) {
                problems.error(path, "нет перевода ключа '" + key + "' для языка " + locale);
            }
        }
    }

    // ------------------------------------------------------------------ скрипты

    private void checkScript(
            LevelDefinition level, String source, String file, String path, Problems problems) {
        try {
            ScriptEngine.create().compile(source, file);
        } catch (ScriptException e) {
            problems.error(path, e.getMessage());
            return;
        }
        // Подключённые общие файлы — и то, что подключают они сами.
        // use "имя" — файл имя.dgs должен быть в паке; проверяется и то, что
        // подключает он сам.
        ArrayDeque<String> queue = new ArrayDeque<>(List.of(source));
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (!queue.isEmpty()) {
            for (String module : ScriptEngine.uses(queue.poll(), file)) {
                if (ScriptEngine.isBuiltinModule(module) || !seen.add(module)) {
                    continue;
                }
                String shared = context.scriptSource(module + ".dgs");
                if (shared == null) {
                    problems.error(path, "use \"" + module + "\": нет файла " + module + ".dgs");
                    continue;
                }
                try {
                    ScriptEngine.create().compile(shared, module + ".dgs");
                    queue.add(shared);
                } catch (ScriptException e) {
                    problems.error(path, "use \"" + module + "\": " + e.getMessage());
                }
            }
        }
        Matcher matcher = ENTITY_CALL.matcher(source);
        while (matcher.find()) {
            if (level.entity(matcher.group(1)) == null) {
                problems.error(
                        path,
                        "entity(\"" + matcher.group(1) + "\"): в уровне нет сущности с таким id");
            }
        }
    }

    // ------------------------------------------------------------------ достижимость

    /**
     * Ищет путь от точки появления до выхода: по полу в 8 направлениях, через телепорты, в обход
     * непроходимых объектов без замка. Скрипты не учитываются, поэтому недостижимый выход — только
     * предупреждение: его может открыть кнопка.
     */
    private static void checkReachability(LevelDefinition level, Problems problems) {
        TileMap tiles = level.tiles();
        int width = tiles.width();
        boolean[] blocked = new boolean[width * tiles.height()];
        List<int[]> teleports = new ArrayList<>();
        List<EntitySpec> exits = new ArrayList<>();
        for (EntitySpec entity : level.entities()) {
            Prefab prefab = entity.prefab();
            if (prefab.hasComponent(LEVEL_EXIT)) {
                exits.add(entity);
            }
            if (!entity.enabled()) {
                continue;
            }
            GridPoint at = entity.at();
            if (prefab.hasComponent(BLOCKING) && !prefab.hasComponent(LOCK) && tiles.inBounds(at)) {
                blocked[at.y() * width + at.x()] = true;
            }
            if (prefab.hasComponent(TELEPORTER)) {
                JsonValue target = JsonValues.get(entity.components().get(TELEPORTER), "target");
                if (target != null && target.isArray() && target.size == 2) {
                    teleports.add(
                            new int[] {
                                at.x(), at.y(), target.child.asInt(), target.child.next.asInt()
                            });
                }
            }
        }
        if (exits.isEmpty()) {
            problems.error(
                    "entities", "в уровне нет выхода (сущности с компонентом " + LEVEL_EXIT + ")");
            return;
        }
        boolean[] reached = new boolean[blocked.length];
        ArrayDeque<GridPoint> queue = new ArrayDeque<>();
        GridPoint spawn = level.spawn();
        reached[spawn.y() * width + spawn.x()] = true;
        queue.add(spawn);
        while (!queue.isEmpty()) {
            GridPoint p = queue.poll();
            // Игрок ходит и по диагонали: проверяется только целевая клетка (как в DarGon3).
            for (Direction step : Direction.values()) {
                visit(tiles, blocked, reached, queue, p.x() + step.dx, p.y() + step.dy);
            }
            for (int[] teleport : teleports) {
                if (teleport[0] == p.x() && teleport[1] == p.y()) {
                    visit(tiles, blocked, reached, queue, teleport[2], teleport[3]);
                }
            }
        }
        for (EntitySpec exit : exits) {
            if (!exit.enabled()) {
                continue; // выход появляется по скрипту — так задумано
            }
            GridPoint at = exit.at();
            if (!tiles.inBounds(at) || !reached[at.y() * tiles.width() + at.x()]) {
                problems.warning(
                        JsonValues.child(exit.path(), "at"),
                        "выход " + at + " недостижим от точки появления без помощи скриптов");
            }
        }
    }

    private static void visit(
            TileMap tiles,
            boolean[] blocked,
            boolean[] reached,
            ArrayDeque<GridPoint> queue,
            int x,
            int y) {
        if (!tiles.isFloor(x, y)) {
            return;
        }
        int i = y * tiles.width() + x;
        if (blocked[i] || reached[i]) {
            return;
        }
        reached[i] = true;
        queue.add(new GridPoint(x, y));
    }
}
