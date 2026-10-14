package com.zaig100.dargon.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LanguageTest extends ScriptTestSupport {

    @Test
    void arithmeticPrecedenceAndNumberDisplay() {
        assertEquals("14", eval("x = 2 + 3 * 4", "x"));
        assertEquals("20", eval("x = (2 + 3) * 4", "x"));
        assertEquals("1", eval("x = 10 % 3", "x"));
        assertEquals("2.5", eval("x = 5 / 2", "x"));
        assertEquals("-3", eval("x = -1 - 2", "x"));
    }

    @Test
    void stringConcatenationAndComparison() {
        assertEquals("hp: 4", eval("x = \"hp: \" + 4", "x"));
        assertEquals("true", eval("x = \"abc\" < \"abd\"", "x"));
        assertTrue(error("x = \"a\" < 1").contains("нельзя сравнить string и number"));
        assertTrue(error("x = [1] - 1").contains("оператор '-' не применим к array и number"));
    }

    @Test
    void singleAndDoubleQuotedStringsAreEqual() {
        assertEquals("true", eval("x = 'door' == \"door\"", "x"));
        assertEquals("it's \"ok\"", eval("x = 'it\\'s \"ok\"'", "x"));
        assertTrue(error("x = 'abc\"").contains("строка не закрыта"));
    }

    @Test
    void equalityHasNoImplicitConversions() {
        assertEquals("false", eval("x = 1 == \"1\"", "x"));
        assertEquals("true", eval("x = null == null", "x"));
        assertEquals("false", eval("a = [1]\nb = [1]\nx = a == b", "x"));
        assertEquals("true", eval("a = [1]\nb = a\nx = a == b", "x"));
    }

    @Test
    void truthinessAndShortCircuit() {
        assertEquals(
                List.of("ok"),
                run("if 0 || null || false { print(\"no\") } else { print(\"ok\") }"));
        // Правая часть не вычисляется — иначе была бы ошибка «неизвестная функция».
        assertEquals("false", eval("x = false && crash()", "x"));
        assertEquals("true", eval("x = true || crash()", "x"));
    }

    @Test
    void functionsClosuresAndRecursion() {
        String source =
                """
                def fib(n) {
                    if n < 2 { return n }
                    return fib(n - 1) + fib(n - 2)
                }
                def counter() {
                    count = 0
                    return def() {
                        count += 1
                        return count
                    }
                }
                c = counter()
                c()
                c()
                print(fib(15), c())
                """;
        assertEquals(List.of("610 3"), run(source));
    }

    @Test
    void returnWithoutValueOnItsOwnLine() {
        String source =
                """
                def f(x) {
                    if x { return
                    }
                    print("after")
                }
                f(true)
                f(false)
                r = f(true)
                """;
        assertEquals(List.of("after"), run(source));
        assertEquals("null", engine.global("r").display());
    }

    @Test
    void scopesAreFunctionLevelAndAssignmentsReachOuterVariables() {
        String source =
                """
                total = 0
                if true { inside = 5 }
                def addToTotal(n) {
                    total += n        // меняет глобальную переменную (в старом DGScript терялось)
                    local = n         // создаётся локально
                }
                addToTotal(3)
                addToTotal(4)
                print(total, inside)
                """;
        assertEquals(List.of("7 5"), run(source));
        assertEquals("null", engine.global("local").display());
    }

    @Test
    void parametersShadowGlobals() {
        assertEquals(
                List.of("1", "10"), run("x = 10\ndef f(x) { x = 1\n print(x) }\nf(0)\nprint(x)"));
    }

    @Test
    void arraysAndMaps() {
        String source =
                """
                grid = [[0, 0], [0, 0]]
                grid[1][0] = 7
                grid[0][1] += 2
                m = {name: "door", "open": false}
                m.open = true
                m["hp"] = 3
                m.hp -= 1
                print(grid, m, grid.length, len(m))
                """;
        assertEquals(
                List.of("[[0, 2], [7, 0]] {name: \"door\", open: true, hp: 2} 2 3"), run(source));
    }

    @Test
    void indexErrorsAreReported() {
        assertTrue(error("a = [1, 2]\nx = a[2]").contains("индекс 2 вне границ [0, 2)"));
        assertTrue(error("a = [1]\nx = a[0.5]").contains("индекс должен быть целым числом"));
        assertTrue(error("m = {}\nx = m[1]").contains("ключ словаря должен быть строкой"));
        assertEquals("null", eval("m = {}\nx = m.missing", "x"));
    }

    @Test
    void loops() {
        String source =
                """
                out = []
                for i = 0, i < 5, i += 1 {
                    if i == 1 { continue }
                    if i == 4 { break }
                    push(out, i)
                }
                for (j = 0; j < 2; j += 1) push(out, "j" + j)
                for x in ["a", "b"] { push(out, x) }
                for k in {p: 1, q: 2} { push(out, k) }
                for ch in "hi" { push(out, ch) }
                n = 0
                while n < 3 { n += 1 }
                do { n += 10 } while false
                print(join(out, ","), n)
                """;
        assertEquals(List.of("0,2,3,j0,j1,a,b,p,q,h,i 13"), run(source));
    }

    @Test
    void forInIteratesOverSnapshot() {
        assertEquals(
                List.of("3 6"),
                run("a = [1, 2, 3]\nfor x in a { push(a, x) }\nprint(len(a) / 2, len(a))"));
    }

    @Test
    void unknownNamesAreErrorsWithPositions() {
        assertEquals("test.dgs:2:1: неизвестная функция 'opne'", error("door = 1\nopne(door)"));
        assertEquals("test.dgs:1:5: неизвестная переменная 'y'", error("x = y + 1"));
        assertTrue(error("x = 5\nx()").contains("'x' не является функцией (number)"));
    }

    @Test
    void arityIsChecked() {
        assertTrue(
                error("def f(a, b) return a\nf(1)")
                        .contains("функция f ожидает 2 аргумента, передано 1"));
    }

    @Test
    void errorsInsideFunctionsCarryScriptStackTrace() {
        String message =
                error("def half(x) {\n  return x / 0\n}\ndef outer() { return half(4) }\nouter()");
        assertTrue(message.startsWith("test.dgs:2:12: деление на ноль"), message);
        assertTrue(message.contains("в функции half (вызов в test.dgs:4:22)"), message);
        assertTrue(message.contains("в функции outer (вызов в test.dgs:5:1)"), message);
    }

    @Test
    void infiniteLoopHitsStepLimitAndEngineStaysUsable() {
        assertTrue(error("while true { }").contains("превышен лимит в 100000 шагов"));
        assertEquals(List.of("still alive"), run("print(\"still alive\")"));
    }

    @Test
    void deepRecursionIsStopped() {
        assertTrue(error("def f(n) return f(n + 1)\nf(0)").contains("слишком глубокая рекурсия"));
    }

    @Test
    void builtinsCannotBeOverwrittenOnlyShadowed() {
        engine.eval("print = 5", "a.dgs");
        assertEquals("5", engine.global("print").display());
        ScriptEngine fresh = newEngine();
        assertEquals("<function print>", fresh.global("print").display());
    }
}
