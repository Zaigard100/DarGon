package com.zaig100.dargon.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DebuggerTest {

    private static final String SOURCE =
            """
            total = 0
            def add(n) {
                local = n * 2
                total = total + local
            }
            add(1)
            add(2)
            """;

    @Test
    void reportsEveryStatementWithLineAndDepth() {
        List<String> seen = new ArrayList<>();
        ScriptEngine engine =
                ScriptEngine.builder()
                        .debugger(s -> seen.add(s.position().line() + "@" + s.depth()))
                        .build();
        engine.eval(SOURCE, "t.dgs");
        // Блоки не останавливают: только инструкции. Тело функции — на глубине 2.
        assertEquals(List.of("1@1", "2@1", "6@1", "3@2", "4@2", "7@1", "3@2", "4@2"), seen);
    }

    @Test
    void framesShowLocalsAndGlobals() {
        List<String> snapshot = new ArrayList<>();
        ScriptEngine engine =
                ScriptEngine.builder()
                        .debugger(
                                s -> {
                                    if (s.position().line() == 4 && snapshot.isEmpty()) {
                                        for (DebugState.Frame f : s.frames()) {
                                            snapshot.add(
                                                    f.function()
                                                            + ":"
                                                            + f.position().line()
                                                            + " "
                                                            + new java.util.TreeMap<>(f.locals())
                                                                    .keySet());
                                        }
                                        snapshot.add("total=" + s.globals().get("total").display());
                                    }
                                })
                        .build();
        engine.eval(SOURCE, "t.dgs");
        assertEquals(List.of("add:4 [local, n]", "(верхний уровень):6 []", "total=0"), snapshot);
    }

    @Test
    void handlerCalledByHostHasItsOwnFrame() {
        List<String> functions = new ArrayList<>();
        ScriptEngine engine =
                ScriptEngine.builder()
                        .debugger(s -> functions.add(s.frames().get(0).function()))
                        .build();
        engine.eval("h = def(self) {\n x = self\n}", "t.dgs");
        functions.clear();
        engine.call(engine.global("h"), Values.of("x"));
        assertEquals(List.of("<lambda>"), functions);
    }

    @Test
    void exceptionFromDebuggerStopsTheScript() {
        List<String> output = new ArrayList<>();
        ScriptEngine engine =
                ScriptEngine.builder()
                        .output(output::add)
                        .debugger(
                                s -> {
                                    if (s.position().line() == 2) {
                                        throw new ScriptException("остановлено");
                                    }
                                })
                        .build();
        ScriptException e =
                assertThrows(
                        ScriptException.class,
                        () -> engine.eval("print(1)\nprint(2)\nprint(3)", "t.dgs"));
        assertTrue(e.getMessage().contains("остановлено"));
        assertEquals(List.of("1"), output);
    }
}
