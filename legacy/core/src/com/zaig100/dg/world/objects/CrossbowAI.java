package com.zaig100.dg.world.objects;

import com.zaig100.dg.utils.contain.CrossbowAIC;
import com.zaig100.dg.utils.contain.CrossbowC;
import com.zaig100.dg.world.World;

public class CrossbowAI extends Crossbow{

    int distance;
    boolean diagonal;

    public CrossbowAI(CrossbowAIC contain) {
        super(contain);
        type = ObjType.CROSSBOW_AI;
        this.distance = contain.getDistanse();
        this.diagonal = contain.isDiagonal();
    }

    @Override
    public void frame() {
        if( Math.sqrt( (x - World.player.getX())*(x - World.player.getX()) + (y - World.player.getY())*(y - World.player.getY()))<=distance){
        super.frame();

        if(World.player.getX() == x){
            if(y>World.player.getY()){
                dy = 1;
                dx = 0;
                angle = 90;
            }
            if(y<World.player.getY()){
                dy = -1;
                dx = 0;
                angle = 270;
            }
            return;
        }

        if(World.player.getY() == y){
            if(x>World.player.getX()){
                dx = -1;
               dy = 0;
                angle = 0;
            }
            if(x<World.player.getX()){
                dx = 1;
                dy = 0;
                angle = 180;
            }
            return;
        }

        if(diagonal) {
            try {
                if (Math.abs((x - World.player.getX()) / (y - World.player.getY())) == 1) {

                    if (y > World.player.getY()) {
                        dy = 1;

                    } else if (y < World.player.getY()) {
                        dy = -1;
                    }

                    if (x > World.player.getX()) {
                        dx = -1;
                    } else if (x < World.player.getX()) {
                        dx = 1;
                    }

                    if (dy == -1) {
                        if (dx == 1) {
                            angle = 180 + 45;
                        } else if (dx == -1) {
                            angle = 270 + 45;
                        }
                    } else if (dy == 1) {
                        if (dx == 1) {
                            angle = 90 + 45;
                        } else if (dx == -1) {
                            angle = 45;
                        }
                    }

                }
            } catch (ArithmeticException e) {
                e.printStackTrace();
            }
        }
        }
    }
}
