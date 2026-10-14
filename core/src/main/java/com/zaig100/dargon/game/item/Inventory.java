package com.zaig100.dargon.game.item;

import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.ItemDefinition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Инвентарь игрока: 12 ячеек (4×3, как в DarGon3). Новый предмет сначала докладывается в ячейку с
 * таким же предметом, потом кладётся в первую пустую. Предметы с {@code maxStack == 1} (ключи,
 * пасхалки) не складываются.
 */
public final class Inventory {

    public static final int SIZE = 12;

    private final ItemCatalog catalog;
    private final ItemStack[] slots = new ItemStack[SIZE];

    public Inventory(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    /** Предмет в ячейке или {@code null}. */
    public ItemStack get(int slot) {
        return slots[slot];
    }

    public void set(int slot, ItemStack stack) {
        slots[slot] = stack;
    }

    /** Непустые ячейки по порядку. */
    public List<ItemStack> items() {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : slots) {
            if (stack != null) {
                result.add(stack);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Поместится ли стопка целиком. */
    public boolean canAdd(ItemStack stack) {
        return copy().add(stack);
    }

    /**
     * Кладёт стопку целиком. Если целиком не помещается — инвентарь не меняется.
     *
     * @return {@code true}, если положено
     */
    public boolean add(ItemStack stack) {
        ItemStack[] backup = slots.clone();
        int left = stack.count();
        int max = maxStack(stack.id());
        for (int i = 0; i < SIZE && left > 0; i++) {
            if (slots[i] != null && slots[i].stacksWith(stack) && slots[i].count() < max) {
                int put = Math.min(left, max - slots[i].count());
                slots[i] = slots[i].withCount(slots[i].count() + put);
                left -= put;
            }
        }
        for (int i = 0; i < SIZE && left > 0; i++) {
            if (slots[i] == null) {
                int put = Math.min(left, max);
                slots[i] = stack.withCount(put);
                left -= put;
            }
        }
        if (left > 0) {
            System.arraycopy(backup, 0, slots, 0, SIZE);
            return false;
        }
        return true;
    }

    /** Убирает {@code count} штук из ячейки. */
    public void take(int slot, int count) {
        ItemStack stack = slots[slot];
        if (stack == null || count > stack.count()) {
            throw new IllegalArgumentException("В ячейке " + slot + " нет " + count + " штук");
        }
        slots[slot] = count == stack.count() ? null : stack.withCount(stack.count() - count);
    }

    /** Сколько всего предметов этого типа. */
    public int count(String id) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.id().equals(id)) {
                total += stack.count();
            }
        }
        return total;
    }

    public Inventory copy() {
        Inventory copy = new Inventory(catalog);
        System.arraycopy(slots, 0, copy.slots, 0, SIZE);
        return copy;
    }

    private int maxStack(String id) {
        ItemDefinition definition = catalog.get(id);
        return definition == null ? Integer.MAX_VALUE : definition.maxStack();
    }

    @Override
    public String toString() {
        return Arrays.toString(slots);
    }
}
