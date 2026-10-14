package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.grid;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.paragraph;
import static com.zaig100.dargon.ui.Ui.text;

import com.zaig100.dargon.game.Cheats;
import com.zaig100.dargon.i18n.Localization;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Режим разработчика: инструменты для отладки и тестирования. В игре — бессмертие, сквозь стены,
 * показать скрытое, без темноты, ускорение, монеты и предметы, пройти и перезагрузить уровень.
 * Везде — выбор уровня, отладочная строка, журнал. Использование меняющих игру инструментов
 * помечает прохождение: достижения и статистика для него выключаются.
 */
final class DevScreen extends UiScreen {

    private static final float WIDTH = 104;

    /** Растёт после каждого действия — подписи переключателей пересчитываются. */
    private final Signal<Integer> version = Signal.of(0);

    @Override
    protected UiNode build() {
        Localization texts = app.texts();
        List<UiNode> buttons = new ArrayList<>();
        boolean inGame = app.gameplay() != null;
        if (inGame) {
            DevTools dev = app.dev();
            buttons.add(
                    toggle(
                            "dev.immortal",
                            c -> c.immortal,
                            () -> dev.toggle(() -> dev.cheats().immortal ^= true)));
            buttons.add(
                    toggle(
                            "dev.noclip",
                            c -> c.noclip,
                            () -> dev.toggle(() -> dev.cheats().noclip ^= true)));
            buttons.add(
                    toggle(
                            "dev.reveal",
                            c -> c.revealAll,
                            () -> dev.toggle(() -> dev.cheats().revealAll ^= true)));
            buttons.add(
                    toggle(
                            "dev.light",
                            c -> c.noDarkness,
                            () -> dev.toggle(() -> dev.cheats().noDarkness ^= true)));
            buttons.add(
                    small(
                            label(
                                    () ->
                                            texts.ui(
                                                    "dev.speed",
                                                    app.gameplay().session().cheats().timeScale)),
                            () -> act(dev::nextTimeScale)));
            buttons.add(small(texts.tr("dev.coins"), () -> act(dev::giveCoins)));
            buttons.add(small(texts.tr("dev.items"), () -> act(dev::giveItems)));
            buttons.add(small(texts.tr("dev.complete"), dev::completeLevel));
            buttons.add(small(texts.tr("dev.reload"), dev::reloadLevel));
        }
        buttons.add(
                small(texts.tr("dev.levels"), () -> app.screens().push(new LevelSelectScreen())));
        buttons.add(
                small(
                        label(
                                () ->
                                        texts.ui(
                                                "dev.debug",
                                                texts.ui(app.debug() ? "touch.on" : "touch.off"))),
                        () -> act(app::toggleDebug)));
        buttons.add(small(texts.tr("dev.journal"), () -> app.screens().push(new DevLogScreen())));
        buttons.add(small(texts.tr("dev.disable"), this::disable));
        buttons.add(small(texts.tr("settings.back"), this::back));
        ((com.zaig100.dargon.ui.Ui.ButtonNode) buttons.get(0)).focused();
        List<UiNode> content = new ArrayList<>();
        content.add(text(texts.tr("dev.title")).style("button"));
        content.add(grid(2, 1, buttons.toArray(new UiNode[0])));
        if (inGame) {
            content.add(
                    paragraph(
                                    version.combine(
                                            texts.localeSignal(),
                                            (v, l) ->
                                                    app.gameplay().session().progress().cheats()
                                                            ? texts.ui("dev.marked")
                                                            : texts.ui("dev.warning")))
                            .style("tiny")
                            .size(WIDTH, -1));
        }
        return layers(Widgets.panel(column(1, content.toArray(new UiNode[0]))))
                .background("dim")
                .expand();
    }

    private UiNode toggle(String key, Function<Cheats, Boolean> state, Runnable change) {
        Localization texts = app.texts();
        return small(
                label(
                        () ->
                                texts.ui(
                                        key,
                                        texts.ui(
                                                state.apply(app.gameplay().session().cheats())
                                                        ? "touch.on"
                                                        : "touch.off"))),
                () -> act(change));
    }

    private ReadSignal<String> label(java.util.function.Supplier<String> text) {
        return version.combine(app.texts().localeSignal(), (v, l) -> text.get());
    }

    private static com.zaig100.dargon.ui.Ui.ButtonNode small(
            ReadSignal<String> label, Runnable action) {
        return button(text(label).style("small"), action).pad(0, 1, 0, 1);
    }

    private void act(Runnable action) {
        action.run();
        version.set(version.get() + 1);
    }

    private void disable() {
        app.settings().developer.set(false);
        back();
    }
}
