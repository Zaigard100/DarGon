package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Состояние скрипта для отладчика: где он сейчас, стек вызовов и переменные. Значения — живые
 * объекты скрипта; читать их можно только во время {@link Debugger#statement}.
 */
public final class DebugState {

    /** Кадр стека: функция, текущее место в ней и её переменные. */
    public record Frame(String function, SourcePosition position, Map<String, Value> locals) {}

    private final Interpreter interpreter;

    DebugState(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /** Инструкция, которая сейчас выполнится. */
    public SourcePosition position() {
        return interpreter.frames.get(interpreter.frames.size() - 1).position;
    }

    /** Глубина вызовов: 1 — верхний уровень скрипта или обработчика. */
    public int depth() {
        return interpreter.frames.size();
    }

    /** Кадры стека, начиная с текущего; кадры хоста без инструкций пропущены. */
    public List<Frame> frames() {
        List<Frame> result = new ArrayList<>();
        for (int i = interpreter.frames.size() - 1; i >= 0; i--) {
            Interpreter.FrameInfo f = interpreter.frames.get(i);
            if (f.position != null) {
                result.add(new Frame(f.function, f.position, locals(f.env)));
            }
        }
        return result;
    }

    /** Глобальные переменные скрипта (без встроенных функций). */
    public Map<String, Value> globals() {
        return interpreter.globals == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(interpreter.globals.variables()));
    }

    /** Переменные от области кадра до глобальной (не включая её); ближние перекрывают дальние. */
    private Map<String, Value> locals(Environment env) {
        Map<String, Value> result = new LinkedHashMap<>();
        for (Environment e = env; e != null && e != interpreter.globals; e = e.parent()) {
            for (Map.Entry<String, Value> v : e.variables().entrySet()) {
                result.putIfAbsent(v.getKey(), v.getValue());
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
