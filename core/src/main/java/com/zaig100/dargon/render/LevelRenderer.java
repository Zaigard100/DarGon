package com.zaig100.dargon.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.LevelState;
import com.zaig100.dargon.game.component.Area;
import com.zaig100.dargon.game.component.AreaEffect;
import com.zaig100.dargon.game.component.Container;
import com.zaig100.dargon.game.component.Door;
import com.zaig100.dargon.game.component.Emitter;
import com.zaig100.dargon.game.component.Facing;
import com.zaig100.dargon.game.component.Fragile;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Hazard;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Layer;
import com.zaig100.dargon.game.component.LevelExit;
import com.zaig100.dargon.game.component.Lock;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.component.Pickup;
import com.zaig100.dargon.game.component.PlayerAnim;
import com.zaig100.dargon.game.component.Projectile;
import com.zaig100.dargon.game.component.ScriptHandlers;
import com.zaig100.dargon.game.component.ShopStock;
import com.zaig100.dargon.game.component.SpriteRef;
import com.zaig100.dargon.game.component.Teleporter;
import com.zaig100.dargon.game.component.Text;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.TileMap;
import com.zaig100.dargon.level.WallEdge;

/**
 * Рисует уровень в логическом разрешении {@value #VIEW_WIDTH}×{@value #VIEW_HEIGHT} (7×5 клеток),
 * игрок — в центре. Пиксели — пиксели тайлов; на экран картинку переносит {@link PixelScreen}.
 *
 * <p>Внутри координаты «Y вниз», как в уровне; переворот — только в {@link #draw}.
 */
public final class LevelRenderer {

    public static final int TILE = 16;
    public static final int VIEW_WIDTH = 7 * TILE;
    public static final int VIEW_HEIGHT = 5 * TILE;

    /** Сколько секунд держится красная вспышка урона. */
    public static final float DAMAGE_FLASH = 0.5f;

    private static final float ITEM_SCALE = 0.5f;
    private static final float SHIELD_SCALE = 0.75f;

    /** Частота мигания героя при неуязвимости (смен в секунду). */
    private static final float BLINK_RATE = 12f;

    private static final float LABEL_SCALE = 0.5f;

    private final GameAssets assets;
    private final ItemCatalog items;
    private final boolean newYear;
    private final Color tint = new Color();

    private EntitySet drawable;
    private EntitySet all;
    private ComponentMapper<GridPos> pos;
    private ComponentMapper<Motion> motion;
    private ComponentMapper<SpriteRef> sprite;
    private ComponentMapper<Layer> layer;
    private ComponentMapper<Pickup> pickup;
    private ComponentMapper<Health> health;
    private ComponentMapper<PlayerAnim> anim;
    private ComponentMapper<Facing> facing;
    private ComponentMapper<Emitter> emitter;
    private ComponentMapper<Area> area;
    private ComponentMapper<AreaEffect> areaEffect;
    private ComponentMapper<Door> door;
    private ComponentMapper<Lock> lock;
    private ComponentMapper<Container> container;
    private ComponentMapper<Marker.Disabled> disabled;
    private ComponentMapper<Hazard> hazard;
    private ComponentMapper<Fragile> fragile;
    private ComponentMapper<Projectile> projectile;
    private ComponentMapper<LevelExit> exits;
    private ComponentMapper<Teleporter> teleporters;
    private ComponentMapper<ShopStock> shops;
    private ComponentMapper<ScriptHandlers> buttons;
    private ComponentMapper<Text> texts;

    /** Левый верхний угол вида в пикселях уровня (Y вниз). */
    private int viewLeft;

    private int viewTop;

    /** Размер вида в пикселях: в игре 7×5 клеток, в редакторе — сколько видно. */
    private int viewWidth = VIEW_WIDTH;

    private int viewHeight = VIEW_HEIGHT;

    /** Клетка вида, где стоит герой (по умолчанию — в центре 7×5). */
    private int heroColumn = 3;

    private int heroRow = 2;
    private SpriteBatch batch;

    /** Центр игрока в пикселях вида (Y вниз) — для темноты. */
    private float centerX;

    private float centerY;

    /**
     * @param newYear новогодний костюм героя (31 декабря — 7 января, как в DarGon3)
     */
    public LevelRenderer(GameAssets assets, ItemCatalog items, boolean newYear) {
        this.assets = assets;
        this.items = items;
        this.newYear = newYear;
    }

    /** Где в виде 7×5 держать героя: колонка 0..6 слева, ряд 0..4 сверху (фон меню). */
    public void setHeroCell(int column, int row) {
        heroColumn = column;
        heroRow = row;
    }

    /** Привязка к миру нового уровня. */
    public void bind(World world) {
        drawable =
                world.entities(
                        Family.all(GridPos.class)
                                .withOne(SpriteRef.class, Pickup.class, Emitter.class)
                                .exclude(Marker.Disabled.class, Marker.Hidden.class));
        all = world.entities(Family.all(GridPos.class).exclude(Marker.Player.class));
        pos = world.mapper(GridPos.class);
        motion = world.mapper(Motion.class);
        sprite = world.mapper(SpriteRef.class);
        layer = world.mapper(Layer.class);
        pickup = world.mapper(Pickup.class);
        health = world.mapper(Health.class);
        anim = world.mapper(PlayerAnim.class);
        facing = world.mapper(Facing.class);
        emitter = world.mapper(Emitter.class);
        area = world.mapper(Area.class);
        areaEffect = world.mapper(AreaEffect.class);
        door = world.mapper(Door.class);
        lock = world.mapper(Lock.class);
        container = world.mapper(Container.class);
        disabled = world.mapper(Marker.Disabled.class);
        hazard = world.mapper(Hazard.class);
        fragile = world.mapper(Fragile.class);
        projectile = world.mapper(Projectile.class);
        exits = world.mapper(LevelExit.class);
        teleporters = world.mapper(Teleporter.class);
        shops = world.mapper(ShopStock.class);
        buttons = world.mapper(ScriptHandlers.class);
        texts = world.mapper(Text.class);
    }

    /**
     * Рисует кадр. Батч уже начат, проекция — логический вид {@code VIEW_WIDTH×VIEW_HEIGHT}.
     *
     * @param alpha доля времени между прошлым и текущим тиком — для плавности
     */
    public void render(SpriteBatch batch, LevelRuntime level, float alpha) {
        this.batch = batch;
        int player = level.player();
        int px = Math.round(pixelX(player, alpha));
        int py = Math.round(pixelY(player, alpha));
        viewLeft = px - heroColumn * TILE;
        viewTop = py - heroRow * TILE;
        LevelState state = level.state();
        drawTiles(state);
        drawEntities(Layer.Z.FLOOR, alpha);
        drawPlayer(player, px, py);
        drawEntities(Layer.Z.OVER, alpha);
        centerX = px - viewLeft + TILE / 2f;
        centerY = py - viewTop + TILE / 2f;
    }

    /**
     * Редактор паков: участок уровня {@code width×height} пикселей с левым верхним углом ({@code
     * left}, {@code top}) в пикселях уровня; камера за героем не следит. Батч начат, проекция —
     * {@code 0..width × 0..height}.
     */
    public void renderArea(
            SpriteBatch batch, LevelRuntime level, int left, int top, int width, int height) {
        this.batch = batch;
        viewLeft = left;
        viewTop = top;
        viewWidth = width;
        viewHeight = height;
        int player = level.player();
        drawTiles(level.state());
        drawEntities(Layer.Z.FLOOR, 0);
        drawPlayer(player, Math.round(pixelX(player, 0)), Math.round(pixelY(player, 0)));
        drawEntities(Layer.Z.OVER, 0);
        viewWidth = VIEW_WIDTH;
        viewHeight = VIEW_HEIGHT;
    }

    /**
     * Верхний слой (поверх темноты, как в DarGon3): подсветка зелья «показать ловушки», ключи над
     * дверями, красная вспышка урона. Вызывать после {@link #render} в том же кадре.
     */
    public void renderOverlay(SpriteBatch batch, LevelRuntime level) {
        this.batch = batch;
        if (level.state().trapSight()) {
            drawTrapSight();
        }
        drawLabels(level);
        Health h = health.get(level.player());
        if (h.sinceDamage < DAMAGE_FLASH) {
            batch.draw(assets.region("damage"), 0, 0, VIEW_WIDTH, VIEW_HEIGHT);
        }
    }

    /**
     * Темнота тёмного уровня — гладкая, в разрешении окна. Без шейдера — текстура DarGon3 в
     * пиксельном виде (и факел снимает темноту сразу).
     */
    public void renderDarkness(SpriteBatch batch, PixelScreen screen, LevelRuntime level) {
        LevelState state = level.state();
        if (!state.isDarkLevel()) {
            return;
        }
        ShaderProgram shader = assets.darkness();
        if (shader == null) {
            if (state.isDark()) {
                screen.beginOverlay(batch);
                this.batch = batch;
                TextureRegion dark = assets.region("dark");
                float x = centerX - dark.getRegionWidth() / 2f;
                float y = VIEW_HEIGHT - centerY - dark.getRegionHeight() / 2f;
                batch.draw(dark, x, y);
                screen.endOverlay(batch);
            }
            return;
        }
        screen.drawSmooth(
                batch,
                shader,
                assets.whitePixel(),
                () -> {
                    shader.setUniformf("u_view", VIEW_WIDTH, VIEW_HEIGHT);
                    shader.setUniformf("u_center", centerX, centerY);
                    shader.setUniformf("u_torch", state.torchProgress());
                    shader.setUniformf("u_time", state.time());
                });
    }

    /** Клетка уровня под точкой вида (Y вниз, пиксели вида) — для кликов. */
    public int cellX(float viewX) {
        return Math.floorDiv(Math.round(viewLeft + viewX - 0.5f), TILE);
    }

    public int cellY(float viewY) {
        return Math.floorDiv(Math.round(viewTop + viewY - 0.5f), TILE);
    }

    private void drawTiles(LevelState state) {
        TileMap tiles = state.tiles();
        int x0 = Math.floorDiv(viewLeft, TILE);
        int y0 = Math.floorDiv(viewTop, TILE);
        for (int y = y0; y <= y0 + viewHeight / TILE; y++) {
            for (int x = x0; x <= x0 + viewWidth / TILE; x++) {
                if (!tiles.inBounds(x, y)) {
                    continue;
                }
                if (tiles.isFloor(x, y)) {
                    draw(assets.tile(0, 0), x * TILE, y * TILE, TILE, TILE, false, 0);
                } else {
                    WallEdge edge = state.edge(x, y);
                    if (edge != WallEdge.NONE) {
                        draw(
                                assets.tile(edge.column, edge.row),
                                x * TILE,
                                y * TILE,
                                TILE,
                                TILE,
                                false,
                                0);
                    }
                }
            }
        }
    }

    private void drawEntities(Layer.Z z, float alpha) {
        for (int i = 0; i < drawable.size(); i++) {
            int entity = drawable.get(i);
            Layer l = layer.getOrNull(entity);
            if ((l == null ? Layer.Z.FLOOR : l.z) != z) {
                continue;
            }
            int x = Math.round(pixelX(entity, alpha));
            int y = Math.round(pixelY(entity, alpha));
            Emitter e = emitter.getOrNull(entity);
            if (e != null && e.kind.equals("fire")) {
                drawFire(e, x, y);
            }
            SpriteRef s = sprite.getOrNull(entity);
            Door d = door.getOrNull(entity);
            if (s != null && s.region != null && (d == null || !d.open)) {
                drawSprite(entity, s, x, y);
            }
            Pickup p = pickup.getOrNull(entity);
            if (p != null) {
                drawItem(p.item, x, y);
            }
        }
    }

    private void drawSprite(int entity, SpriteRef s, int x, int y) {
        TextureRegion region = assets.frame(s.region, s.frame);
        Area a = area.getOrNull(entity);
        if (a != null) {
            // Кусты: картинка в каждой клетке области.
            for (int row = 0; row < a.height; row++) {
                for (int column = 0; column < a.width; column++) {
                    draw(region, x + column * TILE, y + row * TILE, TILE, TILE, s.flipX, 0);
                }
            }
            return;
        }
        Facing f = facing.getOrNull(entity);
        float rotation = f == null ? 0 : s.angle + f.dir.degrees();
        int w = f == null ? region.getRegionWidth() : TILE;
        int h = f == null ? region.getRegionHeight() : TILE;
        // Высокие кадры (двери 16×32) растут вверх от клетки.
        draw(region, x + s.offsetX, y + TILE - h + s.offsetY, w, h, s.flipX, rotation);
    }

    /** Пламя огнемёта: {@code stage} клеток по направлению, сквозь стены. */
    private void drawFire(Emitter e, int x, int y) {
        TextureRegion fire = assets.frame("fire", e.fireFrame);
        for (int i = 1; i <= e.stage; i++) {
            draw(
                    fire,
                    x + e.dir.dx * i * TILE,
                    y + e.dir.dy * i * TILE,
                    TILE,
                    TILE,
                    false,
                    e.dir.degrees() - 90);
        }
    }

    /** Предмет на полу — половинного размера в центре клетки. */
    private void drawItem(ItemStack stack, int x, int y) {
        int size = Math.round(TILE * ITEM_SCALE);
        int inset = (TILE - size) / 2;
        drawIcon(stack, x + inset, y + inset, size);
    }

    /** Значок предмета размером {@code size} (ключ — своего цвета). */
    private void drawIcon(ItemStack stack, int x, int y, int size) {
        drawTinted(ItemIcons.name(items, stack), ItemIcons.tint(stack), x, y, size);
    }

    private void drawTinted(String region, String hex, int x, int y, int size) {
        if (hex != null) {
            batch.setColor(tint.set(Color.valueOf(hex)));
        }
        draw(assets.frame(region, 0), x, y, size, size, false, 0);
        batch.setColor(Color.WHITE);
    }

    private void drawPlayer(int player, int x, int y) {
        Health h = health.get(player);
        PlayerAnim a = anim.get(player);
        if (h.isDead() && h.lastDamageKind != null) {
            String corpse = corpse(h.lastDamageKind);
            // Упавший в пролом герой не виден.
            if (corpse != null) {
                draw(assets.region(corpse), x, y, TILE, TILE, false, 0);
            }
            if (corpse != null || h.lastDamageKind.equals("fall")) {
                return;
            }
        }
        // Неуязвимость после удара — герой мигает.
        if (h.invulnerable > 0 && ((int) (h.invulnerable * BLINK_RATE)) % 2 == 1) {
            return;
        }
        String strip = newYear ? "ng_sprite" : a.running && !h.isDead() ? "run" : "idle";
        draw(assets.frame(strip, a.frame), x, y, TILE, TILE, a.flipX, 0);
        if (h.hasShield() && !h.isDead()) {
            int size = Math.round(TILE * SHIELD_SCALE);
            draw(
                    assets.region("sheld"),
                    x + (a.flipX ? TILE / 4 : 0),
                    y + TILE - size,
                    size,
                    size,
                    false,
                    0);
        }
    }

    /** Картинка смерти по виду последнего урона; {@code null} — обычный герой или пролом. */
    private static String corpse(String kind) {
        switch (kind) {
            case "trap":
            case "explosion":
            case "zone":
                return "amonghero";
            case "fire":
                return "firedhero";
            case "arrow":
                return "shotedhero";
            case "spikes":
                return "spikedhero";
            default:
                return null;
        }
    }

    /**
     * Зелье «показать ловушки»: опасное — красным, полезное — зелёным, механизмы и препятствия —
     * синим, зоны — по клеткам. Рисуется поверх темноты.
     */
    private void drawTrapSight() {
        for (int i = 0; i < all.size(); i++) {
            int entity = all.get(i);
            if (disabled.has(entity)) {
                continue;
            }
            GridPos p = pos.get(entity);
            int x = p.x * TILE;
            int y = p.y * TILE;
            AreaEffect zone = areaEffect.getOrNull(entity);
            Area a = area.getOrNull(entity);
            if (zone != null && a != null) {
                String region = zone.effect.equals("heal") ? "green_sm_obj" : "red_sm_obj";
                for (int row = 0; row < a.height; row++) {
                    for (int column = 0; column < a.width; column++) {
                        draw(
                                assets.region(region),
                                x + column * TILE,
                                y + row * TILE,
                                TILE,
                                TILE,
                                false,
                                0);
                    }
                }
            } else if (hazard.has(entity)
                    || emitter.has(entity)
                    || fragile.has(entity)
                    || projectile.has(entity)) {
                draw(assets.region("damage"), x, y, TILE, TILE, false, 0);
            } else if (pickup.has(entity)
                    || exits.has(entity)
                    || teleporters.has(entity)
                    || container.has(entity)
                    || shops.has(entity)) {
                draw(assets.region("green_obj"), x, y, TILE, TILE, false, 0);
            } else if (a != null) {
                draw(assets.region("blue_obj"), x, y, a.width * TILE, a.height * TILE, false, 0);
            } else if (door.has(entity) || buttons.has(entity) || texts.has(entity)) {
                draw(assets.region("blue_obj"), x, y, TILE, TILE, false, 0);
            }
        }
    }

    /** Над закрытой дверью и запертым сундуком рядом с игроком — ключ нужного цвета. */
    private void drawLabels(LevelRuntime level) {
        GridPos at = level.playerPos();
        for (int i = 0; i < all.size(); i++) {
            int entity = all.get(i);
            Lock l = lock.getOrNull(entity);
            if (l == null || l.key == null || disabled.has(entity)) {
                continue;
            }
            Door d = door.getOrNull(entity);
            boolean shut = d != null ? !d.open : l.locked;
            GridPos p = pos.get(entity);
            if (!shut || Math.abs(p.x - at.x) > 1 || Math.abs(p.y - at.y) > 1) {
                continue;
            }
            int size = Math.round(TILE * LABEL_SCALE);
            int lift = d != null ? 2 * TILE : TILE;
            drawTinted(
                    items.get("key") == null ? "key" : items.get("key").icon(),
                    level.state().keyColor(l.key),
                    p.x * TILE + (TILE - size) / 2,
                    p.y * TILE + TILE - lift - size / 2,
                    size);
        }
    }

    private float pixelX(int entity, float alpha) {
        Motion m = motion.getOrNull(entity);
        float offset = m == null ? 0 : m.prevOffsetX + (m.offsetX - m.prevOffsetX) * alpha;
        return pos.get(entity).x * TILE + offset;
    }

    private float pixelY(int entity, float alpha) {
        Motion m = motion.getOrNull(entity);
        float offset = m == null ? 0 : m.prevOffsetY + (m.offsetY - m.prevOffsetY) * alpha;
        return pos.get(entity).y * TILE + offset;
    }

    /** Рисует прямоугольник, заданный в пикселях уровня с Y вниз; поворот — вокруг центра. */
    private void draw(
            TextureRegion region,
            int x,
            int y,
            int width,
            int height,
            boolean flipX,
            float rotation) {
        float left = x - viewLeft;
        float bottom = viewHeight - (y - viewTop) - height;
        float scaleX = flipX ? -1 : 1;
        batch.draw(
                region, left, bottom, width / 2f, height / 2f, width, height, scaleX, 1, rotation);
    }
}
