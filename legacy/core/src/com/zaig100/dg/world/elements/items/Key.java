package com.zaig100.dg.world.elements.items;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.world.World;

import org.json.simple.JSONObject;

public class Key extends Item {

    Sprite spr = Res.key;
    String keyTag;
    float r, g, b;

    public Key(String keyTag, float r, float g, float b) {
        this.keyTag = keyTag;
        type = ItemType.KEY;
        count = 1;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void render(Batch batch, int x, int y) {
        batch.setColor(r, g, b, 1f);
        batch.draw(
                spr,
                x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                y * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                16 * Configuration.getScale() - 6 * Configuration.getScale(),
                16 * Configuration.getScale() - 6 * Configuration.getScale()
        );
        batch.setColor(1f, 1f, 1f, 1f);
        Res.getFont(3).draw(batch,
                keyTag,
                x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                y * 16 * Configuration.getScale() + 22 * Configuration.getScale()
        );
    }

    @Override
    public void renderInMap(Batch batch, int wX, int wY) {
        batch.setColor(r, g, b, 1f);
        batch.draw(spr,
                wX + (0.25f * 16 * Configuration.getScale()) - World.player.get_wX(),
                wY + (0.25f * 16 * Configuration.getScale()) - World.player.get_wY(),
                16 * Configuration.getScale() * 0.5f,
                16 * Configuration.getScale() * 0.5f
        );
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean use() {
        return false;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("Type", type.toString());
        json.put("KeyTag", keyTag);
        json.put("R", r);
        json.put("G", g);
        json.put("B", b);
        return json;
    }

    public String getKeyTag() {
        return keyTag;
    }
}
