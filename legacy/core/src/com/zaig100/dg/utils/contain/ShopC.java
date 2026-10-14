package com.zaig100.dg.utils.contain;

import com.zaig100.dg.world.elements.items.Item;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;

public class ShopC extends ObjC {
    int[] cost = new int[3];
    Item[] items = new Item[3];

    public ShopC(int x, int y, JSONArray slots, String tag) {
        super(x, y, tag);
        if (slots.size() < 3) {
            throw new RuntimeException("In shop slot counnt < 3");
        }
        for (int i = 0; i < 3; i++) {
            items[i] = Item.jsonToItem((JSONObject) ((JSONObject) slots.get(i)).get("Item"));
            cost[i] = ((Number) ((JSONObject) slots.get(i)).get("Cost")).intValue();
        }
    }

    public int[] getCost() {
        return cost;
    }

    public Item[] getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "ShopC{" +
                "cost=" + Arrays.toString(cost) +
                ", items=" + Arrays.toString(items) +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
