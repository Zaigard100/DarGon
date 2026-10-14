package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;
import com.zaig100.dargon.level.Direction;

/** Стреляет стрелами (арбалет) или выпускает пламя (огнемёт). Направление — {@link Facing}. */
public final class Emitter implements Component {

    /** Вид: {@code "arrow"} или {@code "fire"}. */
    public String kind;

    public Direction dir = Direction.RIGHT;
    public float period;

    /** Огнемёт: максимальная длина пламени. */
    public int maxLength;

    /** Огнемёт: текущая длина пламени (0 — погашен). */
    public int stage;

    public float timer;

    /** Огнемёт: кадр анимации огня и его таймер. */
    public int fireFrame;

    public float fireTimer;

    /** Арбалет уже выпустил первую стрелу. */
    public boolean started;

    @Override
    public void reset() {
        kind = null;
        dir = Direction.RIGHT;
        period = 0;
        maxLength = 0;
        stage = 0;
        timer = 0;
        fireFrame = 0;
        fireTimer = 0;
        started = false;
    }
}
