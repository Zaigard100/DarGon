package com.zaig100.dargon.game.nav;

import com.zaig100.dargon.level.Direction;
import com.zaig100.dargon.level.GridPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Кратчайший путь по сетке в ширину (BFS) с теми же правилами шага, что у игрока: 8 направлений,
 * проверяется только клетка назначения.
 */
public final class Pathfinder {

    /** Можно ли встать в клетку. */
    @FunctionalInterface
    public interface Walkable {
        boolean test(int x, int y);
    }

    private final int width;
    private final int height;
    private final int[] from;
    private final int[] queue;

    public Pathfinder(int width, int height) {
        this.width = width;
        this.height = height;
        this.from = new int[width * height];
        this.queue = new int[width * height];
    }

    /**
     * Путь из {@code start} в {@code goal} без стартовой клетки; пустой, если они совпадают; {@code
     * null}, если пути нет.
     */
    public List<GridPoint> find(GridPoint start, GridPoint goal, Walkable walkable) {
        if (!inBounds(start) || !inBounds(goal) || !walkable.test(goal.x(), goal.y())) {
            return null;
        }
        if (start.equals(goal)) {
            return List.of();
        }
        Arrays.fill(from, -1);
        int startIndex = index(start.x(), start.y());
        int goalIndex = index(goal.x(), goal.y());
        from[startIndex] = startIndex;
        int head = 0;
        int tail = 0;
        queue[tail++] = startIndex;
        while (head < tail) {
            int current = queue[head++];
            if (current == goalIndex) {
                return path(startIndex, goalIndex);
            }
            int cx = current % width;
            int cy = current / width;
            for (Direction dir : Direction.values()) {
                int nx = cx + dir.dx;
                int ny = cy + dir.dy;
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                    continue;
                }
                int next = index(nx, ny);
                if (from[next] < 0 && walkable.test(nx, ny)) {
                    from[next] = current;
                    queue[tail++] = next;
                }
            }
        }
        return null;
    }

    private List<GridPoint> path(int startIndex, int goalIndex) {
        List<GridPoint> path = new ArrayList<>();
        for (int i = goalIndex; i != startIndex; i = from[i]) {
            path.add(new GridPoint(i % width, i / width));
        }
        Collections.reverse(path);
        return path;
    }

    private boolean inBounds(GridPoint p) {
        return p.x() >= 0 && p.y() >= 0 && p.x() < width && p.y() < height;
    }

    private int index(int x, int y) {
        return y * width + x;
    }
}
