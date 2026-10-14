package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.text;

import com.zaig100.dargon.game.Progress;
import com.zaig100.dargon.ui.UiNode;

/**
 * Смерть: «Заново» — текущий уровень (с тем, с чем на него вошли) или, после третьей смерти подряд
 * и в хардкоре, контрольная точка. Решение уже сохранено — выход в меню его не отменяет. Мир под
 * экраном не стоит.
 */
final class WastedScreen extends UiScreen {

    private final Progress.Death death;
    private final int attemptsLeft;

    WastedScreen(Progress.Death death, int attemptsLeft) {
        this.death = death;
        this.attemptsLeft = attemptsLeft;
    }

    @Override
    public boolean pausesGame() {
        return false;
    }

    @Override
    protected UiNode build() {
        var texts = app.texts();
        String hint =
                death == Progress.Death.ROLLBACK
                        ? "wasted.rollback"
                        : attemptsLeft == Integer.MAX_VALUE
                                ? "wasted.casual"
                                : attemptsLeft == 1 ? "wasted.lastAttempt" : "wasted.attempts";
        return column(
                3,
                text(texts.tr("wasted.title")).style("title"),
                text(texts.tr(hint, attemptsLeft)).style("small"),
                Widgets.panel(
                        column(
                                1,
                                button(
                                                texts.tr(
                                                        death == Progress.Death.ROLLBACK
                                                                ? "wasted.toCheckpoint"
                                                                : "wasted.retry"),
                                                app::afterDeath)
                                        .focused(),
                                button(texts.tr("wasted.menu"), app::toMainMenu))));
    }

    @Override
    protected void back() {
        app.afterDeath();
    }
}
