package com.zaig100.dg.world.elements.items;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.world.World;

import org.json.simple.JSONObject;

public class Torch extends Item {

    public Torch() {
        type = ItemType.TORCH;
    }

    public Torch(int count) {
        super(count);
        type = ItemType.TORCH;
    }

    @Override
    public void render(Batch batch, int x, int y) {
        batch.draw(
                Res.torch,
                x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                y * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                16 * Configuration.getScale() - 6 * Configuration.getScale(),
                16 * Configuration.getScale() - 6 * Configuration.getScale()
        );
        Res.getFont(3).draw(batch,
                String.valueOf(count),
                x * 16 * Configuration.getScale() + 28 * Configuration.getScale(),
                y * 16 * Configuration.getScale() + 22 * Configuration.getScale()
        );
    }

    @Override
    public void renderInMap(Batch batch, int wX, int wY) {
        batch.draw(Res.torch,
                wX + (0.25f * 16 * Configuration.getScale()) - World.player.get_wX(),
                wY + (0.25f * 16 * Configuration.getScale()) - World.player.get_wY(),
                16 * Configuration.getScale() * 0.5f,
                16 * Configuration.getScale() * 0.5f
        );
    }

    @Override
    public boolean use() {
        if (!World.map.isDark()) {
            return false;
        }
        World.map.setDark(false);
        return true;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("Type", type.toString());
        json.put("Count", count);
        return json;
    }

}
