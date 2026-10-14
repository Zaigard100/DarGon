package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.eachGrid;
import static com.zaig100.dargon.ui.Ui.image;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.game.item.Inventory;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.ui.FocusNavigator;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.Signal;

/**
 * Инвентарь: сетка 4×3. Фокус — на ячейке; «использовать» применяет предмет, Q выбрасывает один.
 * Ниже — название и описание выбранного предмета и подсказка с клавишами. Крестик в углу закрывает
 * (на сенсорном экране другой кнопки нет).
 */
final class InventoryScreen extends UiScreen {

    private static final int COLUMNS = 4;
    private static final float DESCRIPTION_WIDTH = 96;

    /** Выбранная ячейка. */
    private final Signal<Integer> selected = Signal.of(0);

    @Override
    public Area area() {
        return Area.GAME;
    }

    @Override
    protected UiNode build() {
        var texts = app.texts();
        HudModel hud = app.gameplay().hud();
        var items = app.data().items();
        UiNode slots =
                eachGrid(
                        hud.inventory,
                        COLUMNS,
                        2,
                        slot -> slot.index(),
                        slot -> {
                            var b =
                                    button(
                                                    Widgets.itemSlot(items, slot.stack()),
                                                    () ->
                                                            app.gameplay()
                                                                    .actions()
                                                                    .use(slot.index()))
                                            .onFocus(() -> selected.set(slot.index()))
                                            .pad(0);
                            return slot.index() == selected.get() ? b.focused() : b;
                        });
        return layers(
                        Widgets.panel(
                                column(
                                        2,
                                        button(image("close").size(9, 9), this::back)
                                                .pad(0)
                                                .align(Align.right),
                                        slots,
                                        text(selected.combine(
                                                        hud.inventory.combine(
                                                                texts.localeSignal(),
                                                                (inv, l) -> inv),
                                                        (i, inv) -> describe(stack(i))))
                                                .style("small"),
                                        text(Widgets.keyName(app, Action.USE)
                                                        .combine(
                                                                Widgets.keyName(app, Action.DROP),
                                                                (use, drop) ->
                                                                        texts.ui(
                                                                                "inventory.hint",
                                                                                use,
                                                                                drop)))
                                                .style("hint"))))
                .background("dim")
                .expand();
    }

    private ItemStack stack(int slot) {
        Inventory inventory = app.gameplay().level().state().player().inventory;
        return inventory.get(slot);
    }

    private String describe(ItemStack stack) {
        if (stack == null) {
            return app.texts().ui("inventory.empty");
        }
        return app.texts().ui("item." + stack.id());
    }

    @Override
    public boolean onAction(Action action, FocusNavigator focus) {
        if (action == Action.INVENTORY) {
            back();
            return true;
        }
        if (action == Action.DROP) {
            app.gameplay().actions().drop(selected.get());
            return true;
        }
        return super.onAction(action, focus);
    }
}
