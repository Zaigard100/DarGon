package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.image;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.row;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Signal;

/** Главное меню: продолжить последнее прохождение, новая игра, загрузка, настройки. */
final class MainMenuScreen extends UiScreen {

    private static final float LOGO_WIDTH = 72;
    private static final float LOGO_HEIGHT = LOGO_WIDTH * 105 / 463;

    /** Сколько нажатий на версию открывает режим разработчика и за какое время между ними. */
    private static final int TAPS = 7;

    private static final long TAP_WINDOW_MS = 1500;

    /** Страница игры на GitHub: исходники, релизы, паки. */
    private static final String REPOSITORY = "https://github.com/Zaigard100/DarGon";

    private static final String REPOSITORY_NAME = "Zaigard100/DarGon";

    /** Цвет значка — как у текста ссылки (стиль «link»). */
    private static final ReadSignal<String> LINK_TINT = ReadSignal.constant("#8CB8FF");

    private final Signal<String> versionLabel = Signal.of("");
    private int taps;
    private long lastTap;

    @Override
    protected UiNode build() {
        var texts = app.texts();
        versionLabel.set(texts.ui("menu.version", app.version()));
        boolean canContinue = app.slots().last() >= 0;
        UiNode menu =
                column(
                        2,
                        image("logo").size(LOGO_WIDTH, LOGO_HEIGHT),
                        column(
                                1,
                                button(
                                                texts.tr("menu.continue"),
                                                () -> app.load(app.slots().last()))
                                        .enabled(ReadSignal.constant(canContinue))
                                        .focused(),
                                text(app.texts()
                                                .localeSignal()
                                                .map(
                                                        l ->
                                                                canContinue
                                                                        ? SlotsScreen.summary(
                                                                                app,
                                                                                app.slots().last())
                                                                        : ""))
                                        .style("tiny")
                                        .visible(ReadSignal.constant(canContinue)),
                                button(
                                        texts.tr("menu.new"),
                                        () ->
                                                app.screens()
                                                        .push(
                                                                new SlotsScreen(
                                                                        SlotsScreen.Purpose
                                                                                .NEW_GAME))),
                                button(
                                                texts.tr("menu.load"),
                                                () ->
                                                        app.screens()
                                                                .push(
                                                                        new SlotsScreen(
                                                                                SlotsScreen.Purpose
                                                                                        .LOAD)))
                                        .enabled(ReadSignal.constant(canContinue)),
                                button(
                                        text(texts.localeSignal()
                                                        .map(
                                                                l ->
                                                                        texts.ui(
                                                                                "menu.pack",
                                                                                texts.level(
                                                                                        app.data()
                                                                                                .pack()
                                                                                                .name()))))
                                                .style("small"),
                                        () -> app.screens().push(new PacksScreen())),
                                row(
                                        4,
                                        button(
                                                text(texts.tr("menu.achievements")).style("small"),
                                                () -> app.screens().push(new AchievementsScreen())),
                                        button(
                                                text(texts.tr("menu.stats")).style("small"),
                                                () -> app.screens().push(new StatsScreen())),
                                        button(
                                                text(texts.tr("menu.settings")).style("small"),
                                                () -> app.screens().push(new SettingsScreen())),
                                        button(
                                                        text(texts.tr("menu.developer"))
                                                                .style("small"),
                                                        () -> app.screens().push(new DevScreen()))
                                                .visible(app.settings().developer),
                                        button(
                                                        text(texts.tr("menu.exit")).style("small"),
                                                        app::exit)
                                                .visible(
                                                        ReadSignal.constant(
                                                                !app.platform()
                                                                        .isTouchPrimary())))),
                        text(""));
        return layers(
                menu,
                button(
                                row(
                                        2,
                                        image(ReadSignal.constant("github"), LINK_TINT).size(5, 5),
                                        text(REPOSITORY_NAME).style("link")),
                                () -> Gdx.net.openURI(REPOSITORY))
                        .pad(0, 1, 0, 1)
                        .align(Align.bottomLeft),
                button(text(versionLabel).style("tiny"), this::tapVersion)
                        .pad(0, 1, 0, 1)
                        .align(Align.bottomRight));
    }

    /** Нажатия на версию: семь подряд открывают режим разработчика. */
    private void tapVersion() {
        long now = System.currentTimeMillis();
        taps = now - lastTap > TAP_WINDOW_MS ? 1 : taps + 1;
        lastTap = now;
        var texts = app.texts();
        if (app.settings().developer.get()) {
            app.screens().push(new DevScreen());
            return;
        }
        if (taps >= TAPS) {
            app.settings().developer.set(true);
            versionLabel.set(texts.ui("dev.unlocked"));
        } else if (taps >= TAPS - 4) {
            versionLabel.set(texts.ui("dev.tapsLeft", TAPS - taps));
        }
    }

    @Override
    protected void back() {
        // Из главного меню назад некуда.
    }
}
