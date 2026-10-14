package com.zaig100.dg.world.objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

public class Arrow extends Obj {

    public int dx;
    public int dy;
    public int angle;
    float timer = 0;
    int far;

    public Arrow(int x, int y, int dx, int dy, int angle, String tag) {
        super(x, y, tag);
        type = ObjType.ARROW;
        this.dx = dx;
        this.dy = dy;
        this.angle = angle;
        if (World.map.mapHeight > World.map.mapWidht) {
            far = -World.map.mapHeight * 2;
        } else {
            far = -World.map.mapWidht * 2;
        }
    }

    public Arrow(int x, int y, int dx, int dy, int angle, String tag, int far) {
        super(x, y, tag);
        type = ObjType.ARROW;
        this.dx = dx;
        this.dy = dy;
        this.angle = angle;
        this.far = -far;
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
            case "Far": case "far":
                far = val.asInt();
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
            case "Far": case "far":
                return new NumberVal(far);
        }
        return null;
    }

    @Override
    public void render(SpriteBatch batch) {

        batch.draw(
                Res.arrow,
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
        //System.out.println("X "+x+" Y " +y);
        if ((x == World.player.getX()) && (y == World.player.getY())) {
            if (World.player.getHp() > 0) {
                if (World.player.isSheld()) {
                    World.player.setHp(World.player.getHp() - 1);
                } else {
                    World.player.setHp(World.player.getHp() - 2);
                }
                World.player.setDamgeScr(0f, 3);
            }

        }
        if(last() || ((x == World.player.getX()) && (y == World.player.getY()))) del();


        if (isMove()) {
            move();
        } else {
            x += dx;
            y -= dy;
            timer = 0;
            far++;
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

    @Override
    public void tag_activate(String func) {

        switch (func.split(">")[0]) {
            case "x":
                if (func.split(">")[1] == "++") {
                    x++;
                } else if (func.split(">")[1] == "--") {
                    x--;
                } else {
                    x = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "y":
                if (func.split(">")[1] == "++") {
                    y++;
                } else if (func.split(">")[1] == "--") {
                    y--;
                } else {
                    y = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "dx":
                if (func.split(">")[1] == "++") {
                    dx++;
                } else if (func.split(">")[1] == "--") {
                    dx--;
                } else {
                    dx = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "dy":
                if (func.split(">")[1] == "++") {
                    dy++;
                } else if (func.split(">")[1] == "--") {
                    dy--;
                } else {
                    dy = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "angle":
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


    public boolean last() {
        return far == 0;
    }

}
