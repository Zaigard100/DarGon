
package com.zaig100.dg.world.objects.mobs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.ai.way.Coordinate;
import com.zaig100.dg.utils.ai.way.MainWay;
import com.zaig100.dg.utils.ai.way.NullWay;
import com.zaig100.dg.utils.ai.way.StartWay;
import com.zaig100.dg.utils.contain.TeleportC;
import com.zaig100.dg.utils.contain.mobC.KamikadzeC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.Player;
import com.zaig100.dg.world.World;
import com.zaig100.dg.world.objects.Arrow;

import java.util.Objects;

public class Kamikaze extends Mob{

    int findRadius,iter;
    float speed;
    boolean active = false;

    KamikazeThreard threard;
    public boolean live = false;
    public Kamikaze(KamikadzeC contain) {
        super(contain.getX(),contain.getY(), contain.getTag());
        type = ObjType.KAMIKAZE;
        findRadius = contain.getFindRadius();
        iter = contain.getIter();
        speed = contain.getSpeed();
        threard = new KamikazeThreard(iter);
        threard.updata(x, y, x, y);
        threard.start();
        this.contain = contain;
    }

    public void load(KamikadzeC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.KAMIKAZE;
        findRadius = contain.getFindRadius();
        iter = contain.getIter();
        speed = contain.getSpeed();
        this.contain = contain;
    }
    @Override
    public void render(SpriteBatch batch) {
        batch.draw(
                Res.spikedhero,
                wX - World.player.wX,
                wY - World.player.wY,
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
        );
    }

    @Override
    public void frame() {
        frame_thr();
        threard.updata(x, y, endX, endY);
    }

    void frame_thr() {
        if(distance(World.player.getX(),World.player.getY())<=findRadius){
            if(!(World.player.getX()==endX&&World.player.getY()==endY)) {
                endX = World.player.getX();
                endY = World.player.getY();
                active = true;
            }
        }

        if(active){

            if(World.player.getX()==x&&World.player.getY()==y){
                if(!isMove()) {
                    active = false;
                    World.player.setDamgeScr(0f,1);
                    World.player.setHp(World.player.hp-2);
                    threard.end();
                    del();
                }
            }
            if(!isMove()) {
                x = threard.getX();
                y = threard.getY();
            }

            move(speed);

        }

    }



    @Override
    public void run() {
        frame();
    }

    @Override
    public void setVal(String name, Value val) {
        super.setVal(name, val);
        switch (name){
            case "X": case "x":
                x = val.asInt();
                break;
            case "Y": case "y":
                y = val.asInt();
                break;
            case "Radius": case "radius":
                findRadius = val.asInt();
                break;
            case "Iter": case "iter":
                iter = val.asInt();
                break;
        }
    }

    @Override
    public Value getVal(String name) {

        switch (name){
            case "X": case "x":
                return new NumberVal(x);
            case "Y": case "y":
                return new NumberVal(y);
            case "Radius": case "radius":
                return new NumberVal(findRadius);
            case "Iter": case "iter":
                return new NumberVal(iter);
        }
        return super.getVal(name);
    }

    @Override
    public void tag_activate(String func) {
        super.tag_activate(func);
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
            case "Radius":
                if (func.split(">")[1] == "++") {
                    findRadius++;
                } else if (func.split(">")[1] == "--") {
                    findRadius--;
                } else {
                    findRadius = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "Iter":
                if (func.split(">")[1] == "++") {
                    iter++;
                } else if (func.split(">")[1] == "--") {
                    iter--;
                } else {
                    iter = Integer.parseInt((func.split(">")[1]));
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

    private double distance(int oX, int oY){
        return Math.sqrt(((oX-getX())*(oX-getX()))+((oY-getY())*(oY-getY())));
    }

    @Override
    public void del() {
        threard.end();
        super.del();

    }
}
class KamikazeThreard extends Thread {
    int iter;
    Coordinate start;
    MainWay main;
    public KamikazeThreard(int iter){
        this.iter = iter;
    }
    int x,y,endX,endY;
    int wx,wy;
    boolean live = true;
    boolean isStep = true;

    public void end(){
        live = false;
    }
    @Override
    public void run() {
        while (live) {
            way();
        }
    }
    void updata(int x,int y,int endX,int endY){
        this.x = x;
        this.y = y;
        this.endX = endX;
        this.endY = endY;
    }

    void way(){
        if(isStep) {
            main = new MainWay(x, y, endX, endY, iter);
            main.init();
            start = main.shortWay();
            if (start.getX() != -1 && start.getY() != -1) {
                wx = start.getX();
                wy = start.getY();
            }
        }

    }

    int getX(){
        return wx;
    }

    int getY(){
        return wy;
    }
}
