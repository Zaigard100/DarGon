package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.text;

import com.zaig100.dargon.ui.UiNode;

/** Игра пройдена. */
final class EndScreen extends UiScreen {

    @Override
    protected UiNode build() {
        var texts = app.texts();
        return layers(
                        column(
                                3,
                                text(texts.tr("end.title")).style("title"),
                                text(texts.tr("end.text")).style("small"),
                                button(texts.tr("end.menu"), app::toMainMenu).focused()))
                .background("dim")
                .expand();
    }

    @Override
    protected void back() {
        app.toMainMenu();
    }
}
