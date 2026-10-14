package com.zaig100.dg.utils.contain.mobC;

import com.zaig100.dg.utils.contain.ObjC;

public class KamikadzeC extends ObjC {
    int iter,findRadius;
    float speed;

    public KamikadzeC(int x, int y, String tag, int iter, int findRadius,float speed) {
        super(x, y, tag);
        this.iter = iter;
        this.findRadius = findRadius;
        this.speed = speed;
    }

    public int getIter() {
        return iter;
    }

    public int getFindRadius() {
        return findRadius;
    }

    public float getSpeed() {
        return speed;
    }

    @Override
    public String toString() {
        return "KamikadzeC{" +
                "iter=" + iter +
                ", findRadius=" + findRadius +
                ", speed=" + speed +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
