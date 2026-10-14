package com.zaig100.dg.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class Font {
    public Font() {

    }

    static FreeTypeFontGenerator g;

    public static BitmapFont gFont(float s, String path) {
        FreeTypeFontGenerator.FreeTypeFontParameter pr = new FreeTypeFontGenerator.FreeTypeFontParameter();
        g = new FreeTypeFontGenerator(Gdx.files.internal(path));
        pr.size = Math.round(s);
        BitmapFont font = g.generateFont(pr);
        font.setColor(Color.WHITE);
        return font;
    }

    public static void dispose() {
        g.dispose();
    }


}
