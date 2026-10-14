package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Joystick;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.SpinneyC;
import com.zaig100.dg.utils.contain.TeleportC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

public class Teleport extends Obj {
    public int tx, ty;
    public boolean hide;


    public Teleport(TeleportC contain){
        super(contain.getX(),contain.getY(), contain.getTag());
        type = ObjType.TELEPORT;
        tx = contain.getTx();
        ty = contain.getTy();
        hide = true;
        this.contain = contain;
    }

    public void load(TeleportC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.TELEPORT;
        tx = contain.getTx();
        ty = contain.getTy();
        this.contain = contain;
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.draw(
                Res.teleport,
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
        );
    }

    @Override
    public void frame() {
        if ((World.player.getX() == x) && (World.player.getY() == y)) {
            if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) || (Joystick.isUse())) {
                World.player.teleport(tx, ty);
            }
        }
        if (isMove()) {
            move();
        }
    }

    @Override
    public void show_obj(SpriteBatch batch) {
        batch.draw(
                Res.green_obj,
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
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
            case "TX":
            case "tX":
            case "tx":
                tx = val.asInt();
                break;
            case "TY":
            case "tY":
            case "ty":
                ty = val.asInt();
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
            case "TX":
            case "tX":
            case "tx":
                return new NumberVal(tx);
            case "TY":
            case "tY":
            case "ty":
                return new NumberVal(ty);
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
            case "TX":
                if (func.split(">")[1] == "++") {
                    tx++;
                } else if (func.split(">")[1] == "--") {
                    tx--;
                } else {
                    tx = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "TY":
                if (func.split(">")[1] == "++") {
                    ty++;
                } else if (func.split(">")[1] == "--") {
                    ty--;
                } else {
                    ty = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "Hide":
                if (func.split(">")[1] == "++" || func.split(">")[1] == "--") {
                    hide = !hide;
                } else {
                    hide = Boolean.parseBoolean((func.split(">")[1]));
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
