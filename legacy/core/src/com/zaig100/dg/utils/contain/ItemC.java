package com.zaig100.dg.utils.contain;

import org.json.simple.JSONObject;

public class ItemC extends ObjC {
    JSONObject item;
    boolean active;

    public ItemC(int x, int y, JSONObject item, boolean active, String tag) {
        super(x, y, tag);
        this.item = item;
        this.active = active;
    }

    public JSONObject getItem() {
        return item;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "ItemC{" +
                "item=" + item +
                ", active=" + active +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
