package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Уникальный id сущности для скриптов: {@code entity('door1')}. */
public final class EntityId implements Component {
    public String id;

    @Override
    public void reset() {
        id = null;
    }
}
