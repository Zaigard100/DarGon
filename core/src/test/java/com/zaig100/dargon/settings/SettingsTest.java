package com.zaig100.dargon.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.zaig100.dargon.input.Action;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsTest {

    /** Preferences в памяти: только методы, которые использует хранилище. */
    private static Preferences memory(Map<String, Object> values) {
        return (Preferences)
                Proxy.newProxyInstance(
                        Preferences.class.getClassLoader(),
                        new Class<?>[] {Preferences.class},
                        (proxy, method, args) -> {
                            String name = method.getName();
                            if (name.startsWith("put")) {
                                values.put((String) args[0], args[1]);
                                return proxy;
                            }
                            if (name.startsWith("get")) {
                                return values.getOrDefault(
                                        (String) args[0], args.length > 1 ? args[1] : null);
                            }
                            return null;
                        });
    }

    @Test
    void bindMovesKeyBetweenActionsAndMakesItPrimary() {
        Settings settings = new Settings();
        settings.bind(Action.USE, Input.Keys.W);
        assertEquals(
                List.of(Input.Keys.W, Input.Keys.E, Input.Keys.SPACE, Input.Keys.ENTER),
                settings.keys.get().get(Action.USE));
        assertEquals(
                List.of(Input.Keys.UP),
                settings.keys.get().get(Action.MOVE_UP),
                "клавиша снята с прежнего действия");
        settings.resetKeys();
        assertEquals(Settings.defaultKeys(), settings.keys.get());
    }

    @Test
    void storeSavesOnChangeAndLoadsBack() {
        Map<String, Object> values = new HashMap<>();
        Settings settings = new SettingsStore(memory(values)).load();
        settings.language.set("ru");
        settings.soundVolume.set(3);
        settings.bind(Action.DROP, Input.Keys.X);

        Settings loaded = new SettingsStore(memory(values)).load();
        assertEquals("ru", loaded.language.get());
        assertEquals(3, loaded.soundVolume.get());
        assertEquals(List.of(Input.Keys.X, Input.Keys.Q), loaded.keys.get().get(Action.DROP));
    }

    @Test
    void brokenValuesFallBackToDefaults() {
        Map<String, Object> values = new HashMap<>();
        values.put("version", 1);
        values.put("soundVolume", 99);
        values.put("keys.USE", "abc,33");
        Settings loaded = new SettingsStore(memory(values)).load();
        assertEquals(Settings.MAX_VOLUME, loaded.soundVolume.get());
        assertEquals(List.of(33), loaded.keys.get().get(Action.USE));
    }
}
