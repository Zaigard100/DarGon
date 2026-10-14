package com.zaig100.dg.utils.contain;

public class SpikeC extends ObjC {
    boolean active;
    float tick_sec = 1.5f;

    public SpikeC(int x, int y, boolean active, String tag) {
        super(x, y, tag);
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public float getTick_sec() {
        return tick_sec;
    }

    public void setTick_sec(double tick_sec) {
        this.tick_sec = (float) tick_sec;
    }

    @Override
    public String toString() {
        return "SpikeC{" +
                "active=" + active +
                ", tick_sec=" + tick_sec +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
