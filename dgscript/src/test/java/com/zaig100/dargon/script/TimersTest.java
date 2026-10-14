package com.zaig100.dargon.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimersTest extends ScriptTestSupport {

    @Test
    void afterFiresOnceWhenTimeElapses() {
        run("after(1, def() print(\"boom\"))");
        engine.timers().update(0.5f);
        assertEquals(List.of(), output);
        engine.timers().update(0.5f);
        assertEquals(List.of("boom"), output);
        engine.timers().update(5f);
        assertEquals(List.of("boom"), output);
        assertEquals(0, engine.timers().active());
    }

    @Test
    void everyRepeatsUntilCancelled() {
        run("n = 0\nid = every(0.5, def() { n += 1\n if n == 3 { cancel(id) } })");
        for (int i = 0; i < 10; i++) {
            engine.timers().update(0.5f);
        }
        assertEquals("3", engine.global("n").display());
        assertEquals(0, engine.timers().active());
    }

    @Test
    void timersFireInDueOrder() {
        run(
                """
                after(0.3, def() print("c"))
                after(0.1, def() print("a"))
                after(0.2, def() print("b"))
                """);
        engine.timers().update(1f);
        assertEquals(List.of("a", "b", "c"), output);
    }

    @Test
    void callbacksCanScheduleAndCancelOtherTimers() {
        run(
                """
                later = after(0.2, def() print("should not run"))
                after(0.1, def() {
                    cancel(later)
                    after(0.1, def() print("chained"))
                })
                """);
        engine.timers().update(0.15f);
        engine.timers().update(0.15f);
        assertEquals(List.of("chained"), output);
    }

    @Test
    void errorInCallbackGoesToHandlerAndOthersStillRun() {
        List<ScriptException> errors = new ArrayList<>();
        engine = ScriptEngine.builder().output(output::add).errorHandler(errors::add).build();
        run("after(0.1, def() print(1 / 0))\nafter(0.2, def() print(\"ok\"))");
        engine.timers().update(1f);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("деление на ноль"));
        assertEquals(List.of("ok"), output);
    }

    @Test
    void withoutHandlerCallbackErrorsPropagate() {
        run("after(0, def() print(1 / 0))");
        assertThrows(ScriptException.class, () -> engine.timers().update(0f));
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertTrue(error("every(0, def() print(1))").contains("период должен быть больше нуля"));
        assertTrue(error("after(1, 2)").contains("after: аргумент 2 должен быть функцией"));
    }
}
