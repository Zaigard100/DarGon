package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Компоненты-метки без данных. */
public final class Marker {

    private Marker() {}

    /** Игрок. */
    public static final class Player implements Component {
        @Override
        public void reset() {}
    }

    /** Скрыт: не рисуется, пока не раскрыт (зелье «показать ловушки»). */
    public static final class Hidden implements Component {
        @Override
        public void reset() {}
    }

    /** Непроходимая сущность (закрытая дверь). */
    public static final class Blocking implements Component {
        @Override
        public void reset() {}
    }

    /** Выключенная сущность: не рисуется и не действует, пока скрипт её не включит. */
    public static final class Disabled implements Component {
        @Override
        public void reset() {}
    }
}
