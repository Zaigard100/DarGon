package com.zaig100.dargon.i18n;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Signal;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.function.Function;

/**
 * Тексты игры на текущем языке: интерфейс ({@code i18n/ui*.properties} игры) и пак ({@code
 * i18n/pack*.properties} внутри пака: названия уровней, таблички, достижения). Строка, начинающаяся
 * с {@code @}, — ключ из текстов пака; остальные выводятся как есть.
 */
public final class Localization {

    /** Поддерживаемые языки; первый — запасной. */
    public static final Locale[] LOCALES = {Locale.ENGLISH, Locale.forLanguageTag("ru")};

    private final Function<String, FileHandle> files;
    private final Signal<Locale> locale = Signal.of(LOCALES[0]);
    private I18NBundle ui;
    private AssetReader pack = path -> null;
    private Map<String, String> packTexts = Map.of();

    public Localization(Function<String, FileHandle> files, Locale preferred) {
        this.files = files;
        setLocale(preferred);
    }

    /** Переключает язык; неподдерживаемый язык заменяется английским. */
    public void setLocale(Locale preferred) {
        Locale chosen = LOCALES[0];
        for (Locale supported : LOCALES) {
            if (supported.getLanguage().equals(preferred.getLanguage())) {
                chosen = supported;
            }
        }
        I18NBundle.setExceptionOnMissingKey(true);
        // Запасной язык лежит в базовых файлах (ui.properties). Просить его как ROOT: на «en»
        // I18NBundle, не найдя ui_en, ушёл бы к языку системы раньше базового файла.
        Locale bundle = chosen == LOCALES[0] ? Locale.ROOT : chosen;
        ui = I18NBundle.createBundle(files.apply("i18n/ui"), bundle, "UTF-8");
        packTexts = loadPack(pack, chosen);
        locale.set(chosen);
    }

    /** Тексты пака: файлы {@code i18n/pack.properties} и {@code i18n/pack_<язык>.properties}. */
    public void setPack(AssetReader pack) {
        this.pack = pack;
        packTexts = loadPack(pack, locale.get());
        locale.touch();
    }

    /**
     * Текст другого пака (название и описание в списке паков): ключ {@code @…} ищется в его
     * собственных текстах на текущем языке.
     */
    public String packText(AssetReader otherPack, String text) {
        if (text == null || !text.startsWith("@")) {
            return text;
        }
        String value = loadPack(otherPack, locale.get()).get(text.substring(1));
        return value == null ? text.substring(1) : value;
    }

    private static Map<String, String> loadPack(AssetReader pack, Locale chosen) {
        Map<String, String> texts = new HashMap<>();
        read(pack.read("i18n/pack.properties"), texts);
        if (chosen != LOCALES[0]) {
            read(pack.read("i18n/pack_" + chosen.getLanguage() + ".properties"), texts);
        }
        return texts;
    }

    private static void read(String source, Map<String, String> into) {
        if (source == null) {
            return;
        }
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(source));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String key : properties.stringPropertyNames()) {
            into.put(key, properties.getProperty(key));
        }
    }

    public Locale locale() {
        return locale.get();
    }

    /** Текущий язык как сигнал: интерфейс переключается без перезапуска. */
    public ReadSignal<Locale> localeSignal() {
        return locale;
    }

    /** Строка интерфейса, которая сама меняется при смене языка. */
    public ReadSignal<String> tr(String key, Object... args) {
        return locale.map(l -> ui(key, args));
    }

    /** Текст уровня ({@code "@level.05.name"}), меняющийся при смене языка. */
    public ReadSignal<String> trLevel(String text) {
        return locale.map(l -> level(text));
    }

    /** Строка интерфейса по ключу, с подстановкой {@code {0}}, {@code {1}}… */
    public String ui(String key, Object... args) {
        return get(ui, key, args);
    }

    /** Текст из уровня: {@code "@level.05.name"} — по ключу, иначе как есть. */
    public String level(String text) {
        if (text == null || !text.startsWith("@")) {
            return text;
        }
        String value = packTexts.get(text.substring(1));
        return value == null ? "???" + text.substring(1) + "???" : value;
    }

    private static String get(I18NBundle bundle, String key, Object... args) {
        try {
            return args.length == 0 ? bundle.get(key) : bundle.format(key, args);
        } catch (MissingResourceException e) {
            return "???" + key + "???";
        }
    }
}
