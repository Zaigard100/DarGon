package com.zaig100.dg.utils.ai.way;

import com.zaig100.dg.world.World;

public class StartWay extends Coordinate {

    int iter;
    int endX, endY;
    String wayStory;
    public MainWay main;

    public StartWay(int x, int y, int endX, int endY, int iter,MainWay main) {
        super(x, y);
        this.iter =iter;
        this.endX = endX;
        this.endY = endY;
        this.main = main;
    }

    @Override
    public void init() {

        for(int y = -1; y<=1; y++){
            for(int x = -1; x<=1; x++){
                if(x==0&&y==0) continue;
                if (World.map.getTileId(this.x + x, this.y + y)==11) {
                    //new Way(this.x + x,this.y + y,iter-1,this); // перебор окружащих путей
                    new Way(this.x + x, this.y + y, iter - 1, this,iter + ":x=" + this.x  + "y=" + this.y );// перебор путей с сохранением истории
                }
            }
        }
    }


    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public String getWayStory() {
        return wayStory;
    }

    public void setWayStory(String wayStory) {
        this.wayStory = wayStory;
    }
}
