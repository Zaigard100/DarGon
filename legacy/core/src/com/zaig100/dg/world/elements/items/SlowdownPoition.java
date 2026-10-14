package com.zaig100.dg.world.elements.items;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.world.World;

import org.json.simple.JSONObject;

public class SlowdownPoition extends Item {

    float power;

    public SlowdownPoition(int count, float power) {
        super(count);
        type = ItemType.SLOWDOWN_POITION;
        if(power<1){
            power = 1;
        }
        this.power = power;
    }

    @Override
    public void render(Batch batch, int x, int y) {
        batch.draw(
                Res.slowdown_potion,
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
        batch.draw(Res.slowdown_potion,
                wX + (0.25f * 16 * Configuration.getScale()) - World.player.get_wX(),
                wY + (0.25f * 16 * Configuration.getScale()) - World.player.get_wY(),
                16 * Configuration.getScale() * 0.5f,
                16 * Configuration.getScale() * 0.5f
        );
    }

    @Override
    public boolean use() {
        if(World.player.speed>1/power){
            World.player.speed = 1/power;
            return true;
        }
        return false;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("Type", type.toString());
        json.put("Count", count);
        json.put("Power", power);
        return json;
    }
}
