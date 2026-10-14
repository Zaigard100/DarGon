package com.zaig100.dg.utils.contain;

import com.zaig100.dg.world.objects.Zona;

public class ZonaC extends ObjC {
    int wight, height;
    Zona.ZonaType type;
    float tick;

    public ZonaC(int x, int y, int wight, int height, float tick, String type, String tag) {
        super(x, y, tag);
        this.wight = wight;
        this.height = height;
        this.tick = tick;
        switch (type) {
            case "hp+":
                this.type = Zona.ZonaType.HP_PIUS;
                break;
            case "hp-":
                this.type = Zona.ZonaType.HP_MINUS;
                break;
        }
    }

    public int getWight() {
        return wight;
    }

    public int getHeight() {
        return height;
    }

    public Zona.ZonaType getType() {
        return type;
    }

    public float getTick() {
        return tick;
    }

    @Override
    public String toString() {
        return "ZonaC{" +
                "wight=" + wight +
                ", height=" + height +
                ", type=" + type +
                ", tick=" + tick +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
