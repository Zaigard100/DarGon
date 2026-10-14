package com.zaig100.dargon.level;

/**
 * Выбирает декоративный край для каждой стены по соседям-полам (4-связность).
 *
 * <p>Правила совпадают с DarGon3, кроме обработки края карты: за границей всегда стена, соседи не
 * «заворачивают» на соседнюю строку.
 */
public final class Autotiler {

    private Autotiler() {}

    /** Края для всей карты, индекс — {@code y * width + x}. Для пола — {@link WallEdge#NONE}. */
    public static WallEdge[] compute(TileMap map) {
        return compute(map, null);
    }

    /**
     * Края для всей карты с ручными правками уровня ({@link LevelDefinition#edges()}): где правка
     * задана и клетка — стена, берётся она, иначе край считается по соседям.
     *
     * @param overrides правки по индексу {@code y * width + x}, {@code null} — нет правок
     */
    public static WallEdge[] compute(TileMap map, WallEdge[] overrides) {
        WallEdge[] edges = new WallEdge[map.width() * map.height()];
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                int i = y * map.width() + x;
                WallEdge manual = overrides == null ? null : overrides[i];
                edges[i] = manual != null && !map.isFloor(x, y) ? manual : edge(map, x, y);
            }
        }
        return edges;
    }

    /** Край для одной клетки. */
    public static WallEdge edge(TileMap map, int x, int y) {
        if (map.isFloor(x, y)) {
            return WallEdge.NONE;
        }
        boolean up = map.isFloor(x, y - 1);
        boolean down = map.isFloor(x, y + 1);
        boolean left = map.isFloor(x - 1, y);
        boolean right = map.isFloor(x + 1, y);
        // Порядок проверок важен: при трёх и четырёх соседях побеждает последнее подходящее
        // правило.
        if (up && down && right) {
            return WallEdge.UP_DOWN_RIGHT;
        }
        if (left && down && right) {
            return WallEdge.LEFT_DOWN_RIGHT;
        }
        if (left && up && down) {
            return WallEdge.LEFT_UP_DOWN;
        }
        if (left && up && right) {
            return WallEdge.LEFT_UP_RIGHT;
        }
        if (left && down) {
            return WallEdge.LEFT_DOWN;
        }
        if (left && up) {
            return WallEdge.LEFT_UP;
        }
        if (down && right) {
            return WallEdge.DOWN_RIGHT;
        }
        if (up && right) {
            return WallEdge.UP_RIGHT;
        }
        return WallEdge.NONE;
    }
}
