package com.zaig100.dargon.script;

/**
 * Отладчик DGScript. Движок вызывает его перед каждой инструкцией — в том же потоке, где идёт
 * скрипт. Чтобы остановить выполнение (точка остановки, пошаговый режим), отладчик просто не
 * возвращается, пока пользователь не продолжит; исключение {@link ScriptException} прерывает
 * скрипт.
 */
@FunctionalInterface
public interface Debugger {

    /** Перед инструкцией: {@code state} действителен только во время вызова. */
    void statement(DebugState state);
}
