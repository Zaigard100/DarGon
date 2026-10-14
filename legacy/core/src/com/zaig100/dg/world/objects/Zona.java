package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.ButtonС;
import com.zaig100.dg.utils.contain.ZonaC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

public class Zona extends Obj {

    public enum ZonaType {

        HP_PIUS("hp+"),
        HP_MINUS("hp-");

        String name;

        ZonaType(String s) {
            name = s;
        }

        public String getName() {
            return name;
        }
    }

    int wight, height;
    ZonaType zonaType;
    float tick, timer;
    int i, j;


    public Zona(ZonaC contain){
        super(contain.getX(),contain.getY(), contain.getTag());
        type = ObjType.ZONA;
        zonaType = contain.getType();
        wight = contain.getWight();
        height = contain.getHeight();
        tick = contain.getTick();
        this.contain = contain;
    }

    public void load(ZonaC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.ZONA;
        zonaType = contain.getType();
        wight = contain.getWight();
        height = contain.getHeight();
        this.contain = contain;
    }

    @Override
    public void render(SpriteBatch batch) {

    }

    @Override
    public void frame() {
        tick(tick);
    }

    @Override
    public void show_obj(SpriteBatch batch) {
        switch (zonaType) {
            case HP_PIUS:
                for (i = 0; i < height; i++) {
                    for (j = 0; j < wight; j++) {
                        batch.draw(
                                Res.green_sm_obj,
                                wX + j * 16 * Configuration.getScale() - World.player.get_wX(),
                                wY + i * 16 * Configuration.getScale() - World.player.get_wY(),
                                16 * Configuration.getScale(),
                                16 * Configuration.getScale()
                        );
                    }
                }
                break;
            case HP_MINUS:
                for (i = 0; i < height; i++) {
                    for (j = 0; j < wight; j++) {
                        batch.draw(
                                Res.red_sm_obj,
                                wX + j * 16 * Configuration.getScale() - World.player.get_wX(),
                                wY + i * 16 * Configuration.getScale() - World.player.get_wY(),
                                16 * Configuration.getScale(),
                                16 * Configuration.getScale()
                        );
                    }
                }
                break;
        }
    }

    @Override
    public void setVal(String name, Value val) {
        switch (name) {
            case "X":
            case "x":
                x = val.asInt();
                break;
            case "Y":
            case "y":
                y = val.asInt();
                break;
            case "wight":
            case "Wight":
                wight = val.asInt();
                break;
            case "Height":
            case "height":
                height = val.asInt();
                break;
            case "type": case "Type":
                switch (val.asString()) {
                    case "hp+":
                        zonaType = ZonaType.HP_PIUS;
                        break;
                    case "hp-":
                        zonaType = ZonaType.HP_MINUS;
                        break;
                }
                break;
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
            case "wight":
            case "Wight":
                return new NumberVal(wight);
            case "Height":
            case "height":
                return new NumberVal(height);
            case "type": case "Type":
                return new StringVal(type.name());
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
            case "Wight":
                if (func.split(">")[1] == "++") {
                    wight++;
                } else if (func.split(">")[1] == "--") {
                    wight--;
                } else {
                    wight = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "Height":
                if (func.split(">")[1] == "++") {
                    height++;
                } else if (func.split(">")[1] == "--") {
                    height--;
                } else {
                    height = Integer.parseInt((func.split(">")[1]));
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

    void tick(float second) {
        timer += Gdx.graphics.getDeltaTime();
        if (timer >= second) {
            timer = 0;
            if ((World.player.getX() >= x) && (World.player.getX() <= x + wight) && (World.player.getY() >= y) && (World.player.getY() <= y + height)) {

                switch (zonaType) {
                    case HP_PIUS:
                        if(World.player.getX()<4) {
                            World.player.hp += 1;
                        }
                        break;
                    case HP_MINUS:
                        World.player.setDamgeScr(0f,1);
                        World.player.hp -= 1;
                        break;
                }

            } else {
                timer = 0;
            }
        }
    }

    public float getTick() {
        return tick;
    }

    public void setTick(float tick) {
        this.tick = tick;
    }
}
