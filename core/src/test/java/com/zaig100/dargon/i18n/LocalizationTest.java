package com.zaig100.dargon.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LocalizationTest {

    private final Locale system = Locale.getDefault();

    @AfterEach
    void restore() {
        Locale.setDefault(system);
    }

    private static Localization texts(Locale preferred) {
        String assets = System.getProperty("dargon.assets", "../assets");
        return new Localization(path -> new FileHandle(new File(assets, path)), preferred);
    }

    @Test
    void switchesLanguageRegardlessOfSystemLanguage() {
        Locale.setDefault(Locale.forLanguageTag("ru"));
        Localization texts = texts(Locale.ENGLISH);
        assertEquals("en", texts.locale().getLanguage());
        assertEquals(
                "Shield", texts.ui("item.shield").substring(0, 6), "английский на русской системе");
        texts.setLocale(Locale.forLanguageTag("ru"));
        assertEquals("Щит", texts.ui("item.shield").substring(0, 3));

        Locale.setDefault(Locale.ENGLISH);
        texts.setLocale(Locale.forLanguageTag("ru"));
        assertEquals(
                "Щит", texts.ui("item.shield").substring(0, 3), "русский на английской системе");
    }
}
