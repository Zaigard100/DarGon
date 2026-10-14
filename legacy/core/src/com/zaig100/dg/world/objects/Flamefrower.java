package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.ButtonС;
import com.zaig100.dg.utils.contain.FlamefrowerC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

public class Flamefrower extends Obj {

    public int max, angle, stage;
    int flame_stage, anim_stage = 0;
    float timer = 0, fire_timer = 0;
    int dx, dy;
    int timst = 0;
    public float tick_sec;


    public  Flamefrower(FlamefrowerC contain) {
        super(contain.getX(), contain.getY(), contain.getTag());
        type = ObjType.FLAMETHROWER;
        max = contain.getMax();
        angle = contain.getRot();
        stage = contain.getStage();
        tick_sec = contain.getTick_sec();
        if (angle == 0) {
            dx = 1;
        }
        if (angle == 1) {
            dy = 1;
        }
        if (angle == 2) {
            dx = -1;
        }
        if (angle == 3) {
            dy = -1;
        }
    }

    public void load(FlamefrowerC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.FLAMETHROWER;
        max = contain.getMax();
        angle = contain.getRot();
        stage = contain.getStage();
        tick_sec = contain.getTick_sec();
        if (angle == 0) {
            dx = 1;
        }
        if (angle == 1) {
            dy = 1;
        }
        if (angle == 2) {
            dx = -1;
        }
        if (angle == 3) {
            dy = -1;
        }
        this.contain = contain;
    }

    public void render(SpriteBatch batch) {
        batch.draw(
                Res.flamethrowfer,
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                8 * Configuration.getScale(),
                8 * Configuration.getScale(),
                16 * Configuration.getScale(),
                16 * Configuration.getScale(),
                1,
                1,
                90 * angle
        );

        flame_stage = 0;
        while (flame_stage < stage) {


            batch.draw(
                    Res.fire(anim_stage),
                    ((x + dx * (flame_stage + 1)) + 3) * 16 * Configuration.getScale() - World.player.get_wX(),
                    ((y + dy * (flame_stage + 1)) + 2) * 16 * Configuration.getScale() - World.player.get_wY(),
                    8 * Configuration.getScale(),
                    8 * Configuration.getScale(),
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale(),
                    1,
                    1,
                    90 * (angle - 1)
            );
            frame();
            flame_stage++;
        }
    }

    public void frame() {
        timst = 0;
        while (timst < flame_stage) {
            if ((World.player.getX() == x + ((timst + 1) * dx)) && (World.player.getY() == y + ((timst + 1) * dy))) {
                if (World.player.getHp() > 0) {
                    World.player.setHp(0);
                    World.player.setDamgeScr(0f, 2);
                }
            }
            timst++;
        }
        tick(tick_sec);
        tick1(0.2f);
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

            stage++;
            timer = 0;
            if (stage > max) {
                stage = 0;
            }
        }
    }

    public void tick1(float second) {
        fire_timer += Gdx.graphics.getDeltaTime();
        if (fire_timer >= second) {

            anim_stage++;
            fire_timer = 0;
            if (anim_stage > 1) {
                anim_stage = 0;
            }
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
            case "max": case "Max":
                max = val.asInt();
            case "angle": case "Angle":
                angle = val.asInt();
            case "Stage": case "stage":
                stage = val.asInt();
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
            case "max": case "Max":
                return new NumberVal(max);
            case "angle": case "Angle":
                return new NumberVal(angle);
            case "Stage": case "stage":
                return  new NumberVal(stage);
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
            case "Angle":
                if (func.split(">")[1] == "++") {
                    angle++;
                } else if (func.split(">")[1] == "--") {
                    angle--;
                } else {
                    angle = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "Stage":
                if (func.split(">")[1] == "++") {
                    stage++;
                } else if (func.split(">")[1] == "--") {
                    stage--;
                } else {
                    stage = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "cordN":
                cordinateNormalize();
                break;
            case "del":
                del();
                break;
        }
        if (angle == 0) {
            dx = 1;
        }
        if (angle == 1) {
            dy = 1;
        }
        if (angle == 2) {
            dx = -1;
        }
        if (angle == 3) {
            dy = -1;
        }
    }


}
