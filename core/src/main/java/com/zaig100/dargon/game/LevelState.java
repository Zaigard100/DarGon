package com.zaig100.dargon.game;

import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.ecs.Entity;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.level.Autotiler;
import com.zaig100.dargon.level.EntitySpec;
import com.zaig100.dargon.level.JsonValues;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.level.TileMap;
import com.zaig100.dargon.level.TileType;
import com.zaig100.dargon.level.WallEdge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ресурс мира: карта уровня (изменяемая копия), края стен, игрок и исход уровня. */
public final class LevelState {

    private static final String DEFAULT_KEY_COLOR = "#FFFFFF";

    /** За сколько секунд факел разгорается. */
    public static final float TORCH_IGNITE = 3f;

    private final LevelDefinition definition;
    private final TileMap tiles;
    private final WallEdge[] manualEdges;
    private WallEdge[] edges;
    private final PlayerState player;
    private int playerEntity = Entity.NONE;
    private LevelOutcome outcome = LevelOutcome.NONE;
    private int tilesVersion;
    private boolean dark;

    /** Сколько секунд горит факел; -1 — не зажжён. */
    private float torchTime = -1;

    private float time;
    private boolean trapSight;
    private boolean revealAll;
    private final List<String> message = new ArrayList<>();
    private int openShop = Entity.NONE;
    private final Map<String, String> keyColors = new HashMap<>();

    public LevelState(LevelDefinition definition, PlayerState player) {
        this.definition = definition;
        this.tiles = definition.tiles().copy();
        this.manualEdges = definition.edges();
        this.edges = Autotiler.compute(tiles, manualEdges);
        this.player = player;
        this.dark = definition.dark();
        for (EntitySpec entity : definition.entities()) {
            for (JsonValue param : entity.params().values()) {
                collectKeyColors(param);
            }
        }
        for (ItemStack stack : player.inventory.items()) {
            if (stack.field("key") != null && stack.field("color") != null) {
                keyColors.putIfAbsent(
                        String.valueOf(stack.field("key")), String.valueOf(stack.field("color")));
            }
        }
    }

    /** Ключи в параметрах сущностей (предмет, сундук, товары магазина): ключ → цвет. */
    private void collectKeyColors(JsonValue value) {
        if (value.isObject()) {
            JsonValue key = JsonValues.get(value, "key");
            JsonValue color = JsonValues.get(value, "color");
            if ("key".equals(JsonValues.getString(value, "id", null))
                    && key != null
                    && key.isString()) {
                keyColors.putIfAbsent(
                        key.asString(),
                        color != null && color.isString() ? color.asString() : DEFAULT_KEY_COLOR);
            }
        }
        for (JsonValue child = value.child; child != null; child = child.next) {
            collectKeyColors(child);
        }
    }

    /** Цвет ключа {@code key} (как у предмета-ключа на уровне), для подсказок у дверей. */
    public String keyColor(String key) {
        return keyColors.getOrDefault(key, DEFAULT_KEY_COLOR);
    }

    /** Темно ли сейчас: уровень тёмный и факел не зажжён. */
    public boolean isDark() {
        return dark && torchTime < 0;
    }

    /** Уровень тёмный (с факелом или без). */
    public boolean isDarkLevel() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    /** Зажечь факел: темнота плавно отходит к краям за {@link #TORCH_IGNITE} секунд. */
    public void lightTorch() {
        if (torchTime < 0) {
            torchTime = 0;
        }
    }

    /** Как разгорелся факел: 0 — не зажжён, 1 — полностью. */
    public float torchProgress() {
        return torchTime < 0 ? 0 : Math.min(1f, torchTime / TORCH_IGNITE);
    }

    /** Время игры на уровне (для анимаций, которые идут игровым временем). */
    public float time() {
        return time;
    }

    /** Один тик времени уровня. */
    public void advance(float delta) {
        time += delta;
        if (torchTime >= 0) {
            torchTime += delta;
        }
    }

    /** Действует зелье «показать ловушки» (или разработчик показывает всё скрытое). */
    public boolean trapSight() {
        return trapSight || revealAll;
    }

    /** Выпито зелье «показать ловушки» (без учёта инструментов разработчика). */
    public boolean trapSightPotion() {
        return trapSight;
    }

    public void setRevealAll(boolean revealAll) {
        this.revealAll = revealAll;
    }

    public void setTrapSight(boolean trapSight) {
        this.trapSight = trapSight;
    }

    /** Текст таблички на экране (пусто — нет); скрывается, когда игрок пошёл. */
    public List<String> message() {
        return message;
    }

    public void showMessage(List<String> lines) {
        message.clear();
        message.addAll(lines);
    }

    /** Открытый магазин или {@link Entity#NONE}. */
    public int openShop() {
        return openShop;
    }

    public void setOpenShop(int shop) {
        this.openShop = shop;
    }

    public LevelDefinition definition() {
        return definition;
    }

    /** Текущая карта. Менять только через {@link #setTile}. */
    public TileMap tiles() {
        return tiles;
    }

    /** Край стены в клетке (для отрисовки). */
    public WallEdge edge(int x, int y) {
        return tiles.inBounds(x, y) ? edges[y * tiles.width() + x] : WallEdge.NONE;
    }

    /** Меняет клетку карты и пересчитывает края стен. */
    public void setTile(int x, int y, TileType type) {
        tiles.set(x, y, type);
        edges = Autotiler.compute(tiles, manualEdges);
        tilesVersion++;
    }

    /** Растёт при каждом изменении проходимости: клетки карты, двери (для кэшей поиска пути). */
    public int tilesVersion() {
        return tilesVersion;
    }

    /** Проходимость изменилась не через карту (открылась или закрылась дверь). */
    public void walkabilityChanged() {
        tilesVersion++;
    }

    public PlayerState player() {
        return player;
    }

    public int playerEntity() {
        return playerEntity;
    }

    void setPlayerEntity(int entity) {
        this.playerEntity = entity;
    }

    public LevelOutcome outcome() {
        return outcome;
    }

    public void finish(LevelOutcome outcome) {
        if (this.outcome == LevelOutcome.NONE) {
            this.outcome = outcome;
        }
    }
}
