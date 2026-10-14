package com.zaig100.dg.world.elements.items;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.world.World;

import org.json.simple.JSONObject;

public class Money extends Item {

    public Money(int count) {
        super(count);
        type = ItemType.MONEY;
    }


    @Override
    public void render(Batch batch, int x, int y) {
        if (count <= 10) {
            batch.draw(
                    Res.money,
                    x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                    y * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                    16 * Configuration.getScale() - 6 * Configuration.getScale(),
                    16 * Configuration.getScale() - 6 * Configuration.getScale()
            );
        } else if (count <= 50) {
            batch.draw(
                    Res.bundle,
                    x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                    y * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                    16 * Configuration.getScale() - 6 * Configuration.getScale(),
                    16 * Configuration.getScale() - 6 * Configuration.getScale()
            );
        } else {
            batch.draw(
                    Res.chest(false),
                    x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                    y * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                    16 * Configuration.getScale() - 6 * Configuration.getScale(),
                    16 * Configuration.getScale() - 6 * Configuration.getScale()
            );
        }

        Res.getFont(3).draw(batch,
                String.valueOf(count),
                x * 16 * Configuration.getScale() + 28 * Configuration.getScale(),
                y * 16 * Configuration.getScale() + 22 * Configuration.getScale()
        );

    }

    @Override
    public void renderInMap(Batch batch, int wX, int wY) {
        if (count <= 10) {
            batch.draw(Res.money,
                    wX + (0.25f * 16 * Configuration.getScale()) - World.player.get_wX(),
                    wY + (0.25f * 16 * Configuration.getScale()) - World.player.get_wY(),
                    16 * Configuration.getScale() * 0.5f,
                    16 * Configuration.getScale() * 0.5f
            );
        } else if (count <= 50) {
            batch.draw(Res.bundle,
                    wX + (0.25f * 16 * Configuration.getScale()) - World.player.get_wX(),
                    wY + (0.25f * 16 * Configuration.getScale()) - World.player.get_wY(),
                    16 * Configuration.getScale() * 0.5f,
                    16 * Configuration.getScale() * 0.5f
            );
        } else {
            batch.draw(Res.chest(false),
                    wX + (0.25f * 16 * Configuration.getScale()) - World.player.get_wX(),
                    wY + (0.25f * 16 * Configuration.getScale()) - World.player.get_wY(),
                    16 * Configuration.getScale() * 0.5f,
                    16 * Configuration.getScale() * 0.5f
            );
        }
    }

    @Override
    public boolean use() {
        if (World.player.getHp() > 0) {
            World.player.coin_count += count;
            count = 0;
            return true;
        }
        return false;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("Type", type.toString());
        json.put("Count", count);
        return json;
    }
}
