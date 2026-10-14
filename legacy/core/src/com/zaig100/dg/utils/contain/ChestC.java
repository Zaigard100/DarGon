package com.zaig100.dg.utils.contain;

import com.zaig100.dg.world.elements.items.Item;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class ChestC extends ObjC {
    ArrayList<Item> items = new ArrayList<>();
    boolean isLoked;
    boolean open;
    String keyTag = "open";

    public ChestC(int x, int y, JSONArray items, String tag) {
        super(x, y, tag);
        for (Object item : items) {
            if (item instanceof JSONObject) {
                this.items.add(Item.jsonToItem((JSONObject) item));
            }
        }
        isLoked = false;
        open = false;
    }

    public ChestC(int x, int y, JSONArray items, boolean isLoked, String keyTag, boolean open, String tag) {
        super(x, y, tag);
        for (Object item : items) {
            if (item instanceof JSONObject) {
                this.items.add(Item.jsonToItem((JSONObject) item));
            }
        }
        this.keyTag = keyTag;
        this.isLoked = isLoked;
        this.open = open;
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public boolean isLoked() {
        return isLoked;
    }

    public boolean isOpen() {
        return open;
    }

    public String getKeyTag() {
        return keyTag;
    }

    @Override
    public String toString() {
        return "ChestC{" +
                "items=" + items +
                ", isLoked=" + isLoked +
                ", open=" + open +
                ", keyTag='" + keyTag + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
