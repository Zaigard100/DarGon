package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.paragraph;
import static com.zaig100.dargon.ui.Ui.row;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.i18n.Localization;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.PackDefinition;
import com.zaig100.dargon.ui.FocusNavigator;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.ArrayList;
import java.util.List;

/**
 * Достижения пака: по страницам, полученные — ярко, остальные — бледно; скрытые до получения
 * показываются как «???». Влево/вправо — страницы.
 */
final class AchievementsScreen extends UiScreen {

    private static final int PER_PAGE = 6;
    private static final float WIDTH = 104;

    private final Signal<Integer> page = Signal.of(0);

    @Override
    protected UiNode build() {
        Localization texts = app.texts();
        List<PackDefinition.Achievement> all = app.data().pack().achievements();
        int pages = (all.size() + PER_PAGE - 1) / PER_PAGE;
        int got = 0;
        for (PackDefinition.Achievement a : all) {
            got += app.achievementStore().has(a.id()) ? 1 : 0;
        }
        int unlocked = got;
        List<UiNode> lines = new ArrayList<>();
        lines.add(
                text(texts.localeSignal()
                                .map(l -> texts.ui("achievements.title", unlocked, all.size())))
                        .style("button"));
        for (int slot = 0; slot < PER_PAGE; slot++) {
            int index = slot;
            ReadSignal<String> line =
                    page.combine(
                            texts.localeSignal(),
                            (p, l) -> {
                                int i = p * PER_PAGE + index;
                                return i < all.size() ? describe(texts, all.get(i)) : "";
                            });
            ReadSignal<String> style =
                    page.map(
                            p -> {
                                int i = p * PER_PAGE + index;
                                return i < all.size() && app.achievementStore().has(all.get(i).id())
                                        ? "got"
                                        : "no";
                            });
            lines.add(
                    paragraph(line)
                            .style("tiny")
                            .size(WIDTH, -1)
                            .align(Align.left)
                            .visible(style.map(s -> s.equals("got"))));
            lines.add(
                    paragraph(line)
                            .style("hint")
                            .size(WIDTH, -1)
                            .align(Align.left)
                            .visible(style.map(s -> s.equals("no"))));
        }
        lines.add(
                row(
                        4,
                        button(text("<").style("small"), () -> turn(-1, pages)),
                        text(page.map(p -> (p + 1) + "/" + pages)).style("tiny"),
                        button(text(">").style("small"), () -> turn(1, pages)),
                        button(text(texts.tr("settings.back")).style("small"), this::back)
                                .focused()));
        return layers(Widgets.panel(column(1, lines.toArray(new UiNode[0]))))
                .background("dim")
                .expand();
    }

    private String describe(Localization texts, PackDefinition.Achievement a) {
        boolean has = app.achievementStore().has(a.id());
        String mark = has ? "+ " : "- ";
        String difficulty = texts.ui("difficulty." + a.difficulty());
        if (a.hidden() && !has) {
            return mark + "??? (" + difficulty + ")";
        }
        return mark
                + texts.level(a.name())
                + " ("
                + difficulty
                + "): "
                + texts.level(a.description());
    }

    private void turn(int delta, int pages) {
        page.set(Math.floorMod(page.get() + delta, pages));
    }

    @Override
    public boolean onAction(Action action, FocusNavigator focus) {
        if (action == Action.MOVE_LEFT || action == Action.MOVE_RIGHT) {
            int pages = (app.data().pack().achievements().size() + PER_PAGE - 1) / PER_PAGE;
            turn(action == Action.MOVE_LEFT ? -1 : 1, pages);
            return true;
        }
        return super.onAction(action, focus);
    }
}
