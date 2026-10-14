package com.zaig100.dargon.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.ItemDefinition;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Все ключи интерфейса из кода есть в ui.properties на всех языках. */
class UiTextsTest {

    private static final Pattern KEY = Pattern.compile("\\b(?:tr|ui)\\(\\s*\"([A-Za-z0-9_.]+)\"");

    @Test
    void everyKeyUsedInCodeIsTranslated() throws IOException {
        Set<String> keys = new TreeSet<>();
        Path sources = Path.of("src/main/java");
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = KEY.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    keys.add(m.group(1));
                }
            }
        }
        // Составные ключи: "item." + id, "action." + действие, "language." + язык.
        keys.removeIf(k -> k.endsWith("."));
        for (ItemDefinition item : TestGame.data().items().all()) {
            keys.add("item." + item.id());
        }
        for (Action action : Action.values()) {
            if (action != Action.DEBUG) {
                keys.add("action." + action.name());
            }
        }
        keys.addAll(List.of("language.auto", "language.ru", "language.en"));
        Path assets = Path.of(System.getProperty("dargon.assets", "../assets"));
        for (String file : List.of("ui.properties", "ui_ru.properties")) {
            Properties texts = new Properties();
            try (Reader reader =
                    Files.newBufferedReader(
                            assets.resolve("i18n").resolve(file), StandardCharsets.UTF_8)) {
                texts.load(reader);
            }
            List<String> missing = new ArrayList<>();
            for (String key : keys) {
                if (!texts.containsKey(key)) {
                    missing.add(key);
                }
            }
            assertEquals(List.of(), missing, file);
        }
    }
}
