package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;
import com.zaig100.dargon.game.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Сундук: содержимое и состояние. */
public final class Container implements Component {
    public final List<ItemStack> items = new ArrayList<>();
    public boolean open;

    @Override
    public void reset() {
        items.clear();
        open = false;
    }
}
