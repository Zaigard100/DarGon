package com.zaig100.dg.utils.ai.way;

public abstract class Coordinate {

    int x,y;

    public Coordinate(int x, int y){
        this.x = x;
        this.y = y;
    }

    public abstract void init();

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
