package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.GameSystem;
import com.zaig100.dargon.game.script.ScriptHost;

/** Таймеры скриптов уровня ({@code after}, {@code every}) идут игровым временем. */
public final class ScriptSystem extends GameSystem {

    private ScriptHost scripts;

    @Override
    protected void update(float delta) {
        scripts.update(delta);
    }
}
