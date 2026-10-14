package com.zaig100.dargon.editor;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.kotcrab.vis.ui.VisUI;
import com.zaig100.dargon.editor.model.Json;
import com.zaig100.dargon.editor.model.LevelDoc;
import com.zaig100.dargon.game.GameLog;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.level.Prefab;
import com.zaig100.dargon.level.Problems;
import com.zaig100.dargon.render.GameAssets;
import com.zaig100.dargon.render.LevelRenderer;
import java.util.List;
import java.util.Map;

/**
 * Холст уровня: уровень рисует игровой {@link LevelRenderer} (как в игре), поверх — сетка, границы
 * карты, рамки областей, метки невидимых в игре сущностей, выделение. Колесо — масштаб, средняя
 * кнопка или пробел + перетаскивание — прокрутка.
 */
final class LevelView extends Widget {

    /** Инструмент холста. */
    enum Tool {
        /** ЛКМ — пол, ПКМ — стена. */
        TILES,
        /** ЛКМ — выделить и перетащить; по пустой клетке — поставить выбранный префаб. */
        OBJECTS,
        /** ЛКМ — точка появления героя. */
        SPAWN,
        /** ЛКМ — ручной край стены (выбранный), ПКМ — снова «авто». */
        EDGES
    }

    private static final int TILE = LevelRenderer.TILE;
    private static final int[] ZOOMS = {1, 2, 3, 4, 5, 6, 8};
    private static final Color BACKGROUND = new Color(0.07f, 0.07f, 0.08f, 1);
    private static final Color MAP = new Color(0.11f, 0.12f, 0.1f, 1);
    private static final Color GRID = new Color(1, 1, 1, 0.07f);
    private static final Color BOUNDS = new Color(1, 1, 1, 0.35f);
    private static final Color HOVER = new Color(1, 1, 1, 0.6f);
    private static final Color SELECTION = new Color(1, 0.85f, 0.3f, 1);
    private static final Color AREA_FILL = new Color(1, 0.85f, 0.3f, 0.18f);
    private static final Color AREA = new Color(0.4f, 0.8f, 1f, 0.8f);
    private static final Color MARKER = new Color(0.9f, 0.4f, 0.9f, 0.55f);
    private static final Color PROBLEM = new Color(1, 0.3f, 0.3f, 0.9f);

    private final EditorApp app;
    private final GameAssets assets;
    private final LevelRenderer renderer;
    private final Matrix4 transform = new Matrix4();
    private final Matrix4 saved = new Matrix4();

    private LevelDoc doc;
    private int builtRevision = -1;
    private LevelRuntime runtime;
    private int zoomIndex = 3;
    private int camX;
    private int camY;
    private Tool tool = Tool.OBJECTS;
    private String prefab;

    /** Край для инструмента краёв: {@code .}, {@code #} или {@code 1}–{@code 8}. */
    private char edge = '#';

    /** Цвет метки ручного края. */
    private static final Color MANUAL_EDGE = new Color(0.3f, 0.9f, 1f, 0.9f);

    private int selected = -1;
    private int hoverX = Integer.MIN_VALUE;
    private int hoverY;

    /** Сущности с ошибками: в мире уровня их нет, на холсте — красная метка. */
    private final java.util.Set<Integer> broken = new java.util.HashSet<>();

    /** Ячейки с ошибками текущего уровня — подсвечиваются. */
    private List<int[]> problemCells = List.of();

    // Перетаскивание.
    private boolean painting;
    private boolean paintFloor;
    private char paintEdge;
    private boolean panning;
    private boolean moving;
    private boolean movedOnce;
    private float panStartX;
    private float panStartY;
    private int panCamX;
    private int panCamY;
    private boolean spaceHeld;

    /** Отцентровать карту, когда холст получит размер (при смене уровня). */
    private boolean needsCenter;

    LevelView(EditorApp app, GameAssets assets) {
        this.app = app;
        this.assets = assets;
        this.renderer = new LevelRenderer(assets, app.data().items(), false);
        addListener(new Mouse());
    }

    // ------------------------------------------------------------------ состояние

    void setLevel(LevelDoc doc) {
        this.doc = doc;
        areaStart = null;
        selected = -1;
        builtRevision = -1;
        needsCenter = true;
    }

    /** Данные проекта поменялись (свои префабы): уровень собрать заново. */
    void dataChanged() {
        builtRevision = -1;
    }

    LevelDoc level() {
        return doc;
    }

    void setTool(Tool tool) {
        this.tool = tool;
        areaStart = null;
    }

    Tool tool() {
        return tool;
    }

    void setPrefab(String prefab) {
        this.prefab = prefab;
        areaStart = null;
        tool = Tool.OBJECTS;
    }

    /** Первый угол области (зона, кусты), пока ждём второй щелчок; иначе {@code null}. */
    private int[] areaStart;

    /** Угол поставлен этим же нажатием: отпускание на другой клетке — второй угол. */
    private boolean areaByPress;

    /** Префаб — область {@code width×height}: ставится двумя углами. */
    boolean isAreaPrefab(String id) {
        Prefab p = id == null ? null : app.data().prefabs().get(id);
        return p != null && p.params().containsKey("width") && p.params().containsKey("height");
    }

    /** Ждём второй угол области. */
    boolean isPlacingArea() {
        return areaStart != null;
    }

    /** Отменить начатую область (Esc). */
    boolean cancelArea() {
        boolean had = areaStart != null;
        areaStart = null;
        return had;
    }

    /** Поставить область от первого угла до клетки ({@code x}, {@code y}). */
    private void finishArea(int x, int y) {
        int x0 = Math.min(areaStart[0], x);
        int y0 = Math.min(areaStart[1], y);
        long w = Math.abs(areaStart[0] - x) + 1L;
        long h = Math.abs(areaStart[1] - y) + 1L;
        areaStart = null;
        doc.checkpoint();
        int index = doc.addEntity(prefab, x0, y0);
        Map<String, Object> e = doc.entities().get(index);
        app.prefill(doc, e);
        e.put("width", w);
        e.put("height", h);
        doc.changed();
        select(index);
        app.areaChanged();
    }

    void setEdge(char edge) {
        this.edge = edge;
        tool = Tool.EDGES;
    }

    char edge() {
        return edge;
    }

    String prefab() {
        return prefab;
    }

    int selected() {
        return selected;
    }

    void select(int index) {
        selected = doc == null || index >= doc.entities().size() ? -1 : index;
        app.selectionChanged();
    }

    void setSpaceHeld(boolean held) {
        spaceHeld = held;
    }

    void setProblemCells(List<int[]> cells) {
        problemCells = cells;
    }

    int zoom() {
        return ZOOMS[zoomIndex];
    }

    /** Карта по центру холста. */
    void center() {
        if (doc == null) {
            return;
        }
        float w = getWidth() / zoom();
        float h = getHeight() / zoom();
        camX = Math.round(doc.width() * TILE / 2f - w / 2);
        camY = Math.round(doc.height() * TILE / 2f - h / 2);
    }

    void zoomBy(int delta, float localX, float localY) {
        int next = Math.max(0, Math.min(ZOOMS.length - 1, zoomIndex + delta));
        if (next == zoomIndex) {
            return;
        }
        // Точка под курсором остаётся на месте.
        float levelX = camX + localX / zoom();
        float levelY = camY + (getHeight() - localY) / zoom();
        zoomIndex = next;
        camX = Math.round(levelX - localX / zoom());
        camY = Math.round(levelY - (getHeight() - localY) / zoom());
    }

    /** Крупнейший масштаб, при котором карта видна целиком; карта по центру. */
    void fit() {
        if (doc == null || getWidth() <= 0) {
            return;
        }
        zoomIndex = 0;
        for (int i = ZOOMS.length - 1; i >= 0; i--) {
            if ((doc.width() + 1) * TILE * ZOOMS[i] <= getWidth()
                    && (doc.height() + 1) * TILE * ZOOMS[i] <= getHeight()) {
                zoomIndex = i;
                break;
            }
        }
        center();
    }

    /** Масштаб на шаг относительно центра холста. */
    void zoomStep(int delta) {
        zoomBy(delta, getWidth() / 2, getHeight() / 2);
    }

    /** Сдвинуть вид, чтобы сущность была видна (если она за краем холста). */
    void reveal(int index) {
        if (doc == null || index < 0 || index >= doc.entities().size()) {
            return;
        }
        int[] at = LevelDoc.at(doc.entities().get(index));
        float px = (at[0] + 0.5f) * TILE;
        float py = (at[1] + 0.5f) * TILE;
        float w = getWidth() / zoom();
        float h = getHeight() / zoom();
        if (px < camX + TILE || px > camX + w - TILE || py < camY + TILE || py > camY + h - TILE) {
            camX = Math.round(px - w / 2);
            camY = Math.round(py - h / 2);
        }
    }

    /** Состояние старого холста (после пересборки интерфейса при смене масштаба). */
    void takeStateFrom(LevelView old) {
        doc = old.doc;
        tool = old.tool;
        prefab = old.prefab;
        edge = old.edge;
        zoomIndex = old.zoomIndex;
        camX = old.camX;
        camY = old.camY;
        selected = old.selected;
        needsCenter = old.needsCenter;
        problemCells = old.problemCells;
    }

    /** Удаляет выделенную сущность. */
    void deleteSelected() {
        if (doc == null || selected < 0) {
            return;
        }
        doc.checkpoint();
        doc.entities().remove(selected);
        doc.changed();
        select(-1);
    }

    /** Копия выделенной сущности на соседней клетке справа. */
    void duplicateSelected() {
        if (doc == null || selected < 0) {
            return;
        }
        doc.checkpoint();
        Map<String, Object> copy = Json.copy(doc.entities().get(selected));
        copy.remove("id");
        int[] at = LevelDoc.at(copy);
        copy.put("at", LevelDoc.point(at[0] + 1, at[1]));
        doc.entities().add(copy);
        doc.changed();
        select(doc.entities().size() - 1);
    }

    // ------------------------------------------------------------------ рисование

    /** Пересобрать мир уровня, если документ изменился (для рендерера). */
    private void rebuild() {
        if (doc == null || doc.revision() == builtRevision) {
            return;
        }
        builtRevision = doc.revision();
        Problems problems = new Problems(doc.id());
        LevelDefinition def = app.data().parser().parse(doc.toJson(), doc.id(), problems);
        app.liveProblems(doc, problems);
        broken.clear();
        if (def == null) {
            def = withoutBroken(problems);
            if (def == null) {
                return; // ошибка не в сущностях (карта) — показываем последний удачный вид
            }
        }
        try {
            LevelRuntime next =
                    LevelRuntime.start(
                            def,
                            PlayerState.newGame(app.data().items()),
                            new ActionState(),
                            null,
                            GameLog.NONE);
            if (runtime != null) {
                runtime.dispose();
            }
            runtime = next;
            renderer.bind(runtime.world());
        } catch (RuntimeException e) {
            app.status("Уровень не собирается: " + e.getMessage());
        }
    }

    /**
     * Уровень с ошибками в сущностях всё равно рисуется: без сломанных сущностей (они отмечаются
     * красным), чтобы правки карты и остальных сущностей были видны сразу.
     */
    private LevelDefinition withoutBroken(Problems problems) {
        java.util.TreeSet<Integer> bad =
                new java.util.TreeSet<>(java.util.Comparator.reverseOrder());
        for (com.zaig100.dargon.level.Problem p : problems.list()) {
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("^entities\\[(\\d+)]").matcher(p.path());
            if (p.isError()) {
                if (!m.find()) {
                    return null;
                }
                bad.add(Integer.parseInt(m.group(1)));
            }
        }
        LevelDoc copy = new LevelDoc(doc.id(), Json.copy(doc.root()));
        for (int i : bad) {
            if (i < copy.entities().size()) {
                copy.entities().remove(i);
            }
        }
        broken.addAll(bad);
        return app.data().parser().parse(copy.toJson(), doc.id(), new Problems(doc.id()));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        // Координаты родителя: батч уже сдвинут панелями-разделителями.
        Vector2 origin = new Vector2(getX(), getY());
        fill(batch, origin.x, origin.y, getWidth(), getHeight(), BACKGROUND);
        if (doc == null) {
            return;
        }
        rebuild();
        if (needsCenter && getWidth() > 0) {
            center();
            needsCenter = false;
        }
        batch.flush();
        if (!clipBegin(origin.x, origin.y, getWidth(), getHeight())) {
            return;
        }
        int z = zoom();
        int viewW = (int) Math.ceil(getWidth() / z) + 1;
        int viewH = (int) Math.ceil(getHeight() / z) + 1;
        // Подложка карты: стены без края иначе не видны.
        fill(
                batch,
                sx(origin, 0),
                sy(origin, doc.height() * TILE),
                doc.width() * TILE * z,
                doc.height() * TILE * z,
                MAP);
        if (runtime != null) {
            saved.set(batch.getTransformMatrix());
            // Уровень в пикселях уровня; низ вида совпадает с низом холста.
            float bottom = origin.y + getHeight() - viewH * z;
            transform.set(saved).translate(origin.x, bottom, 0).scale(z, z, 1);
            batch.setTransformMatrix(transform);
            if (batch instanceof SpriteBatch) {
                renderer.renderArea(
                        (SpriteBatch) batch, runtime, camX, camY, viewH == 0 ? 1 : viewW, viewH);
            }
            batch.setTransformMatrix(saved);
        }
        drawGrid(batch, origin);
        drawEntities(batch, origin);
        batch.flush();
        clipEnd();
    }

    /** Экранная X левого края клетки-пикселя уровня {@code px}. */
    private float sx(Vector2 origin, int px) {
        return origin.x + (px - camX) * zoom();
    }

    /** Экранная Y нижнего края для пикселя уровня {@code py} (Y уровня вниз). */
    private float sy(Vector2 origin, int py) {
        return origin.y + getHeight() - (py - camY) * zoom();
    }

    private void drawGrid(Batch batch, Vector2 origin) {
        int z = zoom();
        int w = doc.width();
        int h = doc.height();
        if (z >= 3) {
            for (int x = 1; x < w; x++) {
                fill(batch, sx(origin, x * TILE), sy(origin, h * TILE), 1, h * TILE * z, GRID);
            }
            for (int y = 1; y < h; y++) {
                fill(batch, sx(origin, 0), sy(origin, y * TILE), w * TILE * z, 1, GRID);
            }
        }
        outline(batch, sx(origin, 0), sy(origin, h * TILE), w * TILE * z, h * TILE * z, BOUNDS, 1);
        // Ручные края — уголок, чтобы было видно, где край задан вручную.
        if (tool == Tool.EDGES || z >= 3) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (doc.edgeAt(x, y) != '.') {
                        float s = TILE * z;
                        float cx = sx(origin, x * TILE);
                        float cy = sy(origin, (y + 1) * TILE);
                        float m = Math.max(3, s / 6);
                        fill(batch, cx + s - m - 1, cy + s - m - 1, m, m, MANUAL_EDGE);
                    }
                }
            }
        }
        for (int[] cell : problemCells) {
            cellOutline(batch, origin, cell[0], cell[1], 1, 1, PROBLEM, 2);
        }
        if (hoverX != Integer.MIN_VALUE) {
            cellOutline(batch, origin, hoverX, hoverY, 1, 1, HOVER, 1);
            drawGhost(batch, origin);
        }
    }

    /** Что поставится щелчком: картинка выбранного префаба полупрозрачно под курсором. */
    private void drawGhost(Batch batch, Vector2 origin) {
        if (tool == Tool.OBJECTS && isAreaPrefab(prefab) && nearMap(hoverX, hoverY)) {
            int x0 = areaStart == null ? hoverX : Math.min(areaStart[0], hoverX);
            int y0 = areaStart == null ? hoverY : Math.min(areaStart[1], hoverY);
            int w = areaStart == null ? 1 : Math.abs(areaStart[0] - hoverX) + 1;
            int h = areaStart == null ? 1 : Math.abs(areaStart[1] - hoverY) + 1;
            float s = TILE * zoom();
            fill(
                    batch,
                    sx(origin, x0 * TILE),
                    sy(origin, (y0 + h) * TILE),
                    w * s,
                    h * s,
                    AREA_FILL);
            cellOutline(batch, origin, x0, y0, w, h, SELECTION, 2);
            if (areaStart != null) {
                BitmapFont font = VisUI.getSkin().getFont("default-font");
                font.setColor(Color.WHITE);
                font.draw(batch, w + "×" + h, sx(origin, x0 * TILE) + 4, sy(origin, y0 * TILE) - 4);
            }
            return;
        }
        if (tool != Tool.OBJECTS
                || prefab == null
                || !nearMap(hoverX, hoverY)
                || doc.entityAt(hoverX, hoverY) >= 0) {
            return;
        }
        float s = TILE * zoom();
        float x = sx(origin, hoverX * TILE);
        float y = sy(origin, (hoverY + 1) * TILE);
        TextureRegion icon = app.prefabIcon(prefab);
        if (icon != null) {
            batch.setColor(1, 1, 1, 0.6f);
            float h = s * icon.getRegionHeight() / TILE;
            batch.draw(icon, x, y, s * icon.getRegionWidth() / TILE, h);
            batch.setColor(Color.WHITE);
        } else {
            fill(batch, x + s * 0.2f, y + s * 0.2f, s * 0.6f, s * 0.6f, MARKER);
        }
        cellOutline(batch, origin, hoverX, hoverY, 1, 1, SELECTION, 1);
    }

    private void drawEntities(Batch batch, Vector2 origin) {
        BitmapFont font = VisUI.getSkin().getFont("small-font");
        List<Map<String, Object>> entities = doc.entities();
        for (int i = 0; i < entities.size(); i++) {
            Map<String, Object> e = entities.get(i);
            int[] at = LevelDoc.at(e);
            Prefab p = app.data().prefabs().get(String.valueOf(e.get("prefab")));
            int w = Json.asInt(e.get("width"), 1);
            int h = Json.asInt(e.get("height"), 1);
            if (p != null && p.params().containsKey("width") && (w > 1 || h > 1)) {
                cellOutline(batch, origin, at[0], at[1], w, h, AREA, 1);
            }
            if (p == null || broken.contains(i) || !visibleInGame(p, e)) {
                // Скрытые и выключенные сущности в игре не видны — метка, чтобы их было видно
                // здесь.
                float x = sx(origin, at[0] * TILE);
                float y = sy(origin, (at[1] + 1) * TILE);
                float s = TILE * zoom();
                fill(
                        batch,
                        x + s * 0.15f,
                        y + s * 0.15f,
                        s * 0.7f,
                        s * 0.7f,
                        broken.contains(i) ? PROBLEM : MARKER);
                if (zoom() >= 3) {
                    String label = p == null ? "?" : shortName(p.id());
                    font.setColor(Color.WHITE);
                    font.draw(batch, label, x + 2, y + s / 2 + font.getCapHeight() / 2);
                }
            }
            if (i == selected) {
                cellOutline(batch, origin, at[0], at[1], 1, 1, SELECTION, 2);
            }
        }
    }

    /** Нарисует ли игра эту сущность: есть картинка, не скрыта, не выключена. */
    private static boolean visibleInGame(Prefab p, Map<String, Object> e) {
        if (Boolean.FALSE.equals(e.get("enabled")) || p.hasComponent("Hidden")) {
            return false;
        }
        return p.hasComponent("SpriteRef") || p.hasComponent("Pickup") || p.hasComponent("Emitter");
    }

    private static String shortName(String id) {
        return id.length() <= 4 ? id : id.substring(0, 4);
    }

    private void cellOutline(
            Batch batch, Vector2 origin, int cx, int cy, int w, int h, Color color, int thick) {
        int z = zoom();
        outline(
                batch,
                sx(origin, cx * TILE),
                sy(origin, (cy + h) * TILE),
                w * TILE * z,
                h * TILE * z,
                color,
                thick);
    }

    private void outline(Batch batch, float x, float y, float w, float h, Color c, int t) {
        fill(batch, x, y, w, t, c);
        fill(batch, x, y + h - t, w, t, c);
        fill(batch, x, y, t, h, c);
        fill(batch, x + w - t, y, t, h, c);
    }

    private void fill(Batch batch, float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(white(), x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    private TextureRegion whiteRegion;

    private TextureRegion white() {
        if (whiteRegion == null) {
            whiteRegion = new TextureRegion(assets.whitePixel());
        }
        return whiteRegion;
    }

    // ------------------------------------------------------------------ ввод

    private int cellX(float localX) {
        return Math.floorDiv(Math.round(camX + localX / zoom() - 0.5f), TILE);
    }

    private int cellY(float localY) {
        return Math.floorDiv(Math.round(camY + (getHeight() - localY) / zoom() - 0.5f), TILE);
    }

    /** Сущности можно ставить и за краем карты (огнемёты «в стене» снаружи) — на клетку. */
    private boolean nearMap(int x, int y) {
        return x >= -1 && y >= -1 && x <= doc.width() && y <= doc.height();
    }

    private final class Mouse extends InputListener {
        @Override
        public boolean mouseMoved(InputEvent event, float x, float y) {
            hover(x, y);
            return false;
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
            getStage().setScrollFocus(LevelView.this);
        }

        @Override
        public void exit(InputEvent event, float x, float y, int pointer, Actor to) {
            if (pointer == -1) {
                hoverX = Integer.MIN_VALUE;
            }
        }

        @Override
        public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
            zoomBy(amountY > 0 ? -1 : 1, x, y);
            return true;
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            getStage().setKeyboardFocus(LevelView.this);
            if (doc == null) {
                return false;
            }
            if (button == Buttons.MIDDLE || (spaceHeld && button == Buttons.LEFT)) {
                panning = true;
                panStartX = x;
                panStartY = y;
                panCamX = camX;
                panCamY = camY;
                return true;
            }
            int cx = cellX(x);
            int cy = cellY(y);
            boolean left = button == Buttons.LEFT;
            switch (tool) {
                case TILES -> {
                    if (button != Buttons.LEFT && button != Buttons.RIGHT) {
                        return false;
                    }
                    painting = true;
                    paintFloor = left;
                    doc.checkpoint();
                    if (doc.setFloor(cx, cy, paintFloor)) {
                        doc.changed();
                    }
                }
                case EDGES -> {
                    if (button != Buttons.LEFT && button != Buttons.RIGHT) {
                        return false;
                    }
                    painting = true;
                    paintEdge = left ? edge : '.';
                    doc.checkpoint();
                    if (doc.setEdge(cx, cy, paintEdge)) {
                        doc.changed();
                    }
                }
                case SPAWN -> {
                    if (left && doc.inBounds(cx, cy)) {
                        doc.checkpoint();
                        doc.setSpawn(cx, cy);
                        doc.changed();
                    }
                }
                case OBJECTS -> {
                    if (!left) {
                        // ПКМ: отменить начатую область, иначе — снова просто курсор.
                        if (!cancelArea()) {
                            select(-1);
                            app.stopPlacing();
                        }
                        app.areaChanged();
                        return true;
                    }
                    if (isAreaPrefab(prefab) && nearMap(cx, cy)) {
                        // Область: первый щелчок — угол, второй (или отпускание) — другой угол.
                        if (areaStart == null) {
                            areaStart = new int[] {cx, cy};
                            areaByPress = true;
                            app.areaChanged();
                        } else {
                            finishArea(cx, cy);
                        }
                        return true;
                    }
                    int hit = doc.entityAt(cx, cy);
                    if (hit >= 0) {
                        // Повторный щелчок по той же клетке перебирает сущности в ней.
                        if (hit == selected) {
                            hit = cycle(cx, cy, hit);
                        }
                        select(hit);
                        moving = true;
                        movedOnce = false;
                    } else if (prefab != null && nearMap(cx, cy)) {
                        doc.checkpoint();
                        int index = doc.addEntity(prefab, cx, cy);
                        app.prefill(doc, doc.entities().get(index));
                        doc.changed();
                        select(index);
                    } else {
                        select(-1);
                    }
                }
                default -> {}
            }
            return true;
        }

        @Override
        public void touchDragged(InputEvent event, float x, float y, int pointer) {
            hover(x, y);
            if (panning) {
                camX = panCamX - Math.round((x - panStartX) / zoom());
                camY = panCamY + Math.round((y - panStartY) / zoom());
                return;
            }
            int cx = cellX(x);
            int cy = cellY(y);
            if (painting && tool == Tool.EDGES) {
                if (doc.setEdge(cx, cy, paintEdge)) {
                    doc.changed();
                }
            } else if (painting && doc.setFloor(cx, cy, paintFloor)) {
                doc.changed();
            } else if (moving && selected >= 0 && nearMap(cx, cy)) {
                Map<String, Object> e = doc.entities().get(selected);
                int[] at = LevelDoc.at(e);
                if (at[0] != cx || at[1] != cy) {
                    if (!movedOnce) {
                        doc.checkpoint();
                        movedOnce = true;
                    }
                    e.put("at", LevelDoc.point(cx, cy));
                    doc.changed();
                    app.selectionChanged();
                }
            }
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            if (areaStart != null && areaByPress) {
                areaByPress = false;
                int cx = cellX(x);
                int cy = cellY(y);
                if ((cx != areaStart[0] || cy != areaStart[1]) && nearMap(cx, cy)) {
                    finishArea(cx, cy); // протянули мышью
                }
            }
            painting = false;
            panning = false;
            moving = false;
        }

        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            return false;
        }

        private void hover(float x, float y) {
            hoverX = cellX(x);
            hoverY = cellY(y);
            if (doc != null) {
                app.hover(hoverX, hoverY);
            }
        }
    }

    /** Следующая сущность в той же клетке (по кругу). */
    private int cycle(int cx, int cy, int current) {
        List<Map<String, Object>> list = doc.entities();
        for (int step = 1; step <= list.size(); step++) {
            int i = Math.floorMod(current - step, list.size());
            int[] at = LevelDoc.at(list.get(i));
            if (at[0] == cx && at[1] == cy) {
                return i;
            }
        }
        return current;
    }

    @Override
    public float getMinWidth() {
        return 0;
    }

    @Override
    public float getMinHeight() {
        return 0;
    }

    @Override
    public float getPrefWidth() {
        return 400;
    }

    @Override
    public float getPrefHeight() {
        return 300;
    }

    void dispose() {
        if (runtime != null) {
            runtime.dispose();
        }
    }
}
