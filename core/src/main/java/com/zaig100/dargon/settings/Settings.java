package com.zaig100.dargon.settings;

import com.badlogic.gdx.Input;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Настройки игрока: язык, громкость звуков, раскладка клавиш. Каждое значение — сигнал: экраны и
 * сервисы подписываются и применяют изменения сразу. Хранение — {@link SettingsStore}.
 */
public final class Settings {

    /** Громкость звуков, 0..10. */
    public static final int MAX_VOLUME = 10;

    /** Выбранный пак (id); если он не загрузится — встроенный. */
    public final Signal<String> pack = Signal.of(GameData.BUILTIN_PACK);

    /** Язык ({@code "ru"}, {@code "en"}); пусто — язык системы. */
    public final Signal<String> language = Signal.of("");

    public final Signal<Integer> soundVolume = Signal.of(MAX_VOLUME);

    /** Громкость музыки, 0..10. */
    public final Signal<Integer> musicVolume = Signal.of(6);

    /** Сенсорное управление: {@code "auto"} (на телефоне), {@code "on"}, {@code "off"}. */
    public final Signal<String> touchControls = Signal.of("auto");

    /** Размер сенсорных кнопок, 1..3. */
    public final Signal<Integer> touchSize = Signal.of(2);

    /** Непрозрачность сенсорных кнопок, 2..10 (десятые доли). */
    public final Signal<Integer> touchOpacity = Signal.of(7);

    /** Зеркальная раскладка для левшей: крестовина справа, кнопка действия слева. */
    public final Signal<Boolean> leftHanded = Signal.of(false);

    /** Эффект экрана: {@code "none"}, {@code "crt"}, {@code "bloom"}. */
    public final Signal<String> screenEffect = Signal.of("none");

    /** Открыт режим разработчика (семь нажатий на версию в главном меню). */
    public final Signal<Boolean> developer = Signal.of(false);

    /** Вибрация при уроне. */
    public final Signal<Boolean> vibration = Signal.of(true);

    /** Раскладка: действие → клавиши. Меняется целиком (новая карта), чтобы сработал сигнал. */
    public final Signal<Map<Action, List<Integer>>> keys = Signal.of(defaultKeys());

    /** Раскладка по умолчанию: WASD и стрелки, E/пробел/Enter, Tab/I, Q, Esc, F3. */
    public static Map<Action, List<Integer>> defaultKeys() {
        Map<Action, List<Integer>> keys = new EnumMap<>(Action.class);
        keys.put(Action.MOVE_UP, List.of(Input.Keys.W, Input.Keys.UP));
        keys.put(Action.MOVE_DOWN, List.of(Input.Keys.S, Input.Keys.DOWN));
        keys.put(Action.MOVE_LEFT, List.of(Input.Keys.A, Input.Keys.LEFT));
        keys.put(Action.MOVE_RIGHT, List.of(Input.Keys.D, Input.Keys.RIGHT));
        keys.put(Action.USE, List.of(Input.Keys.E, Input.Keys.SPACE, Input.Keys.ENTER));
        keys.put(Action.INVENTORY, List.of(Input.Keys.TAB, Input.Keys.I));
        keys.put(Action.DROP, List.of(Input.Keys.Q));
        keys.put(Action.PAUSE, List.of(Input.Keys.ESCAPE, Input.Keys.BACK));
        keys.put(Action.DEBUG, List.of(Input.Keys.F3));
        return keys;
    }

    /**
     * Назначает клавишу основной для действия: она становится первой в списке действия и снимается
     * с других действий.
     */
    public void bind(Action action, int keycode) {
        Map<Action, List<Integer>> next = new EnumMap<>(Action.class);
        for (Map.Entry<Action, List<Integer>> entry : keys.get().entrySet()) {
            List<Integer> codes = new ArrayList<>(entry.getValue());
            codes.remove(Integer.valueOf(keycode));
            next.put(entry.getKey(), codes);
        }
        List<Integer> codes = new ArrayList<>(next.getOrDefault(action, List.of()));
        codes.add(0, keycode);
        next.put(action, List.copyOf(codes));
        next.replaceAll((a, c) -> List.copyOf(c));
        keys.set(next);
    }

    public void resetKeys() {
        keys.set(defaultKeys());
    }
}
