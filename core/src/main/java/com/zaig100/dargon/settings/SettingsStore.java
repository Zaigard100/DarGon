package com.zaig100.dargon.settings;

import com.badlogic.gdx.Preferences;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.GameData;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Хранит {@link Settings} в {@link Preferences} (на desktop — файл в домашнем каталоге, на Android
 * — SharedPreferences) и сохраняет при каждом изменении.
 */
public final class SettingsStore {

    private static final String VERSION = "version";
    private static final int CURRENT = 1;

    private final Preferences preferences;

    public SettingsStore(Preferences preferences) {
        this.preferences = preferences;
    }

    /** Читает сохранённые настройки и подписывается на изменения, чтобы сохранять их. */
    public Settings load() {
        Settings settings = new Settings();
        if (preferences.getInteger(VERSION, 0) == CURRENT) {
            settings.language.set(preferences.getString("language", ""));
            settings.pack.set(preferences.getString("pack", GameData.BUILTIN_PACK));
            settings.soundVolume.set(
                    Math.max(
                            0,
                            Math.min(
                                    Settings.MAX_VOLUME,
                                    preferences.getInteger("soundVolume", Settings.MAX_VOLUME))));
            settings.musicVolume.set(
                    clamp(preferences.getInteger("musicVolume", 6), 0, Settings.MAX_VOLUME));
            Map<Action, List<Integer>> keys = new EnumMap<>(Settings.defaultKeys());
            for (Action action : Action.values()) {
                String saved = preferences.getString("keys." + action.name(), null);
                if (saved != null) {
                    keys.put(action, parse(saved));
                }
            }
            settings.keys.set(keys);
            settings.touchControls.set(preferences.getString("touchControls", "auto"));
            settings.touchSize.set(clamp(preferences.getInteger("touchSize", 2), 1, 3));
            settings.touchOpacity.set(clamp(preferences.getInteger("touchOpacity", 7), 2, 10));
            settings.leftHanded.set(preferences.getBoolean("leftHanded", false));
            settings.vibration.set(preferences.getBoolean("vibration", true));
            settings.screenEffect.set(preferences.getString("screenEffect", "none"));
            settings.developer.set(preferences.getBoolean("developer", false));
        }
        settings.language.subscribe(v -> save(settings));
        settings.pack.subscribe(v -> save(settings));
        settings.soundVolume.subscribe(v -> save(settings));
        settings.musicVolume.subscribe(v -> save(settings));
        settings.keys.subscribe(v -> save(settings));
        settings.touchControls.subscribe(v -> save(settings));
        settings.touchSize.subscribe(v -> save(settings));
        settings.touchOpacity.subscribe(v -> save(settings));
        settings.leftHanded.subscribe(v -> save(settings));
        settings.vibration.subscribe(v -> save(settings));
        settings.screenEffect.subscribe(v -> save(settings));
        settings.developer.subscribe(v -> save(settings));
        return settings;
    }

    private void save(Settings settings) {
        preferences.putInteger(VERSION, CURRENT);
        preferences.putString("language", settings.language.get());
        preferences.putString("pack", settings.pack.get());
        preferences.putInteger("soundVolume", settings.soundVolume.get());
        preferences.putInteger("musicVolume", settings.musicVolume.get());
        for (Map.Entry<Action, List<Integer>> entry : settings.keys.get().entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (int code : entry.getValue()) {
                sb.append(sb.length() == 0 ? "" : ",").append(code);
            }
            preferences.putString("keys." + entry.getKey().name(), sb.toString());
        }
        preferences.putString("touchControls", settings.touchControls.get());
        preferences.putInteger("touchSize", settings.touchSize.get());
        preferences.putInteger("touchOpacity", settings.touchOpacity.get());
        preferences.putBoolean("leftHanded", settings.leftHanded.get());
        preferences.putBoolean("vibration", settings.vibration.get());
        preferences.putString("screenEffect", settings.screenEffect.get());
        preferences.putBoolean("developer", settings.developer.get());
        preferences.flush();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<Integer> parse(String saved) {
        List<Integer> codes = new ArrayList<>();
        for (String part : saved.split(",")) {
            if (!part.isBlank()) {
                try {
                    codes.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException e) {
                    // Испорченное значение — пропускаем, остальные клавиши остаются.
                }
            }
        }
        return List.copyOf(codes);
    }
}
