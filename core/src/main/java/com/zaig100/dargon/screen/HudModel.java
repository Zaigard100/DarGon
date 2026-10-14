package com.zaig100.dargon.screen;

import com.zaig100.dargon.game.Interactions;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.game.item.Inventory;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.render.ItemIcons;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Состояние игры для интерфейса. Обновляется раз в кадр из мира; интерфейс подписан на сигналы и
 * перерисовывает только изменившееся. Интерфейс ничего не знает об ECS.
 */
public final class HudModel {

    /** Ячейка инвентаря: номер и предмет ({@code null} — пусто). */
    public record Slot(int index, ItemStack stack) {}

    public final Signal<Integer> hp = Signal.of(PlayerState.MAX_HP);
    public final Signal<Integer> coins = Signal.of(0);
    public final Signal<String> levelName = Signal.of("");
    public final Signal<Boolean> showLevelName = Signal.of(true);
    public final Signal<List<String>> message = Signal.of(List.of());
    public final Signal<List<Slot>> inventory = Signal.of(List.of());

    /** Прочность щита (0 — нет). */
    public final Signal<Integer> shield = Signal.of(0);

    /** Прохождение с инструментами разработчика (на HUD — пометка). */
    public final Signal<Boolean> cheats = Signal.of(false);

    /** Значок кнопки действия: что сделает «использовать» сейчас ({@code null} — ничего). */
    public final Signal<String> actionIcon = Signal.of(null);

    /** Окраска значка действия (ключ своего цвета) или {@code null}. */
    public final Signal<String> actionTint = Signal.of(null);

    void sync(LevelRuntime level, boolean leftSpawn, ItemCatalog items) {
        Interactions.Preview preview = Interactions.preview(level);
        actionIcon.set(icon(preview, items));
        actionTint.set(preview.item() == null ? null : ItemIcons.tint(preview.item()));
        hp.set(Math.max(0, Math.min(level.health().hp, PlayerState.MAX_HP)));
        shield.set(level.health().shield);
        coins.set(level.state().player().coins);
        shield.set(level.health().shield);
        levelName.set(level.state().definition().name());
        showLevelName.set(!leftSpawn);
        List<String> lines = level.state().message();
        if (!lines.equals(message.get())) {
            message.set(List.copyOf(lines));
        }
        Inventory inv = level.state().player().inventory;
        if (inventoryChanged(inv)) {
            List<Slot> slots = new ArrayList<>(Inventory.SIZE);
            for (int i = 0; i < Inventory.SIZE; i++) {
                slots.add(new Slot(i, inv.get(i)));
            }
            inventory.set(List.copyOf(slots));
        }
    }

    private static String icon(Interactions.Preview preview, ItemCatalog items) {
        switch (preview.kind()) {
            case EXIT:
                return "nextlevel";
            case TELEPORT:
                return "teleporter";
            case PICKUP:
                return ItemIcons.name(items, preview.item());
            case BUTTON:
                return "button";
            case DOOR:
                return "key";
            case CHEST:
                return "chest";
            case SHOP:
                return "shop";
            case TABLET:
                return "tablet";
            default:
                return null;
        }
    }

    /** Сравнение без выделения памяти: список меняется только при изменении инвентаря. */
    private boolean inventoryChanged(Inventory inv) {
        List<Slot> current = inventory.get();
        if (current.size() != Inventory.SIZE) {
            return true;
        }
        for (int i = 0; i < Inventory.SIZE; i++) {
            if (!Objects.equals(current.get(i).stack(), inv.get(i))) {
                return true;
            }
        }
        return false;
    }
}
