package com.zaig100.dargon.game;

import static com.zaig100.dargon.game.TestGame.data;
import static com.zaig100.dargon.game.TestGame.json;
import static com.zaig100.dargon.game.TestGame.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.script.ScriptHost;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.GridPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScriptsTest {

    private static void use(TestGame game) {
        game.tap(Action.USE);
        game.ticks(1);
    }

    @Test
    void buttonRunsHandlerWithSelfAndChangesLevel() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"button\", \"id\": \"b\", \"at\": [0, 0], \"onUse\": [\"setTile(2, 0, 'floor')\", \"entity('t').enable()\", \"self.disable()\"] },"
                                        + " { \"prefab\": \"teleport\", \"id\": \"t\", \"at\": [1, 0], \"target\": [2, 0], \"enabled\": false }",
                                "..#"));
        assertFalse(game.level.state().tiles().isFloor(2, 0));
        use(game);
        assertEquals(List.of(), game.errors);
        assertTrue(game.level.state().tiles().isFloor(2, 0));
        assertFalse(game.level.world().has(game.entity("t"), Marker.Disabled.class));
        assertTrue(game.level.world().has(game.entity("b"), Marker.Disabled.class));
    }

    @Test
    void buttonRearmsAfterLeavingItsCellInAnyDirection() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"button\", \"at\": [0, 0], \"onUse\": \"print('click')\" }",
                                "..",
                                ".."));
        use(game);
        use(game);
        assertEquals(List.of("click"), game.output, "повторно — только уйдя с кнопки");
        // Уходим по прямой (в DarGon3 это не перевзводило кнопку).
        game.tap(Action.MOVE_RIGHT);
        game.ticks(17);
        game.tap(Action.MOVE_LEFT);
        game.ticks(17);
        use(game);
        assertEquals(List.of("click", "click"), game.output);
    }

    @Test
    void levelScriptFunctionsAndTimersWork() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"button\", \"id\": \"b\", \"at\": [0, 0], \"onUse\": \"pressed()\" },"
                                        + " { \"prefab\": \"item\", \"id\": \"coin\", \"at\": [1, 0], \"item\": { \"id\": \"coins\" } }",
                                "...."),
                        "def pressed() {\n  entity('coin').moveTo(3, 0)\n  after(1, def() print('later'))\n}\n");
        use(game);
        GridPos coin = game.get("coin", GridPos.class);
        assertEquals(new GridPoint(3, 0), new GridPoint(coin.x, coin.y));
        game.ticks(61);
        assertEquals(List.of("later"), game.output);
    }

    @Test
    void initLoopAndStepHooksAreCalledByTheGame() {
        TestGame game =
                json(
                        map("[0, 0]", "", "..."),
                        "ticks = 0\n"
                                + "def init() { print('init') }\n"
                                + "def loop(dt) { ticks += 1\n if ticks == 1 { print(dt > 0) } }\n"
                                + "def step(x, y) { print('step ' + x + ' ' + y) }\n");
        assertEquals(List.of("init"), game.output);
        game.ticks(3);
        assertEquals(List.of("init", "true"), game.output);
        game.tap(Action.MOVE_RIGHT);
        game.ticks(1);
        assertEquals(List.of("init", "true", "step 1 0"), game.output);
        assertEquals(List.of(), game.errors);
    }

    @Test
    void useLoadsSharedPackScriptsRelativeToLevelsDir() {
        java.util.Map<String, String> files =
                java.util.Map.of(
                        "../secret.dgs",
                        "print('утечка')",
                        "lib/utils.dgs",
                        "def greet(who) { return 'hi ' + who }\n"
                                + "def init() { print(greet('init') + ' ' + entity('coin').x) }\n");
        TestGame game =
                new TestGame(
                        data().parser()
                                .parse(
                                        map(
                                                "[0, 0]",
                                                "{ \"prefab\": \"item\", \"id\": \"coin\", \"at\": [1, 0], \"item\": { \"id\": \"coins\" } }",
                                                ".."),
                                        "test"),
                        PlayerState.newGame(data().items()),
                        "use 'lib/utils'\nprint(greet('level'))\nuse 'nope'\n",
                        files::get,
                        null);
        assertEquals(List.of("hi level", "hi init 1"), game.output);
        assertEquals(1, game.errors.size(), String.valueOf(game.errors));
        assertTrue(game.errors.get(0).contains("нет файла nope.dgs"), game.errors.get(0));
        ScriptHost host = game.level.world().resource(ScriptHost.class);
        assertTrue(
                assertThrows(
                                com.zaig100.dargon.script.ScriptException.class,
                                () -> host.evaluate("use '../secret'", "консоль"))
                        .getMessage()
                        .contains("нет файла ../secret.dgs"));
    }

    @Test
    void failingLoopIsReportedOnceAndSwitchedOff() {
        TestGame game = json(map("[0, 0]", "", ".."), "def loop() { nope() }\n");
        game.ticks(10);
        assertEquals(1, game.errors.size(), String.valueOf(game.errors));
        assertTrue(game.errors.get(0).contains("loop отключена"));
    }

    @Test
    void consoleEvaluatesExpressionsAndStatementsInLevelScope() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"item\", \"id\": \"coin\", \"at\": [1, 0], \"item\": { \"id\": \"coins\" } }",
                                ".."),
                        "counter = 2\n");
        ScriptHost host = game.level.world().resource(ScriptHost.class);
        assertEquals("3", host.evaluate("counter + 1", "консоль").display());
        assertEquals(null, host.evaluate("counter = counter * 10", "консоль"));
        assertEquals("20", host.evaluate("counter", "консоль").display());
        assertEquals("1", host.evaluate("entity('coin').x", "консоль").display());
    }

    @Test
    void debuggerSeesHandlerStatementsUnderHandlerFileName() {
        List<String> seen = new java.util.ArrayList<>();
        TestGame game =
                TestGame.json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"button\", \"id\": \"b\", \"at\": [0, 0], \"onUse\": [\"x = 1\", \"print(x)\"] }",
                                ".."),
                        null,
                        s -> seen.add(s.position().file() + ":" + s.position().line()));
        use(game);
        String file = ScriptHost.handlerFile("test", "b", "use");
        assertEquals(List.of(file + ":1", file + ":2"), seen);
        assertEquals(List.of("1"), game.output);
    }

    @Test
    void scriptErrorsAreLoggedNotThrown() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"button\", \"at\": [0, 0], \"onUse\": \"entity('nope').enable()\" }",
                                ".."));
        use(game);
        assertEquals(1, game.errors.size());
        assertTrue(game.errors.get(0).contains("нет сущности с id 'nope'"), game.errors.get(0));
    }

    @Test
    void level18ButtonsRebuildTheMap() {
        TestGame game = TestGame.level("18");
        game.teleport(game.get("but1", GridPos.class).x, game.get("but1", GridPos.class).y);
        use(game);
        assertEquals(List.of(), game.errors);
        assertTrue(game.level.world().has(game.entity("but1"), Marker.Disabled.class));
        assertFalse(game.level.world().has(game.entity("but2"), Marker.Disabled.class));
    }
}
