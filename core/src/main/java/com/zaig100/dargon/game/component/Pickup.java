package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;
import com.zaig100.dargon.game.item.ItemStack;

/** Предмет на полу. */
public final class Pickup implements Component {
    public ItemStack item;

    @Override
    public void reset() {
        item = null;
    }
}
