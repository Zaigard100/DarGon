package com.zaig100.dargon.editor.model;

import com.zaig100.dargon.level.JsonPrinter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Уровень, открытый в редакторе: JSON уровня как изменяемое дерево (неизвестные поля сохраняются) и
 * история отмены — снимки уровня до каждой правки.
 */
public final class LevelDoc {

    /** Порядок полей уровня в файле. */
    private static final List<String> ORDER =
            List.of(
                    "$schema",
                    "formatVersion",
                    "name",
                    "dark",
                    "checkpoint",
                    "legend",
                    "tiles",
                    "edges",
                    "spawn",
                    "entities",
                    "script",
                    "comment");

    private static final int HISTORY = 200;

    private String id;
    private Map<String, Object> root;
    private final Deque<Map<String, Object>> undo = new ArrayDeque<>();
    private final Deque<Map<String, Object>> redo = new ArrayDeque<>();
    private boolean dirty;

    /** Счётчик правок: виды перерисовывают уровень, когда он меняется. */
    private int revision;

    public LevelDoc(String id, Map<String, Object> root) {
        this.id = id;
        this.root = root;
    }

    /** Новый уровень: пол {@code width×height}, герой в левом верхнем углу. */
    public static LevelDoc create(String id, int width, int height, String schema) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (schema != null) {
            root.put("$schema", schema);
        }
        root.put("formatVersion", 1L);
        root.put("name", "@level." + id + ".name");
        List<Object> rows = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            rows.add(".".repeat(width));
        }
        root.put("tiles", rows);
        root.put("spawn", point(0, 0));
        root.put("entities", new ArrayList<>());
        LevelDoc doc = new LevelDoc(id, root);
        doc.dirty = true;
        return doc;
    }

    public String id() {
        return id;
    }

    void rename(String newId) {
        id = newId;
        dirty = true;
    }

    public Map<String, Object> root() {
        return root;
    }

    public int revision() {
        return revision;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markSaved() {
        dirty = false;
    }

    // ------------------------------------------------------------------ правки и отмена

    /** Перед правкой: запомнить уровень для отмены. Одна «правка» — одно действие игрока. */
    public void checkpoint() {
        undo.push(Json.copy(root));
        if (undo.size() > HISTORY) {
            undo.removeLast();
        }
        redo.clear();
    }

    /** После правки (или во время перетаскивания): уровень изменился. */
    public void changed() {
        dirty = true;
        revision++;
    }

    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        redo.push(root);
        root = undo.pop();
        changed();
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        undo.push(root);
        root = redo.pop();
        changed();
        return true;
    }

    // ------------------------------------------------------------------ карта

    @SuppressWarnings("unchecked")
    public List<Object> rows() {
        return (List<Object>) root.computeIfAbsent("tiles", k -> new ArrayList<>());
    }

    public int width() {
        int w = 0;
        for (Object row : rows()) {
            w = Math.max(w, row.toString().length());
        }
        return w;
    }

    public int height() {
        return rows().size();
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && y < height() && x < rows().get(y).toString().length();
    }

    public boolean isFloor(int x, int y) {
        return inBounds(x, y) && isFloorSymbol(rows().get(y).toString().charAt(x));
    }

    /** Символ карты — пол? По легенде уровня ({@code "legend"}), без неё — всё, кроме {@code #}. */
    private boolean isFloorSymbol(char c) {
        if (root.get("legend") instanceof Map<?, ?> legend) {
            return "floor".equals(legend.get(String.valueOf(c)));
        }
        return c != '#';
    }

    /**
     * Символ пола или стены для записи в карту: из легенды уровня (по возможности {@code .} и
     * {@code #}); если в легенде такого типа нет — он добавляется.
     */
    @SuppressWarnings("unchecked")
    private char symbol(boolean floor) {
        char preferred = floor ? '.' : '#';
        if (!(root.get("legend") instanceof Map<?, ?> legend)) {
            return preferred;
        }
        String type = floor ? "floor" : "wall";
        if (type.equals(legend.get(String.valueOf(preferred)))) {
            return preferred;
        }
        for (Map.Entry<?, ?> e : legend.entrySet()) {
            if (type.equals(e.getValue()) && String.valueOf(e.getKey()).length() == 1) {
                return String.valueOf(e.getKey()).charAt(0);
            }
        }
        if (!legend.containsKey(String.valueOf(preferred))) {
            ((Map<String, Object>) legend).put(String.valueOf(preferred), type);
            return preferred;
        }
        for (char c = '!'; c <= '~'; c++) {
            if (!legend.containsKey(String.valueOf(c))) {
                ((Map<String, Object>) legend).put(String.valueOf(c), type);
                return c;
            }
        }
        throw new IllegalStateException("в легенде нет свободных символов");
    }

    /** Ставит пол или стену; вне карты — ничего. {@code true}, если клетка изменилась. */
    public boolean setFloor(int x, int y, boolean floor) {
        if (!inBounds(x, y) || isFloor(x, y) == floor) {
            return false;
        }
        StringBuilder row = new StringBuilder(rows().get(y).toString());
        row.setCharAt(x, symbol(floor));
        rows().set(y, row.toString());
        clearEdge(x, y);
        return true;
    }

    /** Ручной край клетки сбрасывается на «авто», когда клетка меняется. */
    private void clearEdge(int x, int y) {
        setEdge(x, y, '.');
    }

    /** Ручной край клетки: {@code .} — авто, {@code #} — без края, {@code 1}–{@code 8} — край. */
    public char edgeAt(int x, int y) {
        if (root.get("edges") instanceof List<?> list && y >= 0 && y < list.size()) {
            String row = String.valueOf(list.get(y));
            if (x >= 0 && x < row.length()) {
                return row.charAt(x);
            }
        }
        return '.';
    }

    /**
     * Ставит ручной край клетки; все клетки «авто» — поле {@code edges} убирается. {@code true},
     * если изменилось.
     */
    @SuppressWarnings("unchecked")
    public boolean setEdge(int x, int y, char symbol) {
        if (!inBounds(x, y) || edgeAt(x, y) == symbol) {
            return false;
        }
        List<Object> edges;
        if (root.get("edges") instanceof List<?> list && list.size() == height()) {
            edges = (List<Object>) list;
        } else {
            edges = new ArrayList<>();
            for (int row = 0; row < height(); row++) {
                edges.add(".".repeat(width()));
            }
            root.put("edges", edges);
        }
        StringBuilder row = new StringBuilder(String.valueOf(edges.get(y)));
        while (row.length() < width()) {
            row.append('.');
        }
        row.setCharAt(x, symbol);
        edges.set(y, row.toString());
        boolean manual = false;
        for (Object r : edges) {
            manual |= !String.valueOf(r).chars().allMatch(c -> c == '.');
        }
        if (!manual) {
            root.remove("edges");
        }
        return true;
    }

    /**
     * Новый размер карты. {@code dx}, {@code dy} — сдвиг старой карты (добавление слева/сверху):
     * сущности и точка появления сдвигаются вместе с ней. Новые клетки — стены.
     */
    @SuppressWarnings("unchecked")
    public void resize(int width, int height, int dx, int dy) {
        List<Object> old = rows();
        List<Object> rows = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < width; x++) {
                int ox = x - dx;
                int oy = y - dy;
                boolean floor =
                        oy >= 0
                                && oy < old.size()
                                && ox >= 0
                                && ox < old.get(oy).toString().length()
                                && isFloorSymbol(old.get(oy).toString().charAt(ox));
                row.append(symbol(floor));
            }
            rows.add(row.toString());
        }
        root.put("tiles", rows);
        // Ручные края едут вместе с картой; новые клетки — «авто».
        if (root.get("edges") instanceof List<?> oldEdges) {
            List<Object> edges = new ArrayList<>();
            boolean manual = false;
            for (int y = 0; y < height; y++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < width; x++) {
                    int ox = x - dx;
                    int oy = y - dy;
                    char c = '.';
                    if (oy >= 0 && oy < oldEdges.size()) {
                        String oldRow = String.valueOf(oldEdges.get(oy));
                        if (ox >= 0 && ox < oldRow.length()) {
                            c = oldRow.charAt(ox);
                        }
                    }
                    manual |= c != '.';
                    row.append(c);
                }
                edges.add(row.toString());
            }
            if (manual) {
                root.put("edges", edges);
            } else {
                root.remove("edges");
            }
        }
        if (dx != 0 || dy != 0) {
            int[] spawn = spawn();
            setSpawn(spawn[0] + dx, spawn[1] + dy);
            for (Map<String, Object> e : entities()) {
                int[] at = at(e);
                e.put("at", point(at[0] + dx, at[1] + dy));
            }
        }
    }

    // ------------------------------------------------------------------ герой и сущности

    public int[] spawn() {
        return pointOf(root.get("spawn"));
    }

    public void setSpawn(int x, int y) {
        root.put("spawn", point(x, y));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> entities() {
        return (List<Map<String, Object>>)
                (List<?>) root.computeIfAbsent("entities", k -> new ArrayList<>());
    }

    public static int[] at(Map<String, Object> entity) {
        return pointOf(entity.get("at"));
    }

    /** Сущность в клетке (верхняя — последняя в списке) или {@code -1}. */
    public int entityAt(int x, int y) {
        List<Map<String, Object>> list = entities();
        for (int i = list.size() - 1; i >= 0; i--) {
            int[] at = at(list.get(i));
            if (at[0] == x && at[1] == y) {
                return i;
            }
        }
        return -1;
    }

    /** Добавляет сущность префаба в клетку; возвращает её индекс. */
    public int addEntity(String prefab, int x, int y) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("prefab", prefab);
        e.put("at", point(x, y));
        entities().add(e);
        return entities().size() - 1;
    }

    public static List<Object> point(int x, int y) {
        List<Object> p = new ArrayList<>();
        p.add((long) x);
        p.add((long) y);
        return p;
    }

    private static int[] pointOf(Object value) {
        if (value instanceof List<?> list && list.size() == 2) {
            return new int[] {Json.asInt(list.get(0), 0), Json.asInt(list.get(1), 0)};
        }
        return new int[] {0, 0};
    }

    // ------------------------------------------------------------------ файл

    /** Текст файла уровня: поля в привычном порядке, строки карты — по одной на строку. */
    public String toJson() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : ORDER) {
            if (root.containsKey(key)) {
                out.put(key, root.get(key));
            }
        }
        for (Map.Entry<String, Object> e : root.entrySet()) {
            out.putIfAbsent(e.getKey(), e.getValue());
        }
        for (String key : List.of("tiles", "edges", "legend")) {
            if (out.get(key) instanceof List<?> list) {
                out.put(key, new JsonPrinter.Multiline(list));
            }
        }
        return JsonPrinter.write(out);
    }
}
