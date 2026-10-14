package com.zaig100.dargon.level;

/** Клетка сетки. Начало координат — левый верхний угол, Y растёт вниз (как в ASCII-карте). */
public record GridPoint(int x, int y) {

    public GridPoint offset(int dx, int dy) {
        return new GridPoint(x + dx, y + dy);
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + "]";
    }
}
