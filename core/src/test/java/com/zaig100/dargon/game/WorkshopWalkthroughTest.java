package com.zaig100.dargon.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Hazard;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.script.ScriptHost;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.script.Value;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Пак-пример «Мастерская» ({@code examples/workshop}) проходится от первого уровня до последнего:
 * бот выполняет маршрут из {@code workshop-routes.txt} в настоящем движке, без читов. Падает, если
 * герой погиб, застрял или вышел не туда.
 *
 * <p>Команды маршрута:
 *
 * <ul>
 *   <li>{@code level <id>} — начать уровень, {@code exit <id>} — ожидаемый выход, {@code exit end}
 *       — конец игры;
 *   <li>{@code go x y} — дойти по полу в обход опасных клеток, {@code go! x y} — напрямую;
 *   <li>{@code goe <выражение>} — дойти до клетки {@code [x, y]}, которую вернул DGScript уровня;
 *   <li>{@code use}, {@code wait <сек>}, {@code until <выражение> [сек]} — ждать, пока выражение
 *       DGScript уровня истинно (число в конце — таймаут, по умолчанию 30 с).
 * </ul>
 */
class WorkshopWalkthroughTest {

    private static final Path ROOT = Path.of(System.getProperty("dargon.root", ".."));
    private static final Path PACK = ROOT.resolve("examples/workshop");

    private final ActionState input = new ActionState();
    private final List<String> log = new ArrayList<>();

    /** Пройденные уровни — в сообщение об ошибке: видно, докуда дошёл бот. */
    private final List<String> report = new ArrayList<>();

    private LevelRuntime level;
    private String where = "";

    private static AssetReader dir(Path root) {
        return path -> {
            Path file = root.resolve(path);
            try {
                return Files.isRegularFile(file)
                        ? Files.readString(file, StandardCharsets.UTF_8)
                        : null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    @Test
    void wholePackCanBeFinished() throws IOException {
        GameData data = GameData.load(dir(ROOT.resolve("assets")), dir(PACK));
        PlayerState player = PlayerState.newGame(data.items());
        List<String> lines =
                Files.readAllLines(
                        Path.of(
                                System.getProperty(
                                        "dargon.routes",
                                        "src/test/resources/workshop-routes.txt")));
        List<String> visited = new ArrayList<>();
        int n = 0;
        for (String raw : lines) {
            n++;
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] w = line.split("\\s+", 2);
            where =
                    (level == null ? "" : level.state().definition().id())
                            + " строка "
                            + n
                            + " (пройдены: "
                            + report
                            + ")";
            switch (w[0]) {
                case "level" -> {
                    LevelDefinition def = data.loadLevel(w[1]);
                    String script =
                            def.scriptPath() == null ? null : data.readScript(def.scriptPath());
                    log.clear();
                    level =
                            LevelRuntime.start(
                                    def,
                                    player,
                                    input,
                                    script,
                                    data::readScript,
                                    new GameLog() {
                                        @Override
                                        public void info(String message) {
                                            log.add(message);
                                        }

                                        @Override
                                        public void error(String message) {
                                            fail(def.id() + ": ошибка скрипта: " + message);
                                        }
                                    },
                                    new Cheats(),
                                    new Random(n),
                                    null);
                    visited.add(w[1]);
                }
                case "go", "go!" -> {
                    String[] xy = w[1].split("\\s+");
                    go(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]), w[0].equals("go"));
                }
                case "goe" -> {
                    String at = eval(w[1]).display();
                    String[] xy = at.replaceAll("[\\[\\] ]", "").split(",");
                    go(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]), true);
                }
                case "use" -> {
                    input.press(Action.USE);
                    input.release(Action.USE);
                    ticks(2);
                }
                case "wait" -> ticks(Math.round(Float.parseFloat(w[1]) * 60));
                case "until" -> until(w[1]);
                case "exit" -> {
                    ticks(1);
                    LevelOutcome outcome = level.outcome();
                    boolean end = w[1].equals("end");
                    assertEquals(
                            end ? LevelOutcome.Kind.GAME_END : LevelOutcome.Kind.EXIT,
                            outcome.kind(),
                            where + ": " + log);
                    assertEquals(end ? null : w[1], outcome.next(), where);
                    report.add(
                            level.state().definition().id()
                                    + " (здоровье "
                                    + player.hp
                                    + ", монет "
                                    + player.coins
                                    + ")");
                    level.dispose();
                    level = null;
                }
                default -> fail(where + ": неизвестная команда " + line);
            }
        }
        assertTrue(level == null, "маршрут кончился посреди уровня");
        assertEquals(data.levels().size(), visited.size(), "пройдены не все уровни: " + visited);
    }

    private Value eval(String code) {
        return level.world().resource(ScriptHost.class).evaluate(code, "маршрут");
    }

    private void until(String args) {
        String expr = args;
        float seconds = 30;
        int bracket = args.lastIndexOf(' ');
        if (bracket > 0 && args.substring(bracket + 1).matches("\\d+(\\.\\d+)?")) {
            seconds = Float.parseFloat(args.substring(bracket + 1));
            expr = args.substring(0, bracket);
        }
        for (int i = 0; i < seconds * 60; i++) {
            if (eval(expr).isTruthy()) {
                return;
            }
            ticks(1);
        }
        fail(where + ": не дождались " + expr);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            level.tick();
            if (level.isPlayerDead()) {
                fail(where + ": герой погиб; " + log);
            }
        }
    }

    /** Шаг за шагом к клетке; путь пересчитывается перед каждым шагом (мир меняется). */
    private void go(int tx, int ty, boolean safe) {
        Occupancy occupancy = level.world().resource(Occupancy.class);
        int waited = 0;
        while (true) {
            GridPos at = level.playerPos();
            if (at.x == tx && at.y == ty) {
                return;
            }
            int[] next = nextStep(occupancy, at.x, at.y, tx, ty, safe);
            if (next == null) {
                if (++waited > 60 * 20) {
                    fail(where + ": нет пути из " + at.x + "," + at.y + " в " + tx + "," + ty);
                }
                ticks(1);
                continue;
            }
            Action a =
                    next[0] > at.x
                            ? Action.MOVE_RIGHT
                            : next[0] < at.x
                                    ? Action.MOVE_LEFT
                                    : next[1] > at.y ? Action.MOVE_DOWN : Action.MOVE_UP;
            input.press(a);
            input.release(a);
            ticks(1);
            while (level.world()
                    .get(level.player(), com.zaig100.dargon.game.component.Motion.class)
                    .isMoving()) {
                ticks(1);
            }
        }
    }

    private int[] nextStep(Occupancy occupancy, int sx, int sy, int tx, int ty, boolean safe) {
        int w = level.state().tiles().width();
        int h = level.state().tiles().height();
        boolean[] danger = new boolean[w * h];
        if (safe) {
            EntitySet hazards =
                    level.world()
                            .entities(
                                    Family.all(Hazard.class, GridPos.class)
                                            .exclude(Marker.Disabled.class));
            for (int i = 0; i < hazards.size(); i++) {
                int e = hazards.get(i);
                Hazard hz = level.world().get(e, Hazard.class);
                GridPos p = level.world().get(e, GridPos.class);
                com.zaig100.dargon.game.component.TriggeredCycle cycle =
                        level.world()
                                .mapper(com.zaig100.dargon.game.component.TriggeredCycle.class)
                                .getOrNull(e);
                boolean idle = cycle != null && !cycle.active;
                if (!hz.whenBroken && !idle && level.state().tiles().inBounds(p.x, p.y)) {
                    danger[p.y * w + p.x] = true;
                }
            }
        }
        int[] prev = new int[w * h];
        java.util.Arrays.fill(prev, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int start = sy * w + sx;
        prev[start] = start;
        queue.add(start);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int c = queue.poll();
            int cx = c % w;
            int cy = c / w;
            if (cx == tx && cy == ty) {
                int step = c;
                while (prev[step] != start) {
                    step = prev[step];
                }
                return new int[] {step % w, step / w};
            }
            for (int[] d : dirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                    continue;
                }
                int ni = ny * w + nx;
                boolean goal = nx == tx && ny == ty;
                if (prev[ni] < 0
                        && occupancy.isWalkableForPlayer(nx, ny)
                        && (goal || !danger[ni])) {
                    prev[ni] = c;
                    queue.add(ni);
                }
            }
        }
        return null;
    }
}
