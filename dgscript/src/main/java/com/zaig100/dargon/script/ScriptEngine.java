package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Движок DGScript: компиляция, выполнение, глобальные переменные, модули, таймеры.
 *
 * <p>Один экземпляр на уровень. Все скрипты уровня делят глобальные переменные движка. Движок
 * однопоточный: все вызовы — из потока игры.
 *
 * <pre>{@code
 * ScriptEngine engine = ScriptEngine.builder().output(log::info).build();
 * engine.defineFunction("say", 1, args -> { ui.say(args[0].display()); return null; });
 * engine.run(engine.compile(source, "05.dgs"));
 * engine.call(handler, Values.of("use"));   // обратный вызов из игры
 * engine.timers().update(delta);            // раз в тик
 * }</pre>
 *
 * <p>Встроенные функции доступны всегда, модуль {@code math} — после {@code use "math"}. {@code use
 * "utils"} выполняет файл {@code utils.dgs} из {@link Builder#library} в глобальной области: его
 * функции и переменные видны всем скриптам движка. Каждый модуль подключается один раз.
 */
public final class ScriptEngine {

    /** Настройки движка. */
    public static final class Builder {
        private long maxSteps = 1_000_000;
        private int maxDepth = 200;
        private Consumer<String> output = line -> {};
        private Consumer<ScriptException> errorHandler;
        private Random random = new Random();
        private Debugger debugger;
        private Function<String, String> library = name -> null;

        private Builder() {}

        /**
         * Файлы скриптов для {@code use "имя"}: по имени — исходник файла {@code имя.dgs} или
         * {@code null}, если его нет. Встроенные модули ({@code math}) важнее файлов.
         */
        public Builder library(Function<String, String> library) {
            this.library = library;
            return this;
        }

        /** Отладчик: вызывается перед каждой инструкцией (точки остановки, шаги). */
        public Builder debugger(Debugger debugger) {
            this.debugger = debugger;
            return this;
        }

        /** Лимит шагов на один вход в скрипт (защита от бесконечных циклов). */
        public Builder maxSteps(long maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        /** Максимальная глубина вложенных вызовов функций. */
        public Builder maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        /** Куда пишет {@code print}. По умолчанию вывод отбрасывается. */
        public Builder output(Consumer<String> output) {
            this.output = output;
            return this;
        }

        /**
         * Обработчик ошибок в обратных вызовах, которые хост не ловит сам (таймеры). По умолчанию
         * ошибка пробрасывается дальше.
         */
        public Builder errorHandler(Consumer<ScriptException> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /** Генератор для {@code rand}. Для детерминированных тестов — с фиксированным seed. */
        public Builder random(Random random) {
            this.random = random;
            return this;
        }

        public ScriptEngine build() {
            return new ScriptEngine(this);
        }
    }

    private final Interpreter interpreter;
    private final Environment builtins;
    private final Environment globals;
    private final Map<String, ScriptModule> modules = new HashMap<>();
    private final Set<String> loadedModules = new HashSet<>();
    private final Function<String, String> library;
    private final Consumer<ScriptException> errorHandler;
    private final Timers timers;

    private ScriptEngine(Builder builder) {
        this.interpreter = new Interpreter(builder.maxSteps, builder.maxDepth, this::loadModule);
        this.builtins = Environment.builtins(interpreter);
        this.globals = builtins.child();
        interpreter.debugger = builder.debugger;
        interpreter.globals = globals;
        this.library = builder.library;
        this.errorHandler = builder.errorHandler;
        this.timers = new Timers(this);
        Builtins.install(scope(), builder.output, builder.random, timers);
        modules.put("math", MathModule::install);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Движок с настройками по умолчанию. */
    public static ScriptEngine create() {
        return builder().build();
    }

    // ------------------------------------------------------------------ компиляция и запуск

    /**
     * Компилирует исходник.
     *
     * @param file имя файла для сообщений об ошибках
     * @throws ScriptException при синтаксической ошибке
     */
    public Script compile(String source, String file) {
        return new Script(
                file, new Parser(new Lexer(source, file).tokenize(), file).parseProgram());
    }

    /** Встроенный модуль движка ({@code math}): для него не нужен файл. */
    public static boolean isBuiltinModule(String name) {
        return name.equals("math");
    }

    /**
     * Имена модулей из {@code use "имя"} в исходнике — без выполнения; комментарии и строки
     * пропускаются. Исходник с лексической ошибкой — что успели найти до неё.
     */
    public static List<String> uses(String source, String file) {
        List<String> names = new ArrayList<>();
        List<Token> tokens;
        try {
            tokens = new Lexer(source, file).tokenize();
        } catch (ScriptException e) {
            return names;
        }
        for (int i = 0; i + 1 < tokens.size(); i++) {
            if (tokens.get(i).type() == TokenType.USE
                    && tokens.get(i + 1).type() == TokenType.STRING) {
                names.add(tokens.get(i + 1).text());
            }
        }
        return names;
    }

    /** Выполняет скрипт в глобальной области движка. */
    public void run(Script script) {
        interpreter.enter();
        try {
            script.program.execute(globals);
        } finally {
            interpreter.exit();
        }
    }

    /** Компилирует и выполняет исходник. */
    public void eval(String source, String file) {
        run(compile(source, file));
    }

    /** Вызывает функцию скрипта из хоста (обработчики событий, обратные вызовы). */
    public Value call(Value function, Value... args) {
        if (!(function instanceof FunctionValue f)) {
            throw new ScriptException(
                    "значение типа " + function.typeName() + " не является функцией");
        }
        interpreter.enter();
        try {
            return interpreter.call(f, args.clone(), null);
        } finally {
            interpreter.exit();
        }
    }

    /** Вызов, ошибки которого уходят в обработчик ошибок движка (если он задан). */
    void callSafely(FunctionValue function) {
        try {
            call(function);
        } catch (ScriptException e) {
            if (errorHandler == null) {
                throw e;
            }
            errorHandler.accept(e);
        }
    }

    // ------------------------------------------------------------------ окружение

    /** Задаёт глобальную переменную (видна скриптам, они могут её перезаписать). */
    public void define(String name, Value value) {
        globals.assign(name, value);
    }

    /** Значение глобальной переменной или встроенной функции; {@code null}-значение, если нет. */
    public Value global(String name) {
        Value value = globals.lookup(name);
        return value == null ? NullValue.INSTANCE : value;
    }

    /**
     * Регистрирует встроенную функцию (доступна без {@code use}). Скрипт может перекрыть её своей
     * переменной, но не изменить.
     */
    public void defineFunction(String name, int arity, NativeFunction function) {
        builtins.defineBuiltin(name, new NativeFunctionValue(name, arity, function));
    }

    /** Регистрирует модуль, подключаемый через {@code use "name"}. */
    public void registerModule(String name, ScriptModule module) {
        if (modules.putIfAbsent(name, module) != null) {
            throw new IllegalArgumentException("Модуль уже зарегистрирован: " + name);
        }
    }

    public Timers timers() {
        return timers;
    }

    private void loadModule(String name) {
        if (loadedModules.contains(name)) {
            return;
        }
        ScriptModule module = modules.get(name);
        if (module != null) {
            loadedModules.add(name);
            module.install(scope());
            return;
        }
        String source = library.apply(name);
        if (source == null) {
            throw new ScriptException(
                    "неизвестный модуль '" + name + "': нет файла " + name + ".dgs");
        }
        // Отметить до выполнения: файлы, подключающие друг друга, не зациклятся.
        loadedModules.add(name);
        try {
            compile(source, name + ".dgs").program.execute(globals);
        } catch (ScriptException e) {
            loadedModules.remove(name);
            throw e;
        }
    }

    private ScriptModule.Scope scope() {
        return new ScriptModule.Scope() {
            @Override
            public void function(String name, int arity, NativeFunction function) {
                defineFunction(name, arity, function);
            }

            @Override
            public void constant(String name, Value value) {
                builtins.defineBuiltin(name, value);
            }
        };
    }
}
