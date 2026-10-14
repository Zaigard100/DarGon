package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;
import java.util.ArrayList;
import java.util.List;

/** Текст таблички: строки или ключи локализации {@code "@level.05.tablet1"}. */
public final class Text implements Component {
    public final List<String> lines = new ArrayList<>();

    @Override
    public void reset() {
        lines.clear();
    }
}
