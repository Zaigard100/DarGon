package com.zaig100.dargon.editor;

import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.game.Cheats;
import com.zaig100.dargon.game.GameLog;
import com.zaig100.dargon.game.LevelOutcome;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.ScriptHandlers;
import com.zaig100.dargon.game.script.ScriptHost;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.script.DebugState;
import com.zaig100.dargon.script.Debugger;
import com.zaig100.dargon.script.ScriptException;
import com.zaig100.dargon.script.SourcePosition;
import com.zaig100.dargon.script.Value;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;

/**
 * Запуск уровня под отладчиком: свой мир уровня (песочница), скрипты в отдельном потоке.
 *
 * <p>Потоки никогда не работают с миром одновременно: интерфейс отдаёт задачу (запуск, тик,
 * обработчик, код консоли) и ждёт, пока она не закончится или скрипт не остановится на точке
 * остановки. Пока скрипт стоит, интерфейс читает мир и снимок переменных; «Продолжить» и шаги снова
 * отдают управление потоку скрипта.
 */
final class DebugSession implements Debugger {

    /** Как идти дальше после остановки. */
    enum Mode {
        /** До точки остановки. */
        RUN,
        /** Следующая инструкция, в том числе внутри вызванной функции. */
        INTO,
        /** Следующая инструкция этой функции (вызовы — целиком). */
        OVER,
        /** До возврата из этой функции. */
        OUT
    }

    /** Строка консоли: вывод {@code print}, ошибка или служебное сообщение. */
    record Line(String text, Kind kind) {}

    enum Kind {
        INFO,
        ERROR,
        SYSTEM,
        INPUT
    }

    /** Кадр стека в момент остановки: значения уже переведены в текст. */
    record Frame(String function, SourcePosition position, Map<String, String> locals) {}

    /** Снимок остановки — читается интерфейсом, пока скрипт стоит. */
    record Pause(SourcePosition position, List<Frame> frames, Map<String, String> globals) {}

    private static final long WAIT_MS = 5000;

    private final ConcurrentLinkedQueue<Line> output = new ConcurrentLinkedQueue<>();
    private final BiPredicate<String, Integer> breakpoint;
    private final ActionState input = new ActionState();
    private final List<Runnable> inputChanges = new ArrayList<>();
    private final Thread worker;

    // Под монитором this.
    private Runnable pending;
    private boolean busy;
    private boolean paused;
    private boolean stopping;
    private Pause pause;
    private Mode mode = Mode.RUN;
    private int baseDepth;

    private LevelRuntime runtime;
    private ScriptHost host;
    private boolean finished;

    /**
     * @param breakpoint есть ли точка остановки: файл скрипта (как в сообщениях игры) и строка
     */
    DebugSession(BiPredicate<String, Integer> breakpoint) {
        this.breakpoint = breakpoint;
        worker = new Thread(this::work, "dgscript-debug");
        worker.setDaemon(true);
        worker.start();
    }

    // ------------------------------------------------------------------ интерфейс

    /**
     * Запустить уровень: скрипт уровня выполняется сразу (может остановиться).
     *
     * @param library общие скрипты пака для {@code use}: путь относительно каталога уровней — текст
     *     или {@code null}
     */
    void start(
            LevelDefinition level,
            String script,
            java.util.function.Function<String, String> library,
            ItemCatalog items) {
        system("Запуск уровня " + level.id() + (script == null ? " (без скрипта уровня)" : ""));
        GameLog log =
                new GameLog() {
                    @Override
                    public void info(String message) {
                        output.add(new Line(message, Kind.INFO));
                    }

                    @Override
                    public void error(String message) {
                        output.add(new Line(message, Kind.ERROR));
                    }
                };
        run(
                () -> {
                    runtime =
                            LevelRuntime.start(
                                    level,
                                    PlayerState.newGame(items),
                                    input,
                                    script,
                                    library,
                                    log,
                                    new Cheats(),
                                    new Random(),
                                    this);
                    host = runtime.world().resource(ScriptHost.class);
                });
    }

    /**
     * Мир песочницы; {@code null}, пока уровень не запущен. Читать — только когда {@link #canRead}.
     */
    LevelRuntime runtime() {
        return runtime;
    }

    /** Мир можно читать: скрипт ничего не выполняет или стоит на остановке. */
    synchronized boolean canRead() {
        return runtime != null && (!busy || paused);
    }

    /** Можно отдать новую задачу: поток скрипта свободен. */
    synchronized boolean isIdle() {
        return !busy && runtime != null && !stopping;
    }

    synchronized boolean isPaused() {
        return paused;
    }

    synchronized Pause pause() {
        return paused ? pause : null;
    }

    /** Уровень закончился (выход или смерть героя) — тики больше не идут. */
    boolean isFinished() {
        return finished;
    }

    /** Новые строки консоли (забирает их). */
    List<Line> drain() {
        List<Line> lines = new ArrayList<>();
        for (Line l; (l = output.poll()) != null; ) {
            lines.add(l);
        }
        return lines;
    }

    void system(String text) {
        output.add(new Line(text, Kind.SYSTEM));
    }

    /** Продолжить после остановки. */
    void resume(Mode next) {
        synchronized (this) {
            if (!paused) {
                return;
            }
            mode = next;
            baseDepth = depthAtPause;
            paused = false;
            pause = null;
            notifyAll();
        }
        waitForWorker();
    }

    /** {@code n} тиков игры (время идёт): с накопленными нажатиями клавиш. */
    void tick(int n) {
        List<Runnable> changes;
        synchronized (inputChanges) {
            changes = new ArrayList<>(inputChanges);
            inputChanges.clear();
        }
        run(
                () -> {
                    for (Runnable change : changes) {
                        change.run();
                    }
                    for (int i = 0; i < n && !finished; i++) {
                        runtime.tick();
                        LevelOutcome outcome = runtime.outcome();
                        if (outcome != null && outcome.kind() != LevelOutcome.Kind.NONE) {
                            finished = true;
                            system(
                                    outcome.next() == null
                                            ? "Герой ушёл по лестнице: игра пройдена"
                                            : "Герой ушёл по лестнице на уровень "
                                                    + outcome.next());
                        } else if (runtime.isPlayerDead()) {
                            finished = true;
                            system("Герой погиб");
                        }
                    }
                });
    }

    /** Нажатие или отпускание действия героя — применится в начале следующего тика. */
    void action(Action action, boolean down) {
        synchronized (inputChanges) {
            inputChanges.add(
                    () -> {
                        if (down) {
                            input.press(action);
                        } else {
                            input.release(action);
                        }
                    });
        }
    }

    /** Обработчик события сущности, как будто герой её использовал. */
    void fire(int entity, String event) {
        run(() -> host.fire(entity, event));
    }

    /** Код консоли в глобальной области скриптов уровня. */
    void evaluate(String code) {
        output.add(new Line("> " + code, Kind.INPUT));
        run(
                () -> {
                    try {
                        Value v = host.evaluate(code, "консоль");
                        if (v != null && !"null".equals(v.typeName())) {
                            output.add(new Line("= " + show(v), Kind.INFO));
                        }
                    } catch (ScriptException e) {
                        // Место в служебной обёртке консоли ничего не скажет — только текст.
                        StringBuilder text = new StringBuilder(e.reason());
                        for (String frame : e.scriptTrace()) {
                            text.append("\n    ").append(frame);
                        }
                        output.add(
                                new Line(
                                        stopping ? "выполнение остановлено" : text.toString(),
                                        Kind.ERROR));
                    }
                });
    }

    /** Сущность со скриптом-обработчиком в клетке (для щелчка по песочнице) или -1. */
    int handlerAt(int x, int y, String event) {
        EntitySet set = runtime.world().entities(Family.all(ScriptHandlers.class, GridPos.class));
        for (int i = 0; i < set.size(); i++) {
            int e = set.get(i);
            GridPos p = runtime.world().get(e, GridPos.class);
            if (p.x == x
                    && p.y == y
                    && runtime.world().get(e, ScriptHandlers.class).code.containsKey(event)) {
                return e;
            }
        }
        return -1;
    }

    /** Остановить всё: скрипт прерывается, поток завершается, мир освобождается. */
    void close() {
        synchronized (this) {
            stopping = true;
            paused = false;
            notifyAll();
        }
        try {
            worker.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!worker.isAlive() && runtime != null) {
            runtime.dispose();
        }
    }

    // ------------------------------------------------------------------ поток скрипта

    /** Отдать задачу потоку скрипта и ждать её конца или остановки. */
    private void run(Runnable task) {
        synchronized (this) {
            if (busy || stopping) {
                return;
            }
            pending = task;
            busy = true;
            notifyAll();
        }
        waitForWorker();
    }

    private synchronized void waitForWorker() {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        while (busy && !paused) {
            long left = deadline - System.currentTimeMillis();
            if (left <= 0) {
                output.add(new Line("Скрипт выполняется слишком долго", Kind.ERROR));
                return;
            }
            try {
                wait(left);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void work() {
        while (true) {
            Runnable task;
            synchronized (this) {
                while (pending == null && !stopping) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (stopping) {
                    return;
                }
                task = pending;
                pending = null;
            }
            try {
                task.run();
            } catch (ScriptException e) {
                if (!stopping) {
                    output.add(new Line(e.getMessage(), Kind.ERROR));
                }
            } catch (RuntimeException e) {
                output.add(new Line("Ошибка игры: " + e, Kind.ERROR));
            }
            synchronized (this) {
                busy = false;
                mode = Mode.RUN;
                notifyAll();
            }
        }
    }

    private int depthAtPause;

    /** Перед каждой инструкцией скрипта (поток скрипта): остановиться ли здесь. */
    @Override
    public void statement(DebugState state) {
        if (stopping) {
            throw new ScriptException("выполнение остановлено");
        }
        SourcePosition at = state.position();
        boolean stop =
                switch (mode) {
                    case RUN -> breakpoint.test(at.file(), at.line());
                    case INTO -> true;
                    case OVER -> state.depth() <= baseDepth;
                    case OUT -> state.depth() < baseDepth;
                };
        if (!stop && mode != Mode.RUN && breakpoint.test(at.file(), at.line())) {
            stop = true; // точка остановки внутри перешагнутой функции
        }
        if (!stop) {
            return;
        }
        Pause snapshot = snapshot(state);
        synchronized (this) {
            pause = snapshot;
            depthAtPause = state.depth();
            paused = true;
            notifyAll();
            while (paused && !stopping) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    stopping = true;
                }
            }
        }
        if (stopping) {
            throw new ScriptException("выполнение остановлено");
        }
    }

    private static Pause snapshot(DebugState state) {
        List<Frame> frames = new ArrayList<>();
        for (DebugState.Frame f : state.frames()) {
            frames.add(new Frame(f.function(), f.position(), texts(f.locals())));
        }
        return new Pause(state.position(), frames, texts(state.globals()));
    }

    private static Map<String, String> texts(Map<String, Value> values) {
        Map<String, String> result = new LinkedHashMap<>();
        new java.util.TreeMap<>(values)
                .forEach(
                        (name, v) -> {
                            if (!name.startsWith("__")) {
                                result.put(name, show(v));
                            }
                        });
        return result;
    }

    /** Значение для человека: строки в кавычках, длинное обрезано. */
    static String show(Value v) {
        String text;
        try {
            text = v.display();
        } catch (RuntimeException e) {
            text = "<" + v.typeName() + ">";
        }
        if ("string".equals(v.typeName())) {
            text = "\"" + text + "\"";
        }
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }
}
