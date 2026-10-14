package com.zaig100.dargon.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HostInteropTest extends ScriptTestSupport {

    /** Объект игры, похожий на будущую обёртку сущности. */
    static final class Door implements ScriptObject {
        boolean open;
        int opened;

        @Override
        public String typeName() {
            return "door";
        }

        @Override
        public Value getMember(String name) {
            if (name.equals("open")) {
                return Values.of(open);
            }
            return ScriptObject.super.getMember(name);
        }

        @Override
        public void setMember(String name, Value value) {
            if (name.equals("open")) {
                open = value.isTruthy();
                return;
            }
            ScriptObject.super.setMember(name, value);
        }

        @Override
        public Value callMethod(String name, Value[] args) {
            if (name.equals("toggle")) {
                open = !open;
                opened++;
                return Values.of(open);
            }
            return ScriptObject.super.callMethod(name, args);
        }
    }

    @Test
    void scriptReadsWritesAndCallsHostObjects() {
        Door door = new Door();
        engine.define("door", door);
        engine.eval("door.open = true\nwas = door.open\ndoor.toggle()\ndoor.toggle()", "t.dgs");
        assertTrue(door.open);
        assertEquals(2, door.opened);
        assertEquals("true", engine.global("was").display());
    }

    @Test
    void unknownHostMembersAreReportedWithScriptPosition() {
        engine.define("door", new Door());
        assertEquals(
                "test.dgs:2:6: у объекта door нет изменяемого поля 'color'",
                error("x = 1\ndoor.color = 3"));
        assertEquals("test.dgs:1:6: у объекта door нет метода 'opne'", error("door.opne()"));
    }

    @Test
    void nativeFunctionsAreArityCheckedAndErrorsGetCallPosition() {
        List<String> said = new ArrayList<>();
        engine.defineFunction(
                "say",
                1,
                args -> {
                    said.add(args[0].display());
                    return null;
                });
        engine.defineFunction(
                "fail",
                0,
                args -> {
                    throw new ScriptException("сломалось");
                });
        engine.eval("say(\"привет\")", "t.dgs");
        assertEquals(List.of("привет"), said);
        assertTrue(error("say(1, 2)").contains("функция say ожидает 1 аргумент, передано 2"));
        assertEquals("test.dgs:2:3: сломалось", error("x = 1\n  fail()"));
    }

    @Test
    void hostCallsScriptHandlersWithFreshStepBudget() {
        ScriptEngine small = ScriptEngine.builder().maxSteps(50).build();
        small.eval(
                "def onUse(who) {\n n = 0\n while n < 10 { n += 1 }\n return who + \"!\" }",
                "h.dgs");
        Value handler = small.global("onUse");
        for (int i = 0; i < 10; i++) {
            // каждый вызов тратит ~25 шагов: без сброса бюджета второй вызов упал бы
            assertEquals("player!", small.call(handler, Values.of("player")).display());
        }
    }

    @Test
    void callingNonFunctionFromHostFails() {
        assertThrows(ScriptException.class, () -> engine.call(Values.of(1)));
    }

    @Test
    void modulesAreLoadedByUse() {
        engine.registerModule(
                "level",
                scope -> {
                    scope.constant("LEVEL_NAME", Values.of("NoFire"));
                    scope.function(
                            "double", 1, args -> Values.of(Values.number("double", args, 0) * 2));
                });
        assertTrue(error("x = LEVEL_NAME").contains("неизвестная переменная 'LEVEL_NAME'"));
        assertEquals(List.of("NoFire 8"), run("use \"level\"\nprint(LEVEL_NAME, double(4))"));
        assertEquals(
                "test.dgs:1:1: неизвестный модуль 'nope': нет файла nope.dgs",
                error("use \"nope\""));
    }

    @Test
    void useRunsLibraryFileOnceInGlobalScope() {
        java.util.Map<String, String> files =
                java.util.Map.of(
                        "utils",
                        "use \"lib/more\"\nloaded = loaded + 1\ndef twice(x) { return more(x) * 2 }",
                        "lib/more",
                        "use \"utils\"\ndef more(x) { return x + 1 }",
                        "broken",
                        "def f( {");
        engine = ScriptEngine.builder().output(output::add).library(files::get).build();
        assertEquals(
                List.of("7 1"),
                run("loaded = 0\nuse \"utils\"\nuse \"utils\"\nprint(twice(2) + 1, loaded)"));
        String broken = error("use \"broken\"");
        assertTrue(broken.startsWith("broken.dgs:1:"), broken);
        // Ошибка не запоминается как «подключено»: повторный use снова её покажет.
        assertTrue(error("use \"broken\"").startsWith("broken.dgs:1:"));
        assertEquals(List.of("1"), run("use \"math\"\nprint(abs(-1))"));
    }

    @Test
    void mathModule() {
        assertTrue(error("x = abs(-1)").contains("неизвестная функция 'abs'"));
        String source =
                """
                use "math"
                print(abs(-2), min(3, 1, 2), max(3, 1, 2), round(2.345, 2), round(2.5), floor(-1.5))
                print(clamp(15, 0, 10), pow(2, 10), sqrt(16), round(toDegrees(PI)))
                """;
        assertEquals(List.of("2 1 3 2.35 3 -2", "10 1024 4 180"), run(source));
        assertTrue(
                error("use \"math\"\nx = abs(\"a\")")
                        .contains("abs: аргумент 1 должен быть числом"));
    }

    @Test
    void collectionBuiltins() {
        String source =
                """
                a = [3, 1]
                push(a, 2, 5)
                last = pop(a)
                m = {x: 1, y: 2}
                remove(m, "x")
                print(a, last, keys(m), contains(a, 1), contains(m, "x"), contains("abc", "b"))
                print(range(3), range(2, 5), range(5, 0, -2), array(2, 2))
                print(type(a), type(m), type(1), type("s"), type(null), type(true), type(print))
                print(str(1.5) + "!", num("42") + 1, num("nope"), int(-2.7), len("тест"))
                """;
        assertEquals(
                List.of(
                        "[3, 1, 2] 5 [\"y\"] true false true",
                        "[0, 1, 2] [2, 3, 4] [5, 3, 1] [[0, 0], [0, 0]]",
                        "array map number string null bool function",
                        "1.5! 43 null -2 4"),
                run(source));
    }

    @Test
    void randIsDeterministicWithSeededRandom() {
        String source = "print(rand(1, 6), rand(1, 6), rand(1, 6), rand(10))";
        List<String> first = List.copyOf(run(source));
        engine = newEngine();
        assertEquals(first, run(source));
        assertTrue(error("x = rand(0)").contains("rand: n должно быть больше нуля"));
    }

    @Test
    void definedGlobalsAreVisibleAndWritableByScripts() {
        engine.define("score", Values.of(10));
        engine.eval("score += 5", "t.dgs");
        assertEquals("15", engine.global("score").display());
        assertSame(NullValue.INSTANCE, engine.global("missing"));
    }
}
