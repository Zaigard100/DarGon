package com.zaig100.dg.utils.contain;

public class StairC extends ObjC {
    String next;
    boolean flipX, isEnd = false;

    public StairC(int x, int y, boolean flipX, String next, boolean end, String tag) {
        super(x, y, tag);
        this.flipX = flipX;
        this.next = next;
        isEnd = end;
        if (next == null || next == "END") {
            isEnd = true;
        }
    }

    public boolean getFlipX() {
        return flipX;
    }

    public String getNext() {
        return next;
    }

    public boolean isEnd() {
        return isEnd;
    }

    @Override
    public String toString() {
        return "StairC{" +
                "next='" + next + '\'' +
                ", flipX=" + flipX +
                ", isEnd=" + isEnd +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
