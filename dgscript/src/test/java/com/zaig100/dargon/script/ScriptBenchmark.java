package com.zaig100.dargon.script;

import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Грубый бенчмарк интерпретатора. Запуск: {@code ./gradlew :dgscript:benchmark}. */
@Tag("benchmark")
@SuppressWarnings("checkstyle:RegexpSinglelineJava") // результаты печатаются в консоль
class ScriptBenchmark {

    @Test
    void recursiveCalls() {
        measure(
                "fib(22): ~57k вызовов функций",
                "def fib(n) { if n < 2 { return n }\n return fib(n - 1) + fib(n - 2) }\nx = fib(22)");
    }

    @Test
    void loopsAndCollections() {
        measure(
                "цикл 50k итераций с массивом и словарём",
                "a = []\nm = {s: 0}\nfor i = 0, i < 50000, i += 1 { push(a, i)\n m.s += a[i] % 7 }");
    }

    @Test
    void typicalEventHandler() {
        ScriptEngine engine = ScriptEngine.builder().maxSteps(Long.MAX_VALUE).build();
        engine.eval(
                "state = {open: false, uses: 0}\n"
                        + "def onUse(who) {\n"
                        + "  state.uses += 1\n"
                        + "  if state.uses % 2 == 0 { state.open = !state.open }\n"
                        + "  return state.open\n"
                        + "}",
                "bench.dgs");
        Value handler = engine.global("onUse");
        Value who = Values.of("player");
        for (int i = 0; i < 20_000; i++) {
            engine.call(handler, who);
        }
        int calls = 100_000;
        long start = System.nanoTime();
        for (int i = 0; i < calls; i++) {
            engine.call(handler, who);
        }
        double micros = (System.nanoTime() - start) / 1_000.0 / calls;
        System.out.printf(
                Locale.ROOT,
                "%-45s %8.2f мкс/вызов%n",
                "вызов обработчика события из игры",
                micros);
    }

    private static void measure(String name, String source) {
        ScriptEngine engine = ScriptEngine.builder().maxSteps(Long.MAX_VALUE).build();
        Script script = engine.compile(source, "bench.dgs");
        for (int i = 0; i < 5; i++) {
            engine.run(script);
        }
        int runs = 10;
        long start = System.nanoTime();
        for (int i = 0; i < runs; i++) {
            engine.run(script);
        }
        double millis = (System.nanoTime() - start) / 1e6 / runs;
        System.out.printf(Locale.ROOT, "%-45s %8.2f мс/запуск%n", name, millis);
    }
}
