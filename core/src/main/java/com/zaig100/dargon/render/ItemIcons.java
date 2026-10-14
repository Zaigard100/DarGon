package com.zaig100.dargon.render;

import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.ItemDefinition;

/** Значок предмета; у монет он зависит от количества, как в DarGon3. */
public final class ItemIcons {

    private static final int SMALL_COINS = 10;
    private static final int MEDIUM_COINS = 50;

    private ItemIcons() {}

    public static String name(ItemCatalog items, ItemStack stack) {
        if (stack.id().equals(ItemStack.COINS)) {
            if (stack.count() <= SMALL_COINS) {
                return "money";
            }
            return stack.count() <= MEDIUM_COINS ? "bundle" : "chest";
        }
        ItemDefinition definition = items.get(stack.id());
        return definition == null ? "x" : definition.icon();
    }

    /** Цвет значка ({@code "#RRGGBB"}) или {@code null}. */
    public static String tint(ItemStack stack) {
        Object color = stack.field("color");
        return color instanceof String hex ? hex : null;
    }
}
