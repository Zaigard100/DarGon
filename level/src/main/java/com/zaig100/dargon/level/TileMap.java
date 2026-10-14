package com.zaig100.dargon.level;

import java.util.Arrays;

/**
 * Прямоугольная карта клеток. Начало — левый верхний угол, Y растёт вниз. Всё, что за границей,
 * считается стеной.
 */
public final class TileMap {

    private final int width;
    private final int height;
    private final TileType[] tiles;

    public TileMap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Размер карты должен быть положительным: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.tiles = new TileType[width * height];
        Arrays.fill(tiles, TileType.WALL);
    }

    /** Карта из ASCII-строк с легендой по умолчанию ({@code .} — пол, {@code #} — стена). */
    public static TileMap parse(String... rows) {
        TileMap map = new TileMap(rows[0].length(), rows.length);
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                map.set(
                        x,
                        y,
                        rows[y].charAt(x) == TileType.FLOOR.symbol
                                ? TileType.FLOOR
                                : TileType.WALL);
            }
        }
        return map;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean inBounds(GridPoint p) {
        return inBounds(p.x(), p.y());
    }

    /** Тип клетки; за границей карты — {@link TileType#WALL}. */
    public TileType get(int x, int y) {
        return inBounds(x, y) ? tiles[y * width + x] : TileType.WALL;
    }

    public void set(int x, int y, TileType type) {
        if (!inBounds(x, y)) {
            throw new IndexOutOfBoundsException(
                    "Клетка [" + x + ", " + y + "] вне карты " + width + "x" + height);
        }
        tiles[y * width + x] = type;
    }

    public boolean isFloor(int x, int y) {
        return get(x, y) == TileType.FLOOR;
    }

    public boolean isFloor(GridPoint p) {
        return isFloor(p.x(), p.y());
    }

    public TileMap copy() {
        TileMap copy = new TileMap(width, height);
        System.arraycopy(tiles, 0, copy.tiles, 0, tiles.length);
        return copy;
    }

    /** Строки карты с символами легенды по умолчанию. */
    public String[] rows() {
        String[] rows = new String[height];
        StringBuilder sb = new StringBuilder(width);
        for (int y = 0; y < height; y++) {
            sb.setLength(0);
            for (int x = 0; x < width; x++) {
                sb.append(get(x, y).symbol);
            }
            rows[y] = sb.toString();
        }
        return rows;
    }

    @Override
    public String toString() {
        return String.join("\n", rows());
    }
}
