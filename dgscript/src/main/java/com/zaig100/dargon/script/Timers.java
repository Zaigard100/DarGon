package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.List;

/**
 * Таймеры скриптов: {@code after(sec, fn)}, {@code every(sec, fn)}, {@code cancel(id)}.
 *
 * <p>Время идёт только когда хост вызывает {@link #update(float)} (в игре — раз в тик), поэтому
 * пауза игры автоматически ставит таймеры на паузу, а тесты детерминированы. Обратные вызовы
 * выполняются в потоке хоста с отдельным лимитом шагов на каждый вызов.
 */
public final class Timers {

    private static final class Timer {
        final int id;
        final FunctionValue callback;
        final double period;
        double remaining;
        boolean cancelled;

        Timer(int id, FunctionValue callback, double delay, double period) {
            this.id = id;
            this.callback = callback;
            this.remaining = delay;
            this.period = period;
        }
    }

    private final ScriptEngine engine;
    private final List<Timer> timers = new ArrayList<>();
    private final List<Timer> due = new ArrayList<>();
    private int nextId = 1;

    Timers(ScriptEngine engine) {
        this.engine = engine;
    }

    /** Однократный вызов через {@code seconds}. Возвращает id для {@link #cancel}. */
    public int after(double seconds, FunctionValue callback) {
        return schedule(seconds, 0, callback);
    }

    /** Периодический вызов каждые {@code seconds} (первый — через {@code seconds}). */
    public int every(double seconds, FunctionValue callback) {
        if (!(seconds > 0)) {
            throw new ScriptException("every: период должен быть больше нуля");
        }
        return schedule(seconds, seconds, callback);
    }

    public boolean cancel(int id) {
        for (Timer timer : timers) {
            if (timer.id == id && !timer.cancelled) {
                timer.cancelled = true;
                return true;
            }
        }
        return false;
    }

    /** Отменяет все таймеры (например, при выходе с уровня). */
    public void clear() {
        for (Timer timer : timers) {
            timer.cancelled = true;
        }
        timers.clear();
    }

    /** Число активных таймеров. */
    public int active() {
        int count = 0;
        for (Timer timer : timers) {
            if (!timer.cancelled) {
                count++;
            }
        }
        return count;
    }

    /**
     * Продвигает время и вызывает сработавшие таймеры в порядке срабатывания. Ошибка в обратном
     * вызове передаётся обработчику ошибок движка и не мешает остальным таймерам.
     */
    public void update(float delta) {
        due.clear();
        for (Timer timer : timers) {
            if (!timer.cancelled) {
                timer.remaining -= delta;
                if (timer.remaining <= 0) {
                    due.add(timer);
                }
            }
        }
        // Сначала те, что должны были сработать раньше; при равенстве — в порядке создания.
        due.sort(
                (a, b) ->
                        a.remaining != b.remaining
                                ? Double.compare(a.remaining, b.remaining)
                                : Integer.compare(a.id, b.id));
        for (Timer timer : due) {
            if (timer.cancelled) {
                continue; // отменён обратным вызовом другого таймера
            }
            if (timer.period > 0) {
                // Если за один update прошло несколько периодов, таймер срабатывает один раз.
                timer.remaining += timer.period;
                if (timer.remaining <= 0) {
                    timer.remaining = timer.period;
                }
            } else {
                timer.cancelled = true;
            }
            engine.callSafely(timer.callback);
        }
        timers.removeIf(timer -> timer.cancelled);
    }

    private int schedule(double delay, double period, FunctionValue callback) {
        if (Double.isNaN(delay) || delay < 0) {
            throw new ScriptException("задержка таймера должна быть неотрицательной: " + delay);
        }
        int id = nextId++;
        timers.add(new Timer(id, callback, delay, period));
        return id;
    }
}
