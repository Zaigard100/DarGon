package com.zaig100.dg.utils.contain;

public class HideTrapC extends ObjC {
    boolean active;

    public HideTrapC(int x, int y, boolean active, String tag) {
        super(x, y, tag);
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "HideTrapC{" +
                "active=" + active +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
