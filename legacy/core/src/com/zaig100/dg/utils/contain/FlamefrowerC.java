package com.zaig100.dg.utils.contain;

public class FlamefrowerC extends ObjC {
    int stage, max, rot;
    float tick_sec = 2f;

    public FlamefrowerC(int x, int y, int stage, int max, int rot, String tag) {
        super(x, y, tag);
        this.stage = stage;
        this.max = max;
        this.rot = rot;
    }

    public int getStage() {
        return stage;
    }

    public int getMax() {
        return max;
    }

    public int getRot() {
        return rot;
    }

    public float getTick_sec() {
        return tick_sec;
    }

    public void setTick_sec(double tick_sec) {
        this.tick_sec = (float) tick_sec;
    }

    @Override
    public String toString() {
        return "FlamefrowerC{" +
                "stage=" + stage +
                ", max=" + max +
                ", rot=" + rot +
                ", tick_sec=" + tick_sec +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
