package com.zaig100.dg.utils.contain;

public class CrossbowAIC extends CrossbowC{

    int distanse;
    boolean diagonal;

    public CrossbowAIC(int x, int y, int dx, int dy, int angle, String tag, int distance,boolean diagonal) {
        super(x, y, dx, dy, angle, tag);
        this.distanse = distance;
        this.diagonal = diagonal;
    }

    public int getDistanse() {
        return distanse;
    }

    public boolean isDiagonal() {
        return diagonal;
    }

    @Override
    public String toString() {
        return "CrossbowAIC{" +
                "distanse=" + distanse +
                ", diagonal=" + diagonal +
                ", dx=" + dx +
                ", dy=" + dy +
                ", angle=" + angle +
                ", tick_sec=" + tick_sec +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
