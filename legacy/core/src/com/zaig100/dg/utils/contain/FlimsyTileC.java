package com.zaig100.dg.utils.contain;

public class FlimsyTileC extends ObjC {
    int stage;
    float tick_sec = 1f;

    public FlimsyTileC(int x, int y, int stage, String tag) {
        super(x, y, tag);
        this.stage = stage;
    }

    public int getStage() {
        return stage;
    }

    public float getTick_sec() {
        return tick_sec;
    }

    public void setTick_sec(double tick_sec) {
        this.tick_sec = (float) tick_sec;
    }

    @Override
    public String toString() {
        return "FlimsyTileC{" +
                "stage=" + stage +
                ", tick_sec=" + tick_sec +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
