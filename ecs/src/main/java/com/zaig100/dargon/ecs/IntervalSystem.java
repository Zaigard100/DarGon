package com.zaig100.dargon.ecs;

/**
 * Система, которая срабатывает с фиксированным интервалом игрового времени, а не каждый тик.
 *
 * <p>Если за тик прошло несколько интервалов, {@link #tick()} вызывается несколько раз — время не
 * теряется.
 */
public abstract class IntervalSystem extends GameSystem {

    private final float interval;
    private float accumulated;

    protected IntervalSystem(float interval) {
        if (!(interval > 0f)) {
            throw new IllegalArgumentException("Интервал должен быть больше нуля: " + interval);
        }
        this.interval = interval;
    }

    @Override
    protected final void update(float delta) {
        accumulated += delta;
        while (accumulated >= interval) {
            accumulated -= interval;
            tick();
        }
    }

    /** Одно срабатывание. */
    protected abstract void tick();

    public float interval() {
        return interval;
    }
}
