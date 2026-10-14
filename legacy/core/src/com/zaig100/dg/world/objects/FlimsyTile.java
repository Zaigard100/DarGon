package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.ButtonС;
import com.zaig100.dg.utils.contain.FlimsyTileC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

public class FlimsyTile extends Obj {


    public int crashed_lvl;
    public float tick_sec;
    float timer = 0;



    public  FlimsyTile(FlimsyTileC contain){
        super(contain.getX(), contain.getY(), contain.getTag());
        type = ObjType.FLIMSY_TILE;
        crashed_lvl = 3 - contain.getStage();
        tick_sec = contain.getTick_sec();
        this.contain = contain;
    }

    public void load(FlimsyTileC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.FLIMSY_TILE;
        crashed_lvl = 3 - contain.getStage();
        tick_sec = contain.getTick_sec();
        this.contain = contain;
    }

    public void render(SpriteBatch batch) {
        if (crashed_lvl == 2) {
            batch.draw(
                    Res.clvl2,
                    wX - World.player.get_wX(),
                    wY - World.player.get_wY(),
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale()
            );
        }
        if (crashed_lvl == 1) {
            batch.draw(
                    Res.clvl1,
                    wX - World.player.get_wX(),
                    wY - World.player.get_wY(),
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale()
            );
        }
        if (crashed_lvl <= 0) {
            batch.draw(
                    Res.clvl0,
                    wX - World.player.get_wX(),
                    wY - World.player.get_wY(),
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale()
            );
        }
    }

    public void frame() {
        if ((x == World.player.getX()) && (y == World.player.getY())) {
            if (World.player.getHp() > 0) {
                if (crashed_lvl < 1) {
                    World.player.setHp(0);
                    World.player.setDamgeScr(0f, -1);
                }
            }
        }
        tick(tick_sec);
        if (isMove()) {
            move();
        }
    }

    @Override
    public void show_obj(SpriteBatch batch) {
        batch.draw(
                Res.damage,
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
        );
    }


    public void tick(float second) {
        if ((x == World.player.getX()) && (y == World.player.getY())) {
            if (World.player.getHp() > 0) {
                if (timer < 0 && crashed_lvl > 0) {
                    crashed_lvl--;
                    timer = second;
                }
            }
        }
        timer -= Gdx.graphics.getDeltaTime();
    }

    @Override
    public void setVal(String name, Value val) { //TODO доделать  функции скрипта
        switch (name) {
            case "X":
            case "x":
                x = val.asInt();
                break;
            case "Y":
            case "y":
                y = val.asInt();
                break;
            case "stage":
            case "Stage":
                crashed_lvl = 3 - val.asInt();
        }
    }

    @Override
    public Value getVal(String name) {
        switch (name) {
            case "X":
            case "x":
                return new NumberVal(x);
            case "Y":
            case "y":
                return new NumberVal(y);
            case "stage":
            case "Stage":
                return  new NumberVal(3-crashed_lvl);
        }
        return null;
    }

    @Override
    public void tag_activate(String func) {
        switch (func.split(">")[0]) {
            case "X":
                if (func.split(">")[1] == "++") {
                    x++;
                } else if (func.split(">")[1] == "--") {
                    x--;
                } else {
                    x = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "Y":
                if (func.split(">")[1] == "++") {
                    y++;
                } else if (func.split(">")[1] == "--") {
                    y--;
                } else {
                    y = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "Stage":
                if (func.split(">")[1] == "++") {
                    crashed_lvl--;
                } else if (func.split(">")[1] == "--") {
                    crashed_lvl++;
                } else {
                    crashed_lvl = 3 - Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "cordN":
                cordinateNormalize();
                break;
            case "del":
                del();
                break;
        }
    }


}
