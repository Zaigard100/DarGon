package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Joystick;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.ButtonС;
import com.zaig100.dg.utils.contain.StairC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

public class Stair extends Obj {

    public boolean flip_x, end;
    public String next_path;
    boolean isExit = false;

    public  Stair(StairC contain){
        super(contain.getX(),contain.getY(), contain.getTag());
        type = ObjType.STAIR;
        flip_x = contain.getFlipX();
        next_path = contain.getNext();
        end = contain.isEnd();
    }

    public void load(StairC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.STAIR;
        flip_x = contain.getFlipX();
        next_path = contain.getNext();
        end = contain.isEnd();
        this.contain = contain;
    }

    public void render(SpriteBatch batch) {
        if (Res.nextlv.isFlipX() != flip_x) {
            Res.nextlv.flip(flip_x, false);
        }
        batch.draw(
                Res.nextlv,
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
        );
    }

    @Override
    public void frame() {
        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) || (Joystick.isUse())) {
            if ((x == World.player.getX()) && (y == World.player.getY())) {
                World.player.setIsSheld(false);
                isExit = true;
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
            case "Next": case "next":
                next_path = val.asString();
                break;
            case "end": case "End":
                end = val.asInt() == 1;
                break;
            case "flipx": case "flipX": case "FlipX":
                flip_x = val.asInt() == 1;
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
            case "Next": case "next":
                return  new StringVal(next_path);
            case "end": case "End":
                return NumberVal.fromBoolean(end);
            case "flipx": case "flipX": case "FlipX":
                return NumberVal.fromBoolean(flip_x);
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
            case "End":
                if (func.split(">")[1] == "++" || func.split(">")[1] == "--") {
                    end = !end;
                } else {
                    end = Boolean.parseBoolean((func.split(">")[1]));
                }
                break;
            case "FlipX":
                if (func.split(">")[1] == "++" || func.split(">")[1] == "--") {
                    flip_x = !flip_x;
                } else {
                    flip_x = Boolean.parseBoolean((func.split(">")[1]));
                }
                break;
            case "Next":
                next_path = func.split(">")[1];
                break;
            case "cordN":
                cordinateNormalize();
                break;
            case "del":
                del();
                break;
        }
    }
    public String getNext_path() {
        return next_path;
    }

    public boolean isExit() {
        return isExit;
    }

    public boolean isEnd() {
        return end;
    }

}
