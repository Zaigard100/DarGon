package com.zaig100.dargon.script;

/**
 * Модуль, подключаемый в скрипте через {@code use "имя"}. Регистрируется в {@link
 * ScriptEngine#registerModule}.
 *
 * <pre>{@code
 * engine.registerModule("dargon", scope -> {
 *     scope.function("say", 1, args -> { ui.say(args[0].display()); return Values.nil(); });
 *     scope.constant("level", levelObject);
 * });
 * }</pre>
 */
@FunctionalInterface
public interface ScriptModule {

    void install(Scope scope);

    /** Куда модуль добавляет свои функции и константы (доступны во всех скриптах движка). */
    interface Scope {
        void function(String name, int arity, NativeFunction function);

        void constant(String name, Value value);
    }
}
