package com.zaig100.dargon.game;

import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.game.item.Inventory;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.level.ItemCatalog;
import java.util.List;

/** То, что игрок переносит с уровня на уровень: здоровье, монеты, инвентарь. */
public final class PlayerState {

    /** Максимум здоровья. */
    public static final int MAX_HP = Balance.MAX_HP;

    public int hp;
    public int coins;
    public final Inventory inventory;

    /** Прочность щита (0 — щита нет); держится до поломки и переходит между уровнями. */
    public int shield;

    public PlayerState(int hp, int coins, Inventory inventory) {
        this.hp = hp;
        this.coins = coins;
        this.inventory = inventory;
    }

    /** Новая игра в обычном режиме: 3 зелья, 2 щита, 1 факел. */
    public static PlayerState newGame(ItemCatalog items) {
        return newGame(items, GameMode.NORMAL);
    }

    /**
     * Новая игра: полное здоровье и стартовый набор режима — казуал 4/3/2, обычный 3/2/1, хардкор
     * 1/0/1 (зелья, щиты, факелы).
     */
    public static PlayerState newGame(ItemCatalog items, GameMode mode) {
        int[] kit =
                switch (mode) {
                    case CASUAL -> new int[] {4, 3, 2};
                    case HARDCORE -> new int[] {1, 0, 1};
                    default -> new int[] {3, 2, 1};
                };
        Inventory inventory = new Inventory(items);
        inventory.add(ItemStack.of("potion", kit[0]));
        if (kit[1] > 0) {
            inventory.add(ItemStack.of("shield", kit[1]));
        }
        inventory.add(ItemStack.of("torch", kit[2]));
        return new PlayerState(MAX_HP, 0, inventory);
    }

    /** Новая игра со стартовым набором из пака (стопки предметов в формате уровня). */
    public static PlayerState fromKit(ItemCatalog items, List<JsonValue> kit) {
        Inventory inventory = new Inventory(items);
        for (JsonValue stack : kit) {
            inventory.add(ItemStack.fromJson(stack));
        }
        return new PlayerState(MAX_HP, 0, inventory);
    }

    public PlayerState copy() {
        PlayerState copy = new PlayerState(hp, coins, inventory.copy());
        copy.shield = shield;
        return copy;
    }

    @Override
    public String toString() {
        return "PlayerState[hp=" + hp + ", coins=" + coins + ", " + inventory + "]";
    }
}
