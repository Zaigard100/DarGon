package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;
import java.util.ArrayList;
import java.util.List;

/** Групповые метки для скриптов. */
public final class Tags implements Component {
    public final List<String> tags = new ArrayList<>();

    @Override
    public void reset() {
        tags.clear();
    }
}
