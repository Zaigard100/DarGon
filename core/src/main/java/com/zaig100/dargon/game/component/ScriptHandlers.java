package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;
import java.util.HashMap;
import java.util.Map;

/** Код DGScript для событий сущности: {@code "use" → "entity('door1').enable()"}. */
public final class ScriptHandlers implements Component {
    public final Map<String, String> code = new HashMap<>();

    /**
     * Кнопку можно нажать: после нажатия — нет, пока игрок не уйдёт с её клетки (в DarGon3 нужно
     * было уйти по диагонали — это была ошибка).
     */
    public boolean armed = true;

    @Override
    public void reset() {
        code.clear();
        armed = true;
    }
}
