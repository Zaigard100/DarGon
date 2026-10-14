package com.zaig100.dg.utils.contain;

public class TeleportC extends ObjC {
    int tx, ty;

    public TeleportC(int x, int y, int tx, int ty, String tag) {
        super(x, y, tag);
        this.tx = tx;
        this.ty = ty;
    }

    public int getTx() {
        return tx;
    }

    public int getTy() {
        return ty;
    }

    @Override
    public String toString() {
        return "TeleportC{" +
                "tx=" + tx +
                ", ty=" + ty +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
