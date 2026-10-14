package com.zaig100.dargon.script;

/**
 * Состояние выполнения: лимит шагов, глубина вызовов, значение {@code return}.
 *
 * <p>Лимит шагов защищает игру от зависшего скрипта: каждая инструкция и каждая итерация цикла
 * тратят шаг; при исчерпании выполнение прерывается ошибкой. Счётчик сбрасывается при каждом входе
 * хоста в скрипт ({@link ScriptEngine#run}, {@link ScriptEngine#call}, таймеры).
 */
final class Interpreter {

    static final int NORMAL = 0;
    static final int BREAK = 1;
    static final int CONTINUE = 2;
    static final int RETURN = 3;

    private final long maxSteps;
    private final int maxDepth;

    /** Отладчик или {@code null}. */
    Debugger debugger;

    /** Глобальная область движка — граница локальных переменных для отладчика. */
    Environment globals;

    /** Кадр стека для отладчика: функция, её область и текущее место. */
    static final class FrameInfo {
        final String function;
        Environment env;
        SourcePosition position;

        FrameInfo(String function) {
            this.function = function;
        }
    }

    /** Стек вызовов скрипта; ведётся, только когда есть отладчик. */
    final java.util.List<FrameInfo> frames = new java.util.ArrayList<>();

    private DebugState debugState;

    private long stepsLeft;
    private int depth;
    private int entries;

    /** Подключение модулей для {@code use}. */
    final Stmt.ModuleLoader modules;

    /** Значение последнего {@code return}; читается сразу после завершения тела функции. */
    Value returnValue = NullValue.INSTANCE;

    Interpreter(long maxSteps, int maxDepth, Stmt.ModuleLoader modules) {
        this.maxSteps = maxSteps;
        this.maxDepth = maxDepth;
        this.modules = modules;
    }

    /**
     * Вход хоста в скрипт; на внешнем входе сбрасывает лимит шагов. Парный вызов — {@link #exit}.
     */
    void enter() {
        if (entries++ == 0) {
            stepsLeft = maxSteps;
            depth = 0;
            if (debugger != null) {
                frames.clear();
                frames.add(new FrameInfo("(верхний уровень)"));
            }
        }
    }

    void exit() {
        entries--;
    }

    /**
     * Шаг выполнения перед инструкцией.
     *
     * @param env область инструкции; {@code null} — без остановки отладчика (блок)
     */
    void step(SourcePosition position, Environment env) {
        if (--stepsLeft < 0) {
            throw new ScriptException(
                    "превышен лимит в " + maxSteps + " шагов: возможно, бесконечный цикл",
                    position);
        }
        if (debugger != null && env != null && !frames.isEmpty()) {
            FrameInfo top = frames.get(frames.size() - 1);
            top.env = env;
            top.position = position;
            if (debugState == null) {
                debugState = new DebugState(this);
            }
            debugger.statement(debugState);
        }
    }

    Value call(FunctionValue function, Value[] args, SourcePosition callSite) {
        int arity = function.arity();
        if (arity != FunctionValue.VARIADIC && arity != args.length) {
            throw new ScriptException(
                    "функция "
                            + function.name()
                            + " ожидает "
                            + arity
                            + " "
                            + arguments(arity)
                            + ", передано "
                            + args.length,
                    callSite);
        }
        if (++depth > maxDepth) {
            depth--;
            throw new ScriptException(
                    "слишком глубокая рекурсия (больше " + maxDepth + " вложенных вызовов)",
                    callSite);
        }
        boolean frame = debugger != null && function instanceof UserFunction;
        if (frame) {
            frames.add(new FrameInfo(function.name()));
        }
        try {
            return function.invoke(this, args);
        } catch (ScriptException e) {
            e.at(callSite);
            if (function instanceof UserFunction user) {
                e.addFrame(
                        "в функции "
                                + user.name()
                                + (callSite == null
                                        ? " (вызвана игрой)"
                                        : " (вызов в " + callSite + ")"));
            }
            throw e;
        } catch (StackOverflowError e) {
            throw new ScriptException("переполнение стека: слишком глубокая рекурсия", callSite);
        } finally {
            depth--;
            if (frame) {
                frames.remove(frames.size() - 1);
            }
        }
    }

    private static String arguments(int n) {
        int mod100 = n % 100;
        int mod10 = n % 10;
        if (mod10 == 1 && mod100 != 11) {
            return "аргумент";
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return "аргумента";
        }
        return "аргументов";
    }
}
