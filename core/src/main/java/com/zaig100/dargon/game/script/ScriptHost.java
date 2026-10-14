package com.zaig100.dargon.game.script;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.GameLog;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.component.EntityId;
import com.zaig100.dargon.game.component.ScriptHandlers;
import com.zaig100.dargon.level.TileType;
import com.zaig100.dargon.script.Debugger;
import com.zaig100.dargon.script.FunctionValue;
import com.zaig100.dargon.script.ScriptEngine;
import com.zaig100.dargon.script.ScriptException;
import com.zaig100.dargon.script.Value;
import com.zaig100.dargon.script.Values;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * DGScript на уровне: один движок, файл скрипта уровня и обработчики сущностей ({@code "onUse"}).
 *
 * <p>Обработчик компилируется один раз в функцию {@code def(self) { ... }}: внутри доступна
 * сущность-владелец {@code self}. Функции игры: {@code entity(id)}, {@code setTile(x, y,
 * "floor"|"wall")}, {@code print(...)}, таймеры {@code after}/{@code every}.
 *
 * <p>Ошибка скрипта не роняет игру: она пишется в журнал, а обработчик прерывается.
 */
public final class ScriptHost {

    private static final String HANDLER = "__handler";
    private static final String CONSOLE = "__console";
    private static final String INIT = "init";
    private static final String LOOP = "loop";
    private static final String STEP = "step";
    private final java.util.Set<String> failedHooks = new java.util.HashSet<>();

    private final GameLog log;
    private final String levelId;
    private final String scriptFile;
    private final String scriptSource;
    private final Function<String, String> library;
    private ScriptEngine engine;
    private World world;
    private LevelState level;
    private EntitySet named;
    private ComponentMapper<EntityId> ids;
    private ComponentMapper<ScriptHandlers> handlers;
    private final Map<String, Value> compiled = new HashMap<>();
    private final Debugger debugger;
    private boolean defining;

    /**
     * @param scriptFile файл скрипта уровня (для сообщений) или {@code null}
     * @param scriptSource текст скрипта уровня или {@code null}
     */
    public ScriptHost(GameLog log, String levelId, String scriptFile, String scriptSource) {
        this(log, levelId, scriptFile, scriptSource, null);
    }

    /** То же с отладчиком (редактор паков): он видит каждую инструкцию скриптов уровня. */
    public ScriptHost(
            GameLog log,
            String levelId,
            String scriptFile,
            String scriptSource,
            Debugger debugger) {
        this(log, levelId, scriptFile, scriptSource, path -> null, debugger);
    }

    /**
     * Полный вариант.
     *
     * @param library общие скрипты пака для {@code use "имя"}: путь относительно каталога уровней
     *     ({@code utils.dgs}) — исходник или {@code null}
     */
    public ScriptHost(
            GameLog log,
            String levelId,
            String scriptFile,
            String scriptSource,
            Function<String, String> library,
            Debugger debugger) {
        this.library = library;
        this.log = log;
        this.levelId = levelId;
        this.scriptFile = scriptFile;
        this.scriptSource = scriptSource;
        this.debugger = debugger;
    }

    /** Вызывается после создания сущностей уровня: готовит движок и выполняет скрипт уровня. */
    public void bind(World world) {
        this.world = world;
        this.level = world.resource(LevelState.class);
        this.named = world.entities(Family.all(EntityId.class));
        this.ids = world.mapper(EntityId.class);
        this.handlers = world.mapper(ScriptHandlers.class);
        engine =
                ScriptEngine.builder()
                        .output(log::info)
                        .library(name -> isPackPath(name) ? library.apply(name + ".dgs") : null)
                        .errorHandler(e -> log.error(e.getMessage()))
                        .debugger(
                                debugger == null
                                        ? null
                                        : state -> {
                                            if (!defining) {
                                                debugger.statement(state);
                                            }
                                        })
                        .build();
        engine.defineFunction("entity", 1, args -> entity(Values.string("entity", args, 0)));
        engine.defineFunction(
                "setTile",
                3,
                args -> {
                    setTile(
                            Values.integer("setTile", args, 0),
                            Values.integer("setTile", args, 1),
                            Values.string("setTile", args, 2));
                    return Values.nil();
                });
        engine.defineFunction(
                "achieve",
                1,
                args -> {
                    world.events()
                            .publish(new GameEvents.Achieve(Values.string("achieve", args, 0)));
                    return Values.nil();
                });
        if (scriptSource != null) {
            try {
                engine.eval(scriptSource, scriptFile);
            } catch (ScriptException e) {
                log.error(e.getMessage());
            }
        }
        world.events()
                .subscribe(
                        GameEvents.Step.class, e -> hook(STEP, Values.of(e.x()), Values.of(e.y())));
        hook(INIT);
    }

    /** Имя из {@code use} — путь внутри каталога уровней ({@code lib/utils}), не наружу. */
    private static boolean isPackPath(String name) {
        return !name.isEmpty()
                && !name.startsWith("/")
                && !name.contains("\\")
                && !name.contains(":")
                && !java.util.Arrays.asList(name.split("/")).contains("..");
    }

    /**
     * Функция-хук скрипта уровня, если она объявлена: {@code init()} — после загрузки уровня,
     * {@code loop(dt)} — каждый тик, {@code step(x, y)} — шаг героя. Лишние аргументы не передаются
     * ({@code def loop()} тоже годится). После ошибки хук отключается до конца уровня, чтобы {@code
     * loop} не засыпал журнал одной и той же ошибкой 60 раз в секунду.
     */
    private void hook(String name, Value... args) {
        if (failedHooks.contains(name)) {
            return;
        }
        Value function = engine.global(name);
        if (!(function instanceof FunctionValue f)) {
            return;
        }
        int arity = f.arity();
        Value[] passed =
                arity == FunctionValue.VARIADIC || arity >= args.length
                        ? args
                        : java.util.Arrays.copyOf(args, arity);
        try {
            if (arity != FunctionValue.VARIADIC && arity > args.length) {
                throw new ScriptException(
                        name + " принимает не больше " + args.length + " аргументов");
            }
            engine.call(f, passed);
        } catch (ScriptException e) {
            failedHooks.add(name);
            log.error(e.getMessage() + "\n    " + name + " отключена до перезапуска уровня");
        }
    }

    /** Есть ли у сущности обработчик события. */
    public boolean hasHandler(int entity, String event) {
        ScriptHandlers h = handlers.getOrNull(entity);
        return h != null && h.code.containsKey(event);
    }

    /** Выполняет обработчик события сущности, если он есть. */
    public void fire(int entity, String event) {
        ScriptHandlers h = handlers.getOrNull(entity);
        String code = h == null ? null : h.code.get(event);
        if (code == null) {
            return;
        }
        try {
            Value function = compiled.get(key(entity, event));
            if (function == null) {
                // Служебное определение обработчика отладчику не показываем: только его код.
                defining = true;
                try {
                    engine.eval(HANDLER + " = def(self) { " + code + "\n}", file(entity, event));
                } finally {
                    defining = false;
                }
                function = engine.global(HANDLER);
                compiled.put(key(entity, event), function);
            }
            engine.call(function, new EntityHandle(world, entity, level::walkabilityChanged));
        } catch (ScriptException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Выполняет код в глобальной области скриптов уровня (консоль отладки).
     *
     * @return значение, если код — выражение; {@code null} для инструкций
     * @throws ScriptException ошибка в коде
     */
    public Value evaluate(String code, String file) {
        com.zaig100.dargon.script.Script expression;
        try {
            expression = engine.compile(CONSOLE + " = (" + code + "\n)", file);
        } catch (ScriptException notExpression) {
            engine.eval(code, file);
            return null;
        }
        engine.run(expression);
        return engine.global(CONSOLE);
    }

    /** Имя файла обработчика события в сообщениях и для точек остановки отладчика. */
    public static String handlerFile(String levelId, String entityId, String event) {
        return levelId
                + ": "
                + (entityId == null ? "сущность" : entityId)
                + ".on"
                + capitalize(event);
    }

    /** Один тик: таймеры {@code after}/{@code every}, затем {@code loop(dt)} скрипта уровня. */
    public void update(float delta) {
        engine.timers().update(delta);
        hook(LOOP, Values.of(delta));
    }

    private Value entity(String id) {
        for (int i = 0; i < named.size(); i++) {
            int entity = named.get(i);
            if (id.equals(ids.get(entity).id)) {
                return new EntityHandle(world, entity, level::walkabilityChanged);
            }
        }
        throw new ScriptException("нет сущности с id '" + id + "'");
    }

    private void setTile(int x, int y, String type) {
        TileType tile = TileType.byId(type);
        if (tile == null) {
            throw new ScriptException(
                    "неизвестный тип клетки '" + type + "'; допустимы floor, wall");
        }
        if (!level.tiles().inBounds(x, y)) {
            throw new ScriptException("клетка [" + x + ", " + y + "] вне карты");
        }
        level.setTile(x, y, tile);
    }

    private String file(int entity, String event) {
        EntityId id = ids.getOrNull(entity);
        return handlerFile(levelId, id == null ? null : id.id, event);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String key(int entity, String event) {
        return entity + ":" + event;
    }
}
