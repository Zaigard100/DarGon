package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ошибка скрипта: синтаксическая (при компиляции) или ошибка выполнения.
 *
 * <p>Сообщение выглядит как {@code 05.dgs:12:8: неизвестная функция 'opne'}, а для ошибок внутри
 * функций дополняется стеком вызовов скрипта:
 *
 * <pre>
 * 05.dgs:3:5: деление на ноль
 *     в функции half (05.dgs:7:1)
 * </pre>
 */
public class ScriptException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String reason;
    private SourcePosition position;
    private final List<String> trace = new ArrayList<>();

    public ScriptException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public ScriptException(String reason, SourcePosition position) {
        super(reason);
        this.reason = reason;
        this.position = position;
    }

    public ScriptException(String reason, Throwable cause) {
        super(reason, cause);
        this.reason = reason;
    }

    /** Текст ошибки без позиции. */
    public String reason() {
        return reason;
    }

    /** Место ошибки или {@code null}, если оно неизвестно. */
    public SourcePosition position() {
        return position;
    }

    /** Стек вызовов скрипта: от места ошибки наружу. */
    public List<String> scriptTrace() {
        return Collections.unmodifiableList(trace);
    }

    /** Проставляет позицию, если её ещё нет. Возвращает это же исключение. */
    ScriptException at(SourcePosition where) {
        if (position == null) {
            position = where;
        }
        return this;
    }

    void addFrame(String frame) {
        trace.add(frame);
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (position != null) {
            sb.append(position).append(": ");
        }
        sb.append(reason);
        for (String frame : trace) {
            sb.append("\n    ").append(frame);
        }
        return sb.toString();
    }
}
