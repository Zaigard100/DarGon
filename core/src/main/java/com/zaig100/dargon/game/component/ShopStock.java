package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;
import com.zaig100.dargon.game.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Товары магазина; у каждого — запас (магазины не бесконечные). */
public final class ShopStock implements Component {

    /** Товар, цена и сколько осталось. */
    public static final class Offer {
        private final ItemStack item;
        private final int price;
        private int stock;

        public Offer(ItemStack item, int price, int stock) {
            this.item = item;
            this.price = price;
            this.stock = stock;
        }

        public ItemStack item() {
            return item;
        }

        public int price() {
            return price;
        }

        public int stock() {
            return stock;
        }

        /** Продать одну штуку. */
        public void sell() {
            stock--;
        }
    }

    public final List<Offer> offers = new ArrayList<>();

    @Override
    public void reset() {
        offers.clear();
    }
}
