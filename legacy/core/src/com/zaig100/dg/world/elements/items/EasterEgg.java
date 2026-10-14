package com.zaig100.dg.world.elements.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.zaig100.dg.screen.extensions.Render3D;
import com.zaig100.dg.screen.game.LevelModScreen;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.world.World;

import org.json.simple.JSONObject;

public class EasterEgg extends Item {

    String code;

    public EasterEgg(String code) {
        type = ItemType.EGG;
        this.code = code;
    }

    @Override
    public void render(Batch batch, int x, int y) {
        switch (code) {
            case "4601025111298":
                batch.draw(
                        Res.cube,
                        x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                        y * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                        16 * Configuration.getScale() - 6 * Configuration.getScale(),
                        16 * Configuration.getScale() - 6 * Configuration.getScale()
                );
                break;
            case "jokkeer_lovushcera":
                batch.draw(
                        Res.turn_off,
                        x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                        y * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                        16 * Configuration.getScale() - 6 * Configuration.getScale(),
                        16 * Configuration.getScale() - 6 * Configuration.getScale()
                );
                break;
            case "inf":
                batch.draw(
                        Res.sinf,
                        x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                        y * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                        16 * Configuration.getScale() - 6 * Configuration.getScale(),
                        16 * Configuration.getScale() - 6 * Configuration.getScale()
                );
                if (World.player.inf) {
                    batch.draw(
                            Res.green_obj,
                            x * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                            y * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                            16 * Configuration.getScale() - 6 * Configuration.getScale(),
                            16 * Configuration.getScale() - 6 * Configuration.getScale()
                    );
                }
                break;
        }
    }

    @Override
    public void renderInMap(Batch batch, int wX, int wY) {
        switch (code) {
            case "4601025111298":
                batch.draw(Res.cube,
                        wX + (0.25f * 16 * Configuration.getScale()) - World.player.get_wX(),
                        wY + (0.25f * 16 * Configuration.getScale()) - World.player.get_wY(),
                        16 * Configuration.getScale() * 0.5f,
                        16 * Configuration.getScale() * 0.5f
                );
                break;
            case "jokkeer_lovushcera":
                batch.draw(Res.turn_off,
                        wX + (0.25f * 16 * Configuration.getScale()) - World.player.get_wX(),
                        wY + (0.25f * 16 * Configuration.getScale()) - World.player.get_wY(),
                        16 * Configuration.getScale() * 0.5f,
                        16 * Configuration.getScale() * 0.5f
                );
                break;
            case "inf":
                batch.draw(Res.sinf,
                        wX + (0.25f * 16 * Configuration.getScale()) - World.player.get_wX(),
                        wY + (0.25f * 16 * Configuration.getScale()) - World.player.get_wY(),
                        16 * Configuration.getScale() * 0.5f,
                        16 * Configuration.getScale() * 0.5f
                );
                break;
        }
    }

    @Override
    public boolean use() {
        switch (code) {
            case "4601025111298":
                LevelModScreen.m.setScreen(new Render3D(LevelModScreen.m));
                break;
            case "jokkeer_lovushcera":
                Gdx.app.exit();
                break;
            case "inf":
                World.player.inf = true;
                return false;
        }
        return true;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("Type", type.toString());
        json.put("Code", code);
        return json;
    }
}
