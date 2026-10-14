package com.zaig100.dg.utils.contain;

public class SpinneyC extends ObjC {
    int wight, height;

    public SpinneyC(int x, int y, int wight, int height, String tag) {
        super(x, y, tag);
        this.wight = wight;
        this.height = height;
    }

    public int getWight() {
        return wight;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "SpinneyC{" +
                "wight=" + wight +
                ", height=" + height +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
