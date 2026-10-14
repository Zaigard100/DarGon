package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.ButtonС;
import com.zaig100.dg.utils.contain.CrossbowC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

import java.util.ArrayList;

public class Crossbow extends Obj {

    public int dx, dy, angle;
    public float tick_sec;
    float timer = 0;


    public Crossbow(CrossbowC contain) {
        super(contain.getX(), contain.getY(), contain.getTag());
        type = ObjType.CROSSBOW;
        dx =contain.getDx();
        dy =-contain.getDy();
        angle = contain.getAngle();
        tick_sec = contain.getTick_sec();
        World.map.objectsO.add(new Arrow(x, y, dx, dy, angle, tag + "Arr"));
        this.contain = contain;
    }

    public void load(CrossbowC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.CROSSBOW;
        dx =contain.getDx();
        dy =-contain.getDy();
        angle = contain.getAngle();
        tick_sec = contain.getTick_sec();
        World.map.objectsO.add(new Arrow(x, y, dx, dy, angle, tag + "Arr"));
        this.contain = contain;
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
            case "dx": case "Dx":case "DX":case "dX":
                dx = val.asInt();
                break;
            case "dy": case "Dy":case "DY":case "dY":
                dy = val.asInt();
                break;
            case "Angle": case "angle":
                angle = val.asInt();
                break;
            case "Tick": case "tick":
                tick_sec = (float) val.asNum();
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
            case "dx": case "Dx":case "DX":case "dX":
                return new NumberVal(dx);
            case "dy": case "Dy":case "DY":case "dY":
                return new NumberVal(dy);
            case "Angle": case "angle":
                return new NumberVal(angle);
            case "Tick": case "tick":
                return new NumberVal(tick_sec);
        }
        return null;
    }

    @Override
    public void render(SpriteBatch batch) {

        batch.draw(
                Res.crossbow(false),
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                8 * Configuration.getScale(),
                8 * Configuration.getScale(),
                16 * Configuration.getScale(),
                16 * Configuration.getScale(),
                1,
                1,
                angle
        );

    }

    @Override
    public void frame() {
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
        timer += Gdx.graphics.getDeltaTime();
        if (timer >= second) {
            World.map.objectsO.add(new Arrow(x, y, dx, dy, angle, tag + "Arr"));
            timer = 0;
        }
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
            case "DX":
                if (func.split(">")[1] == "++") {
                    dx++;
                } else if (func.split(">")[1] == "--") {
                    dx--;
                } else {
                    dx = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "DY":
                if (func.split(">")[1] == "++") {
                    dy++;
                } else if (func.split(">")[1] == "--") {
                    dy--;
                } else {
                    dy = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "Angle":
                if (func.split(">")[1] == "++") {
                    angle++;
                } else if (func.split(">")[1] == "--") {
                    angle--;
                } else {
                    angle = Integer.parseInt((func.split(">")[1]));
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
