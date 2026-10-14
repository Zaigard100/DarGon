package com.zaig100.dargon.script;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Общие помощники тестов: движок с перехватом вывода. */
abstract class ScriptTestSupport {

    final List<String> output = new ArrayList<>();
    ScriptEngine engine = newEngine();

    ScriptEngine newEngine() {
        return ScriptEngine.builder()
                .output(output::add)
                .random(new Random(42))
                .maxSteps(100_000)
                .build();
    }

    /** Выполняет скрипт и возвращает всё, что он напечатал. */
    List<String> run(String source) {
        output.clear();
        engine.eval(source, "test.dgs");
        return output;
    }

    /** Значение глобальной переменной после выполнения скрипта, в текстовом виде. */
    String eval(String source, String variable) {
        engine.eval(source, "test.dgs");
        return engine.global(variable).display();
    }

    /** Ожидает ошибку скрипта и возвращает её полный текст. */
    String error(String source) {
        return assertThrows(ScriptException.class, () -> engine.eval(source, "test.dgs"))
                .getMessage();
    }
}
