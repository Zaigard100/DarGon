package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.image;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.Device;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.ItemDefinition;
import com.zaig100.dargon.render.ItemIcons;
import com.zaig100.dargon.settings.Settings;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;
import java.util.List;

/** Повторяющиеся куски интерфейса. */
final class Widgets {

    /** Ячейка предмета: 16×16, значок 10×10. */
    static final int SLOT = 16;

    static final int ICON = 10;

    private Widgets() {}

    /** Панель с фоном и отступами. */
    static UiNode panel(UiNode content) {
        return layers(content).background("panel").pad(3);
    }

    /** Значок предмета (ключ — своего цвета) с количеством в углу. */
    static UiNode itemSlot(ItemCatalog items, ItemStack stack) {
        if (stack == null) {
            return layers(text("")).background("slot").size(SLOT, SLOT);
        }
        ItemDefinition definition = items.get(stack.id());
        boolean showCount = definition == null || definition.maxStack() > 1;
        return layers(
                        image(
                                        ReadSignal.constant(ItemIcons.name(items, stack)),
                                        ReadSignal.constant(ItemIcons.tint(stack)))
                                .size(ICON, ICON),
                        text(showCount ? String.valueOf(stack.count()) : "")
                                .style("tiny")
                                .align(Align.bottomRight))
                .background("slot")
                .size(SLOT, SLOT);
    }

    /**
     * Название кнопки действия для подсказки: клавиша ({@code "E"}) или кнопка геймпада ({@code
     * "A"}) — смотря чем игрок пользовался последним. Меняется при переназначении.
     */
    static ReadSignal<String> keyName(App app, Action action) {
        Settings settings = app.settings();
        return settings.keys.combine(
                app.input().device(),
                (keys, device) -> {
                    if (device == Device.GAMEPAD) {
                        return gamepadName(action);
                    }
                    List<Integer> codes = keys.get(action);
                    return codes == null || codes.isEmpty()
                            ? "—"
                            : Input.Keys.toString(codes.get(0));
                });
    }

    private static String gamepadName(Action action) {
        switch (action) {
            case USE:
                return "A";
            case PAUSE:
                return "B";
            case INVENTORY:
                return "Y";
            case DROP:
                return "X";
            default:
                return "+";
        }
    }
}
