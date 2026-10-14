package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.paragraph;
import static com.zaig100.dargon.ui.Ui.text;

import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;
import java.util.List;

/** Разработчик: последние сообщения журнала (ошибки скриптов и уровней, вывод print). */
final class DevLogScreen extends UiScreen {

    private static final int LINES = 9;
    private static final float WIDTH = 106;

    @Override
    protected UiNode build() {
        List<String> journal = app.journal();
        List<String> last = journal.subList(Math.max(0, journal.size() - LINES), journal.size());
        String body = last.isEmpty() ? app.texts().ui("dev.journalEmpty") : String.join("\n", last);
        return layers(
                        Widgets.panel(
                                column(
                                        2,
                                        text(app.texts().tr("dev.journal")).style("button"),
                                        paragraph(ReadSignal.constant(body))
                                                .style("tiny")
                                                .size(WIDTH, -1),
                                        button(
                                                        text(app.texts().tr("settings.back"))
                                                                .style("small"),
                                                        this::back)
                                                .focused())))
                .background("dim")
                .expand();
    }
}
