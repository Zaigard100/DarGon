package com.zaig100.dargon.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

/**
 * Графика, шрифты и звуки игры. Кадры атласа нарезаются на полосы 16×16 один раз; шрифт
 * генерируется под нужный размер в пикселях экрана и кэшируется.
 */
public final class GameAssets implements Disposable {

    public static final String ATLAS = "atlas/game.atlas";
    private static final String FONT = "fonts/GFont.ttf";
    private static final String[] SOUNDS = {"lov", "wasted", "click1", "click2"};

    /** Символы шрифта: латиница, кириллица, знаки. */
    private static final String CHARS =
            FreeTypeFontGenerator.DEFAULT_CHARS
                    + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя«»„“”—–№…";

    /** Пары «символ, которого может не быть в шрифте → похожий»: тексты пишутся как обычно. */
    private static final String SUBSTITUTES = "«\"»\"„\"“\"”\"—-–-№N….";

    private static final int FRAME = 16;

    /** Картинка «неизвестный предмет» — вместо отсутствующих. */
    private static final String MISSING = "x";

    /** Высота строки интерфейса относительно кегля. */
    private static final float LINE_HEIGHT = 1.35f;

    /** Нижний вынос строки интерфейса относительно кегля. */
    private static final float DESCENT = 0.2f;

    private static final int TILESET_COLUMNS = 5;
    private static final int TILESET_ROWS = 4;

    private final AssetManager manager = new AssetManager();
    private final Map<String, TextureRegion[]> frames = new HashMap<>();
    private final Map<Integer, BitmapFont> fonts = new HashMap<>();
    private final Map<Integer, BitmapFont> uiFonts = new HashMap<>();
    private Texture white;
    private ShaderProgram darkness;
    private boolean darknessTried;
    private final Map<String, TextureRegion> regions = new HashMap<>();

    /** Текстуры пака: имя → картинка; заменяют игровые с тем же именем. */
    private final Map<String, Texture> packTextures = new HashMap<>();

    private TextureRegion[] tiles;
    private TextureAtlas atlas;
    private FreeTypeFontGenerator fontGenerator;

    /** Загружает всё синхронно (асинхронный экран загрузки — позже). */
    public void load() {
        startLoading();
        manager.finishLoading();
        finishLoading();
    }

    /** Ставит всё в очередь асинхронной загрузки. */
    public void startLoading() {
        manager.load(ATLAS, TextureAtlas.class);
        for (String sound : SOUNDS) {
            manager.load(soundPath(sound), Sound.class);
        }
    }

    /**
     * Шаг асинхронной загрузки (вызывать раз в кадр).
     *
     * @return {@code true}, когда всё загружено и готово
     */
    public boolean update() {
        if (atlas != null) {
            return true;
        }
        if (manager.update()) {
            finishLoading();
            return true;
        }
        return false;
    }

    /** Доля загруженного, 0..1. */
    public float progress() {
        return manager.getProgress();
    }

    private void finishLoading() {
        atlas = manager.get(ATLAS, TextureAtlas.class);
        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal(FONT));
        buildTiles();
    }

    /** Плитки 16×16 из тайлсета (игрового или пака). */
    private void buildTiles() {
        TextureRegion tileset = region("tileset");
        tiles = new TextureRegion[TILESET_COLUMNS * TILESET_ROWS];
        for (int row = 0; row < TILESET_ROWS; row++) {
            for (int column = 0; column < TILESET_COLUMNS; column++) {
                tiles[row * TILESET_COLUMNS + column] =
                        new TextureRegion(tileset, column * FRAME, row * FRAME, FRAME, FRAME);
            }
        }
    }

    /**
     * Текстуры пака (имя → PNG). Картинка с именем игровой заменяет её, пока выбран пак; прежние
     * текстуры пака освобождаются. Пустая карта — только игровые картинки.
     *
     * @throws IllegalArgumentException если PNG не читается
     */
    public void usePackTextures(Map<String, byte[]> pngs) {
        for (Texture t : packTextures.values()) {
            t.dispose();
        }
        packTextures.clear();
        regions.clear();
        frames.clear();
        for (Map.Entry<String, byte[]> png : pngs.entrySet()) {
            Pixmap pixmap;
            try {
                pixmap = new Pixmap(png.getValue(), 0, png.getValue().length);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "текстура '" + png.getKey() + "' не читается как PNG", e);
            }
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            packTextures.put(png.getKey(), texture);
        }
        if (atlas != null) {
            buildTiles();
        }
    }

    /** Есть ли картинка с таким именем (в игре или в паке). */
    public boolean has(String name) {
        return packTextures.containsKey(name) || atlas.findRegion(name) != null;
    }

    /** Имена всех игровых картинок атласа (для редактора паков). */
    public java.util.List<String> atlasNames() {
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (TextureAtlas.AtlasRegion r : atlas.getRegions()) {
            names.add(r.name);
        }
        return new java.util.ArrayList<>(names);
    }

    /** Кадр атласа целиком. */
    public TextureRegion region(String name) {
        return regions.computeIfAbsent(name, this::find);
    }

    /**
     * Кадры полосы шириной 16 (слева направо): полоса анимации {@code 96×16} даёт 6 кадров, двери
     * {@code 64×32} — 4. Остальные кадры — целиком.
     *
     * @throws IllegalArgumentException если такого кадра в атласе нет
     */
    public TextureRegion[] frames(String name) {
        return frames.computeIfAbsent(
                name,
                n -> {
                    TextureRegion region = region(n);
                    int width = region.getRegionWidth();
                    int height = region.getRegionHeight();
                    boolean strip = width % FRAME == 0 && (height == FRAME || height == FRAME * 2);
                    int count = strip ? width / FRAME : 1;
                    int frameWidth = strip ? FRAME : width;
                    TextureRegion[] result = new TextureRegion[count];
                    for (int i = 0; i < count; i++) {
                        result[i] =
                                new TextureRegion(region, i * frameWidth, 0, frameWidth, height);
                    }
                    return result;
                });
    }

    /** Кадр полосы с номером {@code index} (по модулю числа кадров). */
    public TextureRegion frame(String name, int index) {
        TextureRegion[] all = frames(name);
        return all[Math.floorMod(index, all.length)];
    }

    /** Кадр 16×16 тайлсета по колонке и строке. */
    public TextureRegion tile(int column, int row) {
        return tiles[row * TILESET_COLUMNS + column];
    }

    /** Шрифт игры высотой {@code pixels} пикселей экрана. */
    public BitmapFont font(int pixels) {
        return fonts.computeIfAbsent(pixels, this::generate);
    }

    private BitmapFont generate(int size) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = CHARS;
        parameter.mono = true;
        parameter.minFilter = Texture.TextureFilter.Nearest;
        parameter.magFilter = Texture.TextureFilter.Nearest;
        BitmapFont font = fontGenerator.generateFont(parameter);
        // В GFont нет кавычек-ёлочек, тире и многоточия: без замены вместо них — квадраты.
        for (int i = 0; i < SUBSTITUTES.length(); i += 2) {
            char missing = SUBSTITUTES.charAt(i);
            BitmapFont.Glyph similar = font.getData().getGlyph(SUBSTITUTES.charAt(i + 1));
            if (!fontGenerator.hasGlyph(missing) && similar != null) {
                font.getData().setGlyph(missing, similar);
            }
        }
        return font;
    }

    /**
     * Шрифт интерфейса высотой {@code logical} логических пикселей при масштабе {@code scale}:
     * генерируется в пикселях экрана (чёткий), а размеры отдаёт в логических.
     */
    public BitmapFont uiFont(int logical, int scale) {
        return uiFonts.computeIfAbsent(
                logical * 1000 + scale,
                key -> {
                    BitmapFont font = generate(logical * scale);
                    // У GFont большие межстрочный интервал и нижний вынос — сжимаем, иначе
                    // строки интерфейса почти вдвое выше кегля.
                    font.getData().setLineHeight(logical * scale * LINE_HEIGHT);
                    font.getData().descent = -logical * scale * DESCENT;
                    font.getData().setScale(1f / scale);
                    font.setUseIntegerPositions(false);
                    return font;
                });
    }

    /** Белый пиксель — целая текстура (координаты 0..1) для полноэкранных шейдеров. */
    public Texture whitePixel() {
        if (white == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            white = new Texture(pixmap);
            pixmap.dispose();
        }
        return white;
    }

    /** Шейдер темноты или {@code null}, если на устройстве он не собрался. */
    public ShaderProgram darkness() {
        if (!darknessTried) {
            darknessTried = true;
            darkness = Shaders.load("shaders/darkness.frag");
        }
        return darkness;
    }

    /** Освобождает шрифты (при смене масштаба экрана). */
    public void clearFonts() {
        fonts.values().forEach(BitmapFont::dispose);
        fonts.clear();
        uiFonts.values().forEach(BitmapFont::dispose);
        uiFonts.clear();
    }

    public void play(String sound, float volume) {
        manager.get(soundPath(sound), Sound.class).play(volume);
    }

    private TextureRegion find(String name) {
        Texture pack = packTextures.get(name);
        if (pack != null) {
            return new TextureRegion(pack);
        }
        TextureRegion region = atlas.findRegion(name);
        if (region == null) {
            if (name.equals(MISSING)) {
                throw new IllegalArgumentException("В атласе нет кадра '" + name + "'");
            }
            // Префаб пака ссылается на картинку, которой нет: значок «?» вместо падения.
            Gdx.app.error("DarGon", "Нет картинки '" + name + "' — рисуется значок «?»");
            return region(MISSING);
        }
        return region;
    }

    private static String soundPath(String name) {
        return "sounds/" + name + ".mp3";
    }

    @Override
    public void dispose() {
        clearFonts();
        for (Texture t : packTextures.values()) {
            t.dispose();
        }
        if (white != null) {
            white.dispose();
        }
        if (darkness != null) {
            darkness.dispose();
        }
        if (fontGenerator != null) {
            fontGenerator.dispose();
        }
        manager.dispose();
    }
}
