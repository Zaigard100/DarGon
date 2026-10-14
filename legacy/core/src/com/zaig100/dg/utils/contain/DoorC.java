package com.zaig100.dg.utils.contain;

public class DoorC extends ObjC {

    String keyTag;
    boolean isOpen;
    int faicing;


    public DoorC(int x, int y, String keyTag, boolean isOpen, int faicing, String tag) {
        super(x, y, tag);
        this.keyTag = keyTag;
        this.isOpen = isOpen;
        this.faicing = faicing;
    }

    public String getKeyTag() {
        return keyTag;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public int getFaicing() {
        return faicing;
    }

    @Override
    public String toString() {
        return "DoorC{" +
                "keyTag='" + keyTag + '\'' +
                ", isDoorOpen=" + isOpen +
                ", faicing=" + faicing +
                ", x=" + x +
                ", y=" + y +
                ", tag='" + tag + '\'' +
                '}';
    }
}
