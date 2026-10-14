package com.zaig100.dargon.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.zaig100.dargon.ui.Theme;
import java.util.HashMap;
import java.util.Map;

/**
 * Внешний вид интерфейса DarGon: шрифт GFont, белый текст, жёлтый фокус, полупрозрачные тёмные
 * панели. Размеры шрифтов — в логических пикселях; при смене масштаба окна тему пересоздают.
 */
public final class GameTheme implements Theme, Disposable {

    /** Стили текста: размер шрифта (логические пиксели) и цвет. */
    private static final Map<String, Integer> SIZES =
            Map.of(
                    "title", 9, "button", 5, "small", 4, "hint", 4, "warn", 4, "tiny", 3, "sign", 5,
                    "link", 3);

    private static final int DEFAULT_SIZE = 6;

    private static final Map<String, Color> COLORS =
            Map.of(
                    "hint", new Color(0.7f, 0.7f, 0.72f, 1f),
                    "warn", new Color(1f, 0.45f, 0.4f, 1f),
                    "button", new Color(0.9f, 0.9f, 0.9f, 1f),
                    "sign", new Color(0x663931ff),
                    "link", new Color(0.55f, 0.72f, 1f, 1f));

    private static final Color FOCUS = new Color(1f, 0.85f, 0.3f, 1f);

    private final GameAssets assets;
    private final int scale;
    private final Texture white;
    private final Map<String, Drawable> drawables = new HashMap<>();
    private final Drawable focus;
    private final Runnable pressed;
    private final Runnable moved;

    public GameTheme(GameAssets assets, int scale, Runnable pressed, Runnable moved) {
        this.assets = assets;
        this.scale = scale;
        this.pressed = pressed;
        this.moved = moved;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        white = new Texture(pixmap);
        pixmap.dispose();
        drawables.put("panel", tinted(0.06f, 0.06f, 0.08f, 1f));
        drawables.put("dim", tinted(0f, 0f, 0f, 0.6f));
        drawables.put("slot", tinted(1f, 1f, 1f, 0.08f));
        // Табличка: деревянная рамка, тянется под текст.
        drawables.put(
                "sign", new NinePatchDrawable(new NinePatch(assets.region("sign"), 2, 2, 2, 2)));
        focus = tinted(1f, 1f, 1f, 0.16f);
    }

    private Drawable tinted(float r, float g, float b, float a) {
        return new TextureRegionDrawable(new TextureRegion(white)).tint(new Color(r, g, b, a));
    }

    @Override
    public BitmapFont font(String style) {
        return assets.uiFont(SIZES.getOrDefault(style == null ? "" : style, DEFAULT_SIZE), scale);
    }

    @Override
    public Color color(String style) {
        return COLORS.getOrDefault(style == null ? "" : style, Color.WHITE);
    }

    @Override
    public Color focusColor() {
        return FOCUS;
    }

    @Override
    public Drawable drawable(String name) {
        return drawables.computeIfAbsent(name, n -> new TextureRegionDrawable(assets.region(n)));
    }

    @Override
    public Drawable focusBackground() {
        return focus;
    }

    @Override
    public void pressed() {
        pressed.run();
    }

    @Override
    public void focusMoved() {
        moved.run();
    }

    public int scale() {
        return scale;
    }

    @Override
    public void dispose() {
        white.dispose();
    }
}
