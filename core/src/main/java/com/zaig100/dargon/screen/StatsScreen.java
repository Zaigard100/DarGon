package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.game.PlayStats;
import com.zaig100.dargon.i18n.Localization;
import com.zaig100.dargon.ui.UiNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Статистика: всего смертей и времени, самые смертоносные уровни и от чего там гибнут. */
final class StatsScreen extends UiScreen {

    private static final int TOP = 5;

    @Override
    protected UiNode build() {
        Localization texts = app.texts();
        PlayStats stats = app.stats();
        List<UiNode> lines = new ArrayList<>();
        lines.add(text(texts.tr("stats.title")).style("button"));
        long minutes = Math.round(stats.totalTime() / 60);
        lines.add(
                text(texts.localeSignal()
                                .map(
                                        l ->
                                                texts.ui(
                                                        "stats.total",
                                                        stats.totalDeaths(),
                                                        minutes / 60,
                                                        String.format("%02d", minutes % 60))))
                        .style("small"));
        List<PlayStats.Level> deadly = new ArrayList<>(stats.levels());
        deadly.removeIf(l -> l.totalDeaths() == 0);
        deadly.sort(Comparator.comparingInt(PlayStats.Level::totalDeaths).reversed());
        if (deadly.isEmpty()) {
            lines.add(text(texts.tr("stats.noDeaths")).style("hint"));
        }
        for (PlayStats.Level level : deadly.subList(0, Math.min(TOP, deadly.size()))) {
            lines.add(
                    text(texts.localeSignal().map(l -> describe(texts, level)))
                            .style("tiny")
                            .align(Align.left));
        }
        lines.add(button(texts.tr("slots.back"), this::back).focused());
        return layers(Widgets.panel(column(2, lines.toArray(new UiNode[0]))))
                .background("dim")
                .expand();
    }

    private String describe(Localization texts, PlayStats.Level level) {
        StringBuilder causes = new StringBuilder();
        for (Map.Entry<String, Integer> cause : level.deaths.entrySet()) {
            causes.append(causes.length() == 0 ? "" : ", ")
                    .append(texts.ui("cause." + cause.getKey()))
                    .append(' ')
                    .append(cause.getValue());
        }
        String name;
        try {
            name = texts.level(app.data().loadLevel(level.id).name());
        } catch (RuntimeException e) {
            name = level.id;
        }
        return texts.ui("stats.level", name, level.totalDeaths(), causes);
    }
}
