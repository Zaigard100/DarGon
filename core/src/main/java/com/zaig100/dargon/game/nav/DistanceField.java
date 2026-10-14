package com.zaig100.dargon.game.nav;

import com.zaig100.dargon.level.Direction;
import java.util.Arrays;

/**
 * Расстояния в шагах (8 направлений) от каждой клетки до цели — поиск в ширину от цели. Одно поле
 * обслуживает всех преследователей с этой целью; пересчитывается, только когда меняется цель или
 * проходимость.
 */
public final class DistanceField {

    /** Клетка недостижима. */
    public static final int UNREACHABLE = Integer.MAX_VALUE;

    /**
     * Порядок соседей при равной длине пути — как перебор в DarGon3 (там Y рос вверх): нижний ряд
     * слева направо, затем левый и правый, затем верхний ряд.
     */
    private static final int[][] NEIGHBOURS = {
        {-1, 1}, {0, 1}, {1, 1}, {-1, 0}, {1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };

    private final int width;
    private final int height;
    private final int[] distance;
    private final int[] queue;
    private int targetX = -1;
    private int targetY = -1;
    private int version = -1;
    private int lastUsed = -1;

    public DistanceField(int width, int height) {
        this.width = width;
        this.height = height;
        this.distance = new int[width * height];
        this.queue = new int[width * height];
    }

    /** Пересчитывает поле, если изменились цель или версия проходимости. */
    public void update(int x, int y, int walkabilityVersion, Pathfinder.Walkable walkable) {
        if (x == targetX && y == targetY && walkabilityVersion == version) {
            return;
        }
        targetX = x;
        targetY = y;
        version = walkabilityVersion;
        Arrays.fill(distance, UNREACHABLE);
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        int head = 0;
        int tail = 0;
        distance[y * width + x] = 0;
        queue[tail++] = y * width + x;
        while (head < tail) {
            int current = queue[head++];
            int cx = current % width;
            int cy = current / width;
            for (Direction dir : Direction.values()) {
                int nx = cx + dir.dx;
                int ny = cy + dir.dy;
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                    continue;
                }
                int next = ny * width + nx;
                if (distance[next] == UNREACHABLE && walkable.test(nx, ny)) {
                    distance[next] = distance[current] + 1;
                    queue[tail++] = next;
                }
            }
        }
    }

    /** Поле посчитано для этой цели. */
    public boolean isFor(int x, int y) {
        return targetX == x && targetY == y;
    }

    /** Отметка использования (для переиспользования полей). */
    public void markUsed(int tick) {
        lastUsed = tick;
    }

    public int lastUsed() {
        return lastUsed;
    }

    public int distance(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return UNREACHABLE;
        }
        return distance[y * width + x];
    }

    /**
     * Следующий шаг из {@code (x, y)} к цели путём не длиннее {@code maxSteps}: индекс в {@code
     * out} — {@code out[0], out[1]}. Возвращает {@code false}, если такого пути нет.
     */
    public boolean nextStep(int x, int y, int maxSteps, Pathfinder.Walkable walkable, int[] out) {
        int best = UNREACHABLE;
        for (int[] n : NEIGHBOURS) {
            int nx = x + n[0];
            int ny = y + n[1];
            int d = distance(nx, ny);
            if (d < best && d + 1 <= maxSteps && walkable.test(nx, ny)) {
                best = d;
                out[0] = nx;
                out[1] = ny;
            }
        }
        return best != UNREACHABLE;
    }
}
