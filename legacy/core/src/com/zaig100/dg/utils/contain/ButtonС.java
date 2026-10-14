package com.zaig100.dg.utils.contain;

import java.util.Arrays;

public class ButtonС extends ObjC {
    String[] func;

    public ButtonС(int x, int y, String[] func, String tag) {
        super(x, y, tag);
        this.func = func;
    }

    public String[] getFunc() {
        return func;
    }

    @Override
    public String toString() {
        return "ButtonС{" +
                "func=" + Arrays.toString(func) +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
