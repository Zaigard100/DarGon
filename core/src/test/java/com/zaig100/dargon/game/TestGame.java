package com.zaig100.dargon.game;

import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.GridPoint;
import com.zaig100.dargon.level.LevelDefinition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Запуск уровней в тестах: настоящие данные игры, ввод «руками», тики без графики. */
final class TestGame {

    private static GameData data;

    final ActionState input = new ActionState();
    final PlayerState player;
    final LevelRuntime level;

    /** Ошибки и вывод скриптов. */
    final List<String> errors = new ArrayList<>();

    final List<String> output = new ArrayList<>();

    TestGame(LevelDefinition definition, PlayerState player, String script) {
        this(definition, player, script, null);
    }

    TestGame(
            LevelDefinition definition,
            PlayerState player,
            String script,
            com.zaig100.dargon.script.Debugger debugger) {
        this(definition, player, script, path -> null, debugger);
    }

    /** С общими скриптами пака для {@code use} (путь относительно каталога уровней). */
    TestGame(
            LevelDefinition definition,
            PlayerState player,
            String script,
            java.util.function.Function<String, String> library,
            com.zaig100.dargon.script.Debugger debugger) {
        this.player = player;
        this.level =
                LevelRuntime.start(
                        definition,
                        player,
                        input,
                        script,
                        library,
                        new GameLog() {
                            @Override
                            public void info(String message) {
                                output.add(message);
                            }

                            @Override
                            public void error(String message) {
                                errors.add(message);
                            }
                        },
                        new Cheats(),
                        new java.util.Random(),
                        debugger);
    }

    TestGame(LevelDefinition definition, PlayerState player) {
        this(definition, player, null);
    }

    /** Уровень игры по id с новым игроком (и его скриптом). */
    static TestGame level(String id) {
        LevelDefinition definition = data().loadLevel(id);
        return new TestGame(
                definition,
                PlayerState.newGame(data().items()),
                definition.scriptPath() == null
                        ? null
                        : data().readScript(definition.scriptPath()));
    }

    /** Уровень из JSON со скриптом уровня под отладчиком. */
    static TestGame json(String json, String script, com.zaig100.dargon.script.Debugger debugger) {
        return new TestGame(
                data().parser().parse(json, "test"),
                PlayerState.newGame(data().items()),
                script,
                debugger);
    }

    /** Уровень из JSON со скриптом уровня. */
    static TestGame json(String json, String script) {
        return new TestGame(
                data().parser().parse(json, "test"), PlayerState.newGame(data().items()), script);
    }

    /** Сущность по id. */
    int entity(String id) {
        var named =
                level.world()
                        .entities(
                                com.zaig100.dargon.ecs.Family.all(
                                        com.zaig100.dargon.game.component.EntityId.class));
        for (int i = 0; i < named.size(); i++) {
            int e = named.get(i);
            if (id.equals(
                    level.world().get(e, com.zaig100.dargon.game.component.EntityId.class).id)) {
                return e;
            }
        }
        throw new AssertionError("нет сущности " + id);
    }

    <T extends com.zaig100.dargon.ecs.Component> T get(String id, Class<T> type) {
        return level.world().get(entity(id), type);
    }

    /** Ставит игрока в клетку мгновенно. */
    void teleport(int x, int y) {
        level.playerPos().set(x, y);
        level.world().get(level.player(), com.zaig100.dargon.game.component.Motion.class).snap();
    }

    /** Уровень из JSON (настоящие префабы). */
    static TestGame json(String json) {
        return new TestGame(
                data().parser().parse(json, "test"), PlayerState.newGame(data().items()));
    }

    static synchronized GameData data() {
        if (data == null) {
            Path assets = Path.of(System.getProperty("dargon.assets", "../assets"));
            data =
                    GameData.loadBuiltin(
                            path -> {
                                Path file = assets.resolve(path);
                                try {
                                    return Files.isRegularFile(file)
                                            ? Files.readString(file, StandardCharsets.UTF_8)
                                            : null;
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
        }
        return data;
    }

    void ticks(int n) {
        for (int i = 0; i < n; i++) {
            level.tick();
        }
    }

    /** Тики, пока условие не выполнится; падает, если не выполнилось за {@code max} тиков. */
    int tickUntil(BooleanSupplier condition, int max) {
        for (int i = 1; i <= max; i++) {
            level.tick();
            if (condition.getAsBoolean()) {
                return i;
            }
        }
        throw new AssertionError("условие не выполнилось за " + max + " тиков");
    }

    /** Короткое нажатие: нажали и отпустили между тиками. */
    void tap(Action action) {
        input.press(action);
        input.release(action);
    }

    void hold(Action... actions) {
        for (Action action : actions) {
            input.press(action);
        }
    }

    void releaseAll(Action... actions) {
        for (Action action : actions) {
            input.release(action);
        }
    }

    void click(int x, int y) {
        input.click(new GridPoint(x, y));
    }

    GridPoint pos() {
        return new GridPoint(level.playerPos().x, level.playerPos().y);
    }

    boolean moving() {
        return level.world()
                .get(level.player(), com.zaig100.dargon.game.component.Motion.class)
                .isMoving();
    }

    /** Уровень в JSON: карта, спавн и сущности. */
    static String map(String spawn, String entities, String... rows) {
        StringBuilder tiles = new StringBuilder();
        for (String row : rows) {
            tiles.append(tiles.length() == 0 ? "" : ", ").append('"').append(row).append('"');
        }
        return "{ \"formatVersion\": 1, \"name\": \"test\", \"spawn\": "
                + spawn
                + ", \"tiles\": ["
                + tiles
                + "], \"entities\": ["
                + entities
                + "] }";
    }
}
