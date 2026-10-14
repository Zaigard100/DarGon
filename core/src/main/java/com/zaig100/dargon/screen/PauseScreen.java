package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.text;

import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.ui.FocusNavigator;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;

/** Пауза: продолжить, перезапустить уровень, настройки, главное меню, выход. */
final class PauseScreen extends UiScreen {

    @Override
    protected UiNode build() {
        var texts = app.texts();
        return layers(
                        Widgets.panel(
                                column(
                                        2,
                                        text(texts.tr("pause.title")).style("title"),
                                        button(texts.tr("pause.resume"), this::back).focused(),
                                        button(texts.tr("pause.restart"), app::restartLevel),
                                        button(
                                                texts.tr("pause.settings"),
                                                () -> app.screens().push(new SettingsScreen())),
                                        button(
                                                        texts.tr("menu.developer"),
                                                        () -> app.screens().push(new DevScreen()))
                                                .visible(app.settings().developer),
                                        button(texts.tr("pause.menu"), app::toMainMenu),
                                        button(texts.tr("pause.exit"), app::exit)
                                                .visible(
                                                        ReadSignal.constant(
                                                                !app.platform()
                                                                        .isTouchPrimary())))))
                .background("dim")
                .expand();
    }

    @Override
    public boolean onAction(Action action, FocusNavigator focus) {
        if (action == Action.INVENTORY) {
            return true;
        }
        return super.onAction(action, focus);
    }
}
