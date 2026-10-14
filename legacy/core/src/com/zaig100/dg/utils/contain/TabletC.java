package com.zaig100.dg.utils.contain;

import java.util.Arrays;

public class TabletC extends ObjC {
    String[] text;

    public TabletC(int x, int y, String[] text, String tag) {
        super(x, y, tag);
        this.text = text;
    }

    public String[] getText() {
        return text;
    }

    @Override
    public String toString() {
        return "TabletC{" +
                "text=" + Arrays.toString(text) +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
