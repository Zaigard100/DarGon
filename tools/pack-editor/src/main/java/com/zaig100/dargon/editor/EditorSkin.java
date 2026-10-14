package com.zaig100.dargon.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.I18NBundle;
import com.kotcrab.vis.ui.Locales;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTextField;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Оформление редактора: скин VisUI, но со шрифтами Noto Sans (в стандартных нет кириллицы). Стиль
 * {@code mono} у полей ввода — для JSON и скриптов.
 */
final class EditorSkin {

    private static final String VIS = "com/kotcrab/vis/ui/skin/x1/";
    private static final String FONTS = "editor/fonts/";

    /** Буквы и знаки, которые бывают в текстах уровней и интерфейсе. */
    private static final String CHARS =
            FreeTypeFontGenerator.DEFAULT_CHARS
                    + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя"
                    + "«»—–−…№•·×°";

    private EditorSkin() {}

    /**
     * @param scale плотность экрана
     * @param ui масштаб интерфейса: шрифты рисуются в ui раз крупнее (вид сцены — тоже), чтобы
     *     буквы оставались чёткими
     */
    static void load(float scale, float ui) {
        BitmapFont font = font("NotoSans-Regular.ttf", 15 * scale, ui);
        BitmapFont small = font("NotoSans-Regular.ttf", 13 * scale, ui);
        BitmapFont mono = font("NotoSansMono-Regular.ttf", 14 * scale, ui);
        Skin skin = new Skin();
        skin.add("default-font", font, BitmapFont.class);
        skin.add("small-font", small, BitmapFont.class);
        skin.add("mono-font", mono, BitmapFont.class);
        skin.addRegions(new TextureAtlas(Gdx.files.classpath(VIS + "uiskin.atlas")));
        // Шрифты уже в скине — раздел шрифтов из uiskin.json убираем, остальное как есть.
        String json =
                Gdx.files
                        .classpath(VIS + "uiskin.json")
                        .readString("UTF-8")
                        .replaceFirst(
                                "(?s)com\\.badlogic\\.gdx\\.graphics\\.g2d\\.BitmapFont:\\s*\\{.*?\\n\\},\\s*",
                                "");
        skin.load(
                new FileHandle("uiskin.json") {
                    @Override
                    public InputStream read() {
                        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                    }
                });
        VisTextField.VisTextFieldStyle base = skin.get(VisTextField.VisTextFieldStyle.class);
        VisTextField.VisTextFieldStyle monoStyle = new VisTextField.VisTextFieldStyle(base);
        monoStyle.font = mono;
        skin.add("mono", monoStyle);
        // Границы панелей: полоса пошире, чтобы её было легко схватить мышью.
        for (boolean vertical : new boolean[] {false, true}) {
            String name = vertical ? "default-vertical" : "default-horizontal";
            VisSplitPane.VisSplitPaneStyle style =
                    new VisSplitPane.VisSplitPaneStyle(
                            skin.get(name, VisSplitPane.VisSplitPaneStyle.class));
            style.handle = wide(skin, style.handle, vertical);
            style.handleOver = wide(skin, style.handleOver, vertical);
            skin.add(vertical ? "wide-vertical" : "wide-horizontal", style);
        }
        VisUI.load(skin);
        // Тексты VisUI (выбор файла, кнопки диалогов) — по-русски.
        Locales.setFileChooserBundle(bundle("FileChooser"));
        Locales.setDialogsBundle(bundle("Dialogs"));
        Locales.setButtonBarBundle(bundle("ButtonBar"));
        Locales.setCommonBundle(bundle("Common"));
    }

    private static I18NBundle bundle(String name) {
        return I18NBundle.createBundle(
                Gdx.files.classpath("editor/i18n/" + name), Locale.ROOT, "UTF-8");
    }

    private static Drawable wide(Skin skin, Drawable d, boolean vertical) {
        Drawable copy = skin.newDrawable(d);
        if (vertical) {
            copy.setMinHeight(6);
        } else {
            copy.setMinWidth(6);
        }
        return copy;
    }

    private static BitmapFont font(String file, float logical, float ui) {
        BitmapFont font = font(file, Math.round(logical * ui));
        font.getData().setScale(1 / ui);
        font.setUseIntegerPositions(false);
        return font;
    }

    private static BitmapFont font(String file, int size) {
        FreeTypeFontGenerator generator =
                new FreeTypeFontGenerator(Gdx.files.classpath(FONTS + file));
        FreeTypeFontGenerator.FreeTypeFontParameter p =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size;
        p.characters = CHARS;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(p);
        generator.dispose();
        return font;
    }
}
