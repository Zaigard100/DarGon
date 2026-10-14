package com.zaig100.dargon.level;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Уровень после разбора: карта, точка появления, сущности. Неизменяем, кроме карты: её копию
 * получает игра ({@link TileMap#copy()}), а исходная остаётся для перезапуска уровня.
 */
public final class LevelDefinition {

    /** Текущая версия формата файлов уровней. */
    public static final int FORMAT_VERSION = 1;

    private final String id;
    private final String name;
    private final boolean dark;
    private final boolean checkpoint;
    private final TileMap tiles;
    private final WallEdge[] edges;
    private final GridPoint spawn;
    private final List<EntitySpec> entities;
    private final Map<String, EntitySpec> byId;
    private final String script;

    LevelDefinition(
            String id,
            String name,
            boolean dark,
            boolean checkpoint,
            TileMap tiles,
            WallEdge[] edges,
            GridPoint spawn,
            List<EntitySpec> entities,
            Map<String, EntitySpec> byId,
            String script) {
        this.id = id;
        this.name = name;
        this.dark = dark;
        this.checkpoint = checkpoint;
        this.tiles = tiles;
        this.edges = edges;
        this.spawn = spawn;
        this.entities = Collections.unmodifiableList(entities);
        this.byId = Collections.unmodifiableMap(byId);
        this.script = script;
    }

    /**
     * Идентификатор — путь файла относительно каталога уровней без {@code .json}: {@code
     * "shops/shop_05"}.
     */
    public String id() {
        return id;
    }

    /** Название для игрока: ключ локализации {@code "@level.05.name"} или строка. */
    public String name() {
        return name;
    }

    /** Уровень тёмный: видно только вокруг игрока (пока не зажжён факел). */
    public boolean dark() {
        return dark;
    }

    /** Контрольная точка: при входе на уровень игра сохраняется. */
    public boolean checkpoint() {
        return checkpoint;
    }

    /** Исходная карта. Не изменять — для игры брать {@link TileMap#copy()}. */
    public TileMap tiles() {
        return tiles;
    }

    /**
     * Ручные края стен (поле {@code edges}) по индексу {@code y * width + x}: {@code null} в ячейке
     * — край по автотайлингу. Весь массив {@code null}, если правок нет. Передаётся в {@link
     * Autotiler#compute(TileMap, WallEdge[])}.
     */
    public WallEdge[] edges() {
        return edges == null ? null : edges.clone();
    }

    public GridPoint spawn() {
        return spawn;
    }

    public List<EntitySpec> entities() {
        return entities;
    }

    /** Сущность по {@code id}; {@code null}, если такой нет. */
    public EntitySpec entity(String id) {
        return byId.get(id);
    }

    /**
     * Файл скрипта уровня, как он записан в уровне (относительно файла уровня); или {@code null}.
     */
    public String script() {
        return script;
    }

    /**
     * Путь скрипта относительно каталога уровней: для уровня {@code shops/shop_05} и скрипта {@code
     * shop.dgs} — {@code shops/shop.dgs}.
     */
    public String scriptPath() {
        if (script == null) {
            return null;
        }
        int slash = id.lastIndexOf('/');
        return slash < 0 ? script : id.substring(0, slash + 1) + script;
    }

    @Override
    public String toString() {
        return "Level["
                + id
                + ", "
                + tiles.width()
                + "x"
                + tiles.height()
                + ", "
                + entities.size()
                + " entities]";
    }
}
