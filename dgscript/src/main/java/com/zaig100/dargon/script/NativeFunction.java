package com.zaig100.dargon.script;

/**
 * Функция, реализованная на Java. Регистрируется через {@link ScriptEngine#defineFunction} или в
 * модуле.
 *
 * <p>Число аргументов проверяет движок. Ошибки сообщаются через {@link ScriptException} — позиция
 * вызова в скрипте будет добавлена автоматически.
 */
@FunctionalInterface
public interface NativeFunction {
    Value call(Value[] args);
}
