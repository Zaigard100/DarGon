package com.zaig100.dg.world.objects.mobs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.objects.Obj;

public class Mob extends Obj {

    public enum Stage{

        PACIFIC(0),
        FIND(1),
        AGGRESSIVE(2);

        int state;

        Stage(int state) {
            this.state = state;
        }

        public int getState() {
            return state;
        }
    }

    int endX,endY;
    Stage state;

    public Mob(int x, int y, String tag) {
        super(x, y, tag);
    }

    @Override
    public void setVal(String name, Value val) {

    }

    @Override
    public Value getVal(String name) {
        return NumberVal.ZERO;
    }

    @Override
    public void render(SpriteBatch batch) {

    }

    @Override
    public void frame() {

    }

    @Override
    public void show_obj(SpriteBatch batch) {

    }

    @Override
    public void tag_activate(String func) {

    }
}
