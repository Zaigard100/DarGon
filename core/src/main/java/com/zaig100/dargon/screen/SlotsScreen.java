package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.paragraph;
import static com.zaig100.dargon.ui.Ui.row;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.game.GameMode;
import com.zaig100.dargon.game.Progress;
import com.zaig100.dargon.i18n.Localization;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.save.SaveSlots;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.ArrayList;
import java.util.List;

/**
 * Слоты сохранений: для новой игры (занятый слот — с подтверждением) или для загрузки (и удаления с
 * подтверждением). Каждый слот показывает уровень, здоровье, монеты, время в игре и режим.
 */
final class SlotsScreen extends UiScreen {

    /** Зачем открыт экран. */
    enum Purpose {
        NEW_GAME,
        LOAD
    }

    private final Purpose purpose;

    /** Меняется после удаления слота — экран пересобирается. */
    private final Signal<Integer> version = Signal.of(0);

    SlotsScreen(Purpose purpose) {
        this.purpose = purpose;
    }

    @Override
    protected UiNode build() {
        var texts = app.texts();
        List<UiNode> rows = new ArrayList<>();
        for (int i = 0; i < SaveSlots.COUNT; i++) {
            int index = i;
            ReadSignal<String> title =
                    version.combine(texts.localeSignal(), (v, l) -> title(index));
            ReadSignal<String> details =
                    version.combine(texts.localeSignal(), (v, l) -> details(index));
            var slot =
                    button(
                                    column(
                                            0,
                                            text(title).style("small").align(Align.left),
                                            text(details).style("tiny").align(Align.left)),
                                    () -> choose(index))
                            .size(84, -1)
                            .enabled(
                                    version.map(
                                            v ->
                                                    purpose == Purpose.NEW_GAME
                                                            || slotHasGame(index)));
            if (i == 0) {
                slot.focused();
            }
            if (purpose == Purpose.LOAD) {
                rows.add(
                        row(
                                2,
                                slot,
                                button(text("x").style("small"), () -> delete(index))
                                        .enabled(
                                                version.map(
                                                        v -> !app.slots().slot(index).isEmpty()))));
            } else {
                rows.add(slot);
            }
        }
        List<UiNode> content = new ArrayList<>();
        content.add(
                text(texts.tr(purpose == Purpose.NEW_GAME ? "slots.newTitle" : "slots.loadTitle"))
                        .style("button"));
        content.addAll(rows);
        content.add(button(texts.tr("slots.back"), this::back));
        return layers(Widgets.panel(column(2, content.toArray(new UiNode[0]))))
                .background("dim")
                .expand();
    }

    private boolean slotHasGame(int index) {
        return app.slots().slot(index).progress() != null;
    }

    /** Краткое описание слота для главного меню. */
    static String summary(App app, int index) {
        SaveSlots.Slot slot = app.slots().slot(index);
        if (slot.progress() == null) {
            return "";
        }
        Progress.Snapshot at = slot.progress().resumeTarget();
        return app.texts()
                .ui(
                        "menu.continueHint",
                        app.texts().level(app.data().loadLevel(at.level()).name()),
                        at.player().hp,
                        at.player().coins);
    }

    private String title(int index) {
        Localization texts = app.texts();
        SaveSlots.Slot slot = app.slots().slot(index);
        if (slot.isBroken()) {
            return texts.ui("slots.broken", index + 1);
        }
        if (slot.isEmpty()) {
            return texts.ui("slots.empty", index + 1);
        }
        Progress p = slot.progress();
        Progress.Snapshot at = p.resumeTarget();
        LevelDefinition level = app.data().loadLevel(at.level());
        return texts.ui("slots.title", index + 1, texts.level(level.name()));
    }

    private String details(int index) {
        Localization texts = app.texts();
        SaveSlots.Slot slot = app.slots().slot(index);
        if (slot.isBroken()) {
            return slot.error();
        }
        if (slot.isEmpty()) {
            return "";
        }
        Progress p = slot.progress();
        Progress.Snapshot at = p.resumeTarget();
        long minutes = Math.round(p.playTime() / 60);
        return (p.cheats() ? "DEV " : "")
                + texts.ui(
                        "slots.details",
                        at.player().hp,
                        at.player().coins,
                        minutes / 60,
                        String.format("%02d", minutes % 60),
                        texts.ui("mode." + p.mode().id()));
    }

    private void choose(int index) {
        if (purpose == Purpose.LOAD) {
            app.load(index);
            return;
        }
        if (app.slots().slot(index).isEmpty()) {
            app.screens().push(new ModeScreen(index));
        } else {
            app.screens()
                    .push(
                            new ConfirmScreen(
                                    app.texts().tr("slots.overwrite", index + 1),
                                    () -> app.screens().push(new ModeScreen(index))));
        }
    }

    private void delete(int index) {
        app.screens()
                .push(
                        new ConfirmScreen(
                                app.texts().tr("slots.delete", index + 1),
                                () -> {
                                    app.slots().delete(index);
                                    version.set(version.get() + 1);
                                }));
    }

    /** Режим для новой игры. */
    static final class ModeScreen extends UiScreen {
        private static final float HINT_WIDTH = 90;

        private final int slot;

        ModeScreen(int slot) {
            this.slot = slot;
        }

        /** Если открыты отрезки — выбрать, откуда начать; иначе — с первого уровня. */
        private void chooseStart(GameMode mode) {
            if (app.unlockedStarts().isEmpty()) {
                app.newGame(slot, mode);
            } else {
                app.screens().push(new StartScreen(slot, mode));
            }
        }

        @Override
        protected UiNode build() {
            var texts = app.texts();
            return layers(
                            Widgets.panel(
                                    column(
                                            2,
                                            text(texts.tr("mode.title")).style("button"),
                                            button(
                                                    column(
                                                            0,
                                                            text(texts.tr("mode.casual"))
                                                                    .style("small"),
                                                            paragraph(texts.tr("mode.casual.hint"))
                                                                    .style("tiny")
                                                                    .size(HINT_WIDTH, -1)),
                                                    () -> chooseStart(GameMode.CASUAL)),
                                            button(
                                                            column(
                                                                    0,
                                                                    text(texts.tr("mode.normal"))
                                                                            .style("small"),
                                                                    text(texts.tr(
                                                                                    "mode.normal.hint"))
                                                                            .style("tiny")),
                                                            () ->
                                                                    app.newGame(
                                                                            slot, GameMode.NORMAL))
                                                    .focused(),
                                            button(
                                                    column(
                                                            0,
                                                            text(texts.tr("mode.hardcore"))
                                                                    .style("small"),
                                                            paragraph(
                                                                            texts.tr(
                                                                                    "mode.hardcore.hint"))
                                                                    .style("tiny")
                                                                    .size(HINT_WIDTH, -1)),
                                                    () -> chooseStart(GameMode.HARDCORE)),
                                            button(texts.tr("slots.back"), this::back))))
                    .background("dim")
                    .expand();
        }
    }

    /** Откуда начать: с первого уровня или с открытого достижениями отрезка. */
    static final class StartScreen extends UiScreen {
        private final int slot;
        private final GameMode mode;

        StartScreen(int slot, GameMode mode) {
            this.slot = slot;
            this.mode = mode;
        }

        @Override
        protected UiNode build() {
            var texts = app.texts();
            List<UiNode> buttons = new ArrayList<>();
            buttons.add(text(texts.tr("start.title")).style("button"));
            buttons.add(
                    button(texts.tr("start.beginning"), () -> app.newGame(slot, mode)).focused());
            for (String level : app.unlockedStarts()) {
                buttons.add(
                        button(
                                texts.tr("start.from", level),
                                () -> app.newGame(slot, mode, level)));
            }
            buttons.add(button(texts.tr("slots.back"), this::back));
            return layers(Widgets.panel(column(1, buttons.toArray(new UiNode[0]))))
                    .background("dim")
                    .expand();
        }
    }
}
