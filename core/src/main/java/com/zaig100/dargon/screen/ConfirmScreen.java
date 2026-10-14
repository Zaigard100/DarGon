package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.paragraph;
import static com.zaig100.dargon.ui.Ui.row;

import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;

/** Вопрос «Да / Нет». По умолчанию фокус на «Нет». */
final class ConfirmScreen extends UiScreen {

    private static final float WIDTH = 96;

    private final ReadSignal<String> question;
    private final Runnable yes;

    ConfirmScreen(ReadSignal<String> question, Runnable yes) {
        this.question = question;
        this.yes = yes;
    }

    @Override
    protected UiNode build() {
        var texts = app.texts();
        return layers(
                        Widgets.panel(
                                column(
                                        3,
                                        paragraph(question).style("small").size(WIDTH, -1),
                                        row(
                                                6,
                                                button(texts.tr("confirm.yes"), this::accept),
                                                button(texts.tr("confirm.no"), this::back)
                                                        .focused()))))
                .background("dim")
                .expand();
    }

    private void accept() {
        back();
        yes.run();
    }
}
