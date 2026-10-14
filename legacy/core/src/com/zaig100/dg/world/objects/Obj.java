package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.contain.ObjC;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;

public abstract class Obj implements Runnable {

    public enum ObjType {
        OBJ,
        ARROW,
        BUTTON,
        CROSSBOW,
        CROSSBOW_AI,
        FLAMETHROWER,
        FLIMSY_TILE,
        HIDE_TRAP,
        ITEM,
        SPIKE,
        SPINNEY,
        STAIR,
        TELEPORT,
        DOOR,
        CHEST,
        TABLET,
        SHOP,
        ZONA,
        MOB,
        KAMIKAZE
    }

    public int x;
    public int y;
    public int wX;
    public int wY;
    int objID;
    public String tag;
    public ObjType type = ObjType.OBJ;
    double frameStepDist = 0;

    public ObjC contain;

    public Obj(int x, int y, String tag) {
        this.x = x;
        this.y = y;
        cordinateNormalize();
        this.tag = tag;
    }

    public void setObjID(int id) {
        objID = id;
    }

    public void cordinateNormalize() {
        wX = (x + 3) * 16 * (int) Configuration.getScale();
        wY = (y + 2) * 16 * (int) Configuration.getScale();
    }

    public void move(){
        move(1f);
    }

    public void move(float speed) {
        frameStepDist += Configuration.getScale() * Gdx.graphics.getDeltaTime() * 60 * speed;
        if(frameStepDist>=1) {
            if (wX != (x + 3) * 16 * (int) Configuration.getScale()) {
                if (wX > (x + 3) * 16 * (int) Configuration.getScale()) {
                    if ((wX - frameStepDist) > (x+3) * 16 * Configuration.getScale()) {
                        wX = wX - (int)frameStepDist;
                    }else{
                        wX = (int)((x + 3) * 16 *  Configuration.getScale());
                    }

                }
                if (wX < (x + 3) * 16 * (int) Configuration.getScale()) {
                    if ((wX + frameStepDist) < (x+3) * 16 * Configuration.getScale()) {
                        wX = wX + (int)frameStepDist;
                    }else {
                        wX = (int)((x + 3) * 16 *  Configuration.getScale());
                    }
                }
            }

            if (wY != (y + 2) * 16 * (int) Configuration.getScale()) {
                if (wY > (y + 2) * 16 * (int) Configuration.getScale()) {
                    if ((int)(wY - frameStepDist) > (y+2) * 16 * Configuration.getScale()) {
                        wY =  wY - (int)frameStepDist;
                    }else{
                        wY = (int)((y + 2) * 16 * Configuration.getScale());
                    }
                }
                if (wY < (y + 2) * 16 * (int) Configuration.getScale()) {
                    if ((wY + frameStepDist) < (y+2) * 16 * Configuration.getScale()) {
                        wY = wY + (int)frameStepDist;
                    }else{
                        wY = (int)((y + 2) * 16 * Configuration.getScale());
                    }
                }
            }
        }
        frameStepDist = 0;
    }

    public abstract void setVal(String name, Value val);

    public abstract Value getVal(String name);

    public boolean isMove() {
        return (wX != (x + 3) * 16 * (int) Configuration.getScale() || wY != (y + 2) * 16 * (int) Configuration.getScale());
    }

    public abstract void render(SpriteBatch batch);

    public abstract void frame();

    public abstract void show_obj(SpriteBatch batch);

    public abstract void tag_activate(String func);

    public ObjType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getTag() {
        return tag;
    }

    public int getObjID() {
        return objID;
    }

    @Override
    public void run() {
        frame();
    }

    public void del() {
        if (World.map.objectsU.contains(this)) {
            World.map.objectsU.remove(this);
        } else if (World.map.objectsO.contains(this)) {
            World.map.objectsO.remove(this);
        } else if (World.map.stair.contains(this)) {
            World.map.stair.remove(this);
        }
    }
}
