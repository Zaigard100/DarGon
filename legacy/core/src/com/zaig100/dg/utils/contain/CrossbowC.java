package com.zaig100.dg.utils.contain;

public class CrossbowC extends ObjC {
    int dx, dy, angle;
    float tick_sec = 4f;

    public CrossbowC(int x, int y, int dx, int dy, int angle, String tag) {
        super(x, y, tag);
        this.dx = dx;
        this.dy = dy;
        this.angle = angle;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getAngle() {
        return angle;
    }

    public float getTick_sec() {
        return tick_sec;
    }

    public void setTick_sec(double tick_sec) {
        this.tick_sec = (float) tick_sec;
    }

    @Override
    public String toString() {
        return "CrossbowC{" +
                "dx=" + dx +
                ", dy=" + dy +
                ", angle=" + angle +
                ", tick_sec=" + tick_sec +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
