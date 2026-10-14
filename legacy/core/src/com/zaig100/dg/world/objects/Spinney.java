package com.zaig100.dg.world.objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.SpinneyC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

public class Spinney extends Obj {
    public int wight, height;
    private int i, j;


    public Spinney(SpinneyC contain){
        super(contain.getX(),contain.getY(), contain.getTag());
        type = ObjType.SPINNEY;
        wight = Math.abs(contain.getWight());
        height = Math.abs(contain.getHeight());
        this.contain = contain;
    }

    public void load(SpinneyC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.SPINNEY;
        wight = Math.abs(contain.getWight());
        height = Math.abs(contain.getHeight());
        this.contain = contain;
    }

    public void render(SpriteBatch batch) {
        for (i = 0; i < height; i++) {
            for (j = 0; j < wight; j++) {
                batch.draw(
                        Res.spinney,
                        (wX + j * 16 * Configuration.getScale()) - World.player.get_wX(),
                        (wY + i * 16 * Configuration.getScale()) - World.player.get_wY(),
                        16 * Configuration.getScale(),
                        16 * Configuration.getScale()
                );
            }
        }
    }

    @Override
    public void frame() {
        if (isMove()) {
            move();
        }
    }

    @Override
    public void show_obj(SpriteBatch batch) {
        batch.draw(
                Res.blue_obj,
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                wight * 16 * Configuration.getScale(),
                height * 16 * Configuration.getScale()
        );
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
}
