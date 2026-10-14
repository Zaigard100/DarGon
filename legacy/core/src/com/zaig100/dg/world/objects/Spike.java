package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.ButtonС;
import com.zaig100.dg.utils.contain.SpikeC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

public class Spike extends Obj {
    public boolean active, trigered;
    public float tick_sec;
    private float timer, timer1;


    public Spike(SpikeC contain){
        super(contain.getX(),contain.getY(), contain.getTag());
        type = ObjType.SPIKE;
        tick_sec =contain.getTick_sec();
        active = contain.isActive();
        this.contain = contain;
    }

    public void load(SpikeC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.SPIKE;
        tick_sec =contain.getTick_sec();
        active = contain.isActive();
        this.contain = contain;
    }

    public void render(SpriteBatch batch) {
        if (active) {
            batch.draw(Res.spike_1,
                    wX - World.player.get_wX() - Configuration.getScale(),
                    wY - World.player.get_wY() + Configuration.getScale(),
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale()
            );
        } else {
            batch.draw(Res.spike_0,
                    wX - World.player.get_wX() - Configuration.getScale(),
                    wY - World.player.get_wY() + Configuration.getScale(),
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale()
            );
        }
    }

    public void frame() {
        if ((x == World.player.getX()) && (y == World.player.getY())) {
            if (World.player.getHp() > 0) {
                trigered = true;
                if (active) {
                    World.player.setHp(0);
                    World.player.setDamgeScr(0f, 4);
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
        if (trigered) {
            if (timer > second && !active) {
                active = true;
                timer = 0;
                timer1 = 0;
                trigered = false;
            } else {
                timer += Gdx.graphics.getDeltaTime();
            }

        } else {
            if (timer1 > 2 * second && active) {
                active = false;
                timer1 = 0;
                timer = 0;
            } else {
                timer1 += Gdx.graphics.getDeltaTime();
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
            case "Active": case "active":
                active = val.asInt() == 1;
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
            case "Active": case "active":
                return NumberVal.fromBoolean(active);
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
            case "Active":
                if (func.split(">")[1] == "++" || func.split(">")[1] == "--") {
                    active = !active;
                } else {
                    active = Boolean.parseBoolean(func.split(">")[1]);
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
