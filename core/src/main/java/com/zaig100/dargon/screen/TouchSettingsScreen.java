package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.row;
import static com.zaig100.dargon.ui.Ui.text;

import com.zaig100.dargon.i18n.Localization;
import com.zaig100.dargon.settings.Settings;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.List;

/**
 * Сенсорное управление: включение, размер и прозрачность кнопок, раскладка для левшей, вибрация.
 */
final class TouchSettingsScreen extends UiScreen {

    private static final List<String> MODES = List.of("auto", "on", "off");

    @Override
    protected UiNode build() {
        Localization texts = app.texts();
        Settings s = app.settings();
        return layers(
                        Widgets.panel(
                                column(
                                        2,
                                        text(texts.tr("touch.title")).style("button"),
                                        button(
                                                        label(
                                                                "touch.mode",
                                                                s.touchControls.map(
                                                                        m ->
                                                                                texts.ui(
                                                                                        "touch.mode."
                                                                                                + m))),
                                                        () ->
                                                                s.touchControls.set(
                                                                        next(
                                                                                s.touchControls
                                                                                        .get())))
                                                .focused(),
                                        button(
                                                label(
                                                        "touch.size",
                                                        s.touchSize.map(String::valueOf)),
                                                () -> s.touchSize.set(s.touchSize.get() % 3 + 1)),
                                        row(
                                                2,
                                                button("-", () -> opacity(-1)),
                                                text(
                                                        label(
                                                                "touch.opacity",
                                                                s.touchOpacity.map(
                                                                        v -> v * 10 + "%"))),
                                                button("+", () -> opacity(1))),
                                        button(
                                                label("touch.leftHanded", yesNo(s.leftHanded)),
                                                () -> s.leftHanded.set(!s.leftHanded.get())),
                                        button(
                                                label("touch.vibration", yesNo(s.vibration)),
                                                () -> s.vibration.set(!s.vibration.get())),
                                        button(texts.tr("settings.back"), this::back))))
                .background("dim")
                .expand();
    }

    /** Подпись «Название: значение», меняется со значением и языком. */
    private ReadSignal<String> label(String key, ReadSignal<String> value) {
        Localization texts = app.texts();
        return value.combine(texts.localeSignal(), (v, l) -> texts.ui(key, v));
    }

    private ReadSignal<String> yesNo(Signal<Boolean> flag) {
        Localization texts = app.texts();
        return flag.combine(texts.localeSignal(), (v, l) -> texts.ui(v ? "touch.on" : "touch.off"));
    }

    private static String next(String mode) {
        return MODES.get((MODES.indexOf(mode) + 1) % MODES.size());
    }

    private void opacity(int delta) {
        Signal<Integer> o = app.settings().touchOpacity;
        o.set(Math.max(2, Math.min(10, o.get() + delta)));
    }

    @Override
    protected void closed() {
        // Размер и раскладка применяются пересборкой экранов.
        app.screens().rebuild();
    }
}
