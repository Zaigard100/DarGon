package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Joystick;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.ButtonС;
import com.zaig100.dg.utils.contain.TeleportC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.Map;
import com.zaig100.dg.world.World;

public class Button extends Obj {
    String[] func;
    Map M;
    boolean active = true;


    public Button(ButtonС contain, Map m){
        super(contain.getX(),contain.getY(), contain.getTag());
        type = ObjType.BUTTON;
        func = contain.getFunc();
        M = m;//map
        this.contain = contain;
    }

    public void load(ButtonС contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.BUTTON;
        func = contain.getFunc();
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
        }
        return null;
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.draw(Res.button, wX - World.player.get_wX(), wY - World.player.get_wY(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
    }

    @Override
    public void frame() {


        //Joystick.frame((int) ((Gdx.graphics.getWidth() - 16 * 7 * Configuration.getScale()) / 2), (int) ((Gdx.graphics.getHeight() - 16 * 5 * Configuration.getScale()) / 2));
        if ((World.player.getX() == x) && (World.player.getY() == y) && active) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || (Joystick.isUse() && Gdx.input.justTouched())) {
                for (int i = 0; i < func.length; i++) {
                    M.doFunc(func[i]);
                }
                active = false;
            }
        }

        if ((World.player.getX() != x) && (World.player.getY() != y)) {
            active = true;
        }

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
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
        );
    }

    public Button setMap(Map M) {
        this.M = M;
        return this;
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
            case "cordN":
                cordinateNormalize();
                break;
            case "del":
                del();
                break;
        }
    }
}