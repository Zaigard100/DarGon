package com.zaig100.dg.world.elements.items;

import com.badlogic.gdx.graphics.g2d.Batch;

import org.json.simple.JSONObject;

public class Empty extends Item {

    public Empty() {
        type = ItemType.EMPTY;
    }

    @Override
    public void render(Batch butch, int x, int y) {

    }

    @Override
    public void renderInMap(Batch batch, int wX, int wY) {

    }

    @Override
    public boolean use() {
        return false;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("Type", type.toString());
        return json;
    }
}
