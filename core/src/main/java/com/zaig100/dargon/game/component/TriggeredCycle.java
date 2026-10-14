package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/**
 * Шипы: наступили — через {@code delay} секунд выдвигаются; выдвинутые убирают себя через {@code 2
 * × delay}, если на них снова не наступили. Повторяет таймеры DarGon3.
 */
public final class TriggeredCycle implements Component {
    public boolean active;
    public boolean triggered;
    public float delay;

    /** Время с момента, как на шипы наступили. */
    public float timer;

    /** Время, сколько шипы выдвинуты без нового наступания. */
    public float activeTimer;

    @Override
    public void reset() {
        active = false;
        triggered = false;
        delay = 0;
        timer = 0;
        activeTimer = 0;
    }
}
