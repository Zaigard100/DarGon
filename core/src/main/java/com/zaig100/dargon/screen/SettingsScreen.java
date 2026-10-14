package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.row;
import static com.zaig100.dargon.ui.Ui.text;

import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.settings.Settings;
import com.zaig100.dargon.ui.FocusNavigator;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.List;

/** Настройки: язык (меняется сразу), громкость звуков, управление. */
final class SettingsScreen extends UiScreen {

    /** Порядок переключения языка: системный, русский, английский. */
    private static final List<String> LANGUAGES = List.of("", "ru", "en");

    @Override
    protected UiNode build() {
        var texts = app.texts();
        Settings settings = app.settings();
        return layers(
                        Widgets.panel(
                                column(
                                        1,
                                        text(texts.tr("settings.title")).style("button"),
                                        button(
                                                        settings.language.combine(
                                                                texts.localeSignal(),
                                                                (language, l) ->
                                                                        texts.ui(
                                                                                "settings.language",
                                                                                texts.ui(
                                                                                        "language."
                                                                                                + (language
                                                                                                                .isEmpty()
                                                                                                        ? "auto"
                                                                                                        : language)))),
                                                        this::nextLanguage)
                                                .focused(),
                                        button(
                                                settings.screenEffect.combine(
                                                        texts.localeSignal(),
                                                        (e, l) ->
                                                                texts.ui(
                                                                        "settings.effect",
                                                                        texts.ui("effect." + e))),
                                                this::nextEffect),
                                        volumeRow(settings.soundVolume, "settings.sound"),
                                        volumeRow(settings.musicVolume, "settings.music"),
                                        button(
                                                texts.tr("settings.touch"),
                                                () ->
                                                        app.screens()
                                                                .push(new TouchSettingsScreen())),
                                        button(
                                                texts.tr("settings.controls"),
                                                () -> app.screens().push(new ControlsScreen())),
                                        button(
                                                texts.tr("settings.reset"),
                                                () ->
                                                        app.screens()
                                                                .push(
                                                                        new ConfirmScreen(
                                                                                texts.tr(
                                                                                        "settings.resetConfirm"),
                                                                                app::resetAll))),
                                        button(texts.tr("settings.back"), this::back))))
                .background("dim")
                .expand();
    }

    private void nextLanguage() {
        int i = LANGUAGES.indexOf(app.settings().language.get());
        app.settings().language.set(LANGUAGES.get((i + 1) % LANGUAGES.size()));
    }

    private void nextEffect() {
        List<String> effects = List.of("none", "crt", "bloom");
        String current = app.settings().screenEffect.get();
        app.settings()
                .screenEffect
                .set(effects.get((effects.indexOf(current) + 1) % effects.size()));
    }

    /** Строка «− Звук: 7 +». */
    private UiNode volumeRow(Signal<Integer> volume, String key) {
        var texts = app.texts();
        return row(
                2,
                button("-", () -> change(volume, -1)),
                text(volume.combine(texts.localeSignal(), (v, l) -> texts.ui(key, v))),
                button("+", () -> change(volume, 1)));
    }

    private void change(Signal<Integer> volume, int delta) {
        volume.set(Math.max(0, Math.min(Settings.MAX_VOLUME, volume.get() + delta)));
        app.sound("click1");
    }

    @Override
    public boolean onAction(Action action, FocusNavigator focus) {
        return action == Action.INVENTORY || super.onAction(action, focus);
    }
}
