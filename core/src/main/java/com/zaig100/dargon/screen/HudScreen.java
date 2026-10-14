package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.image;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.row;
import static com.zaig100.dargon.ui.Ui.spacer;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.Signal;

/**
 * HUD поверх игрового окна: справа — здоровье, монеты, сумка и пауза (как меню DarGon3), слева
 * сверху — название уровня до первого шага, текст табличек. Не забирает клавиши у игры.
 */
final class HudScreen extends UiScreen {

    private final Signal<String> debugText = Signal.of("");

    @Override
    public Area area() {
        return Area.GAME;
    }

    @Override
    public boolean pausesGame() {
        return false;
    }

    @Override
    public boolean modal() {
        return false;
    }

    @Override
    protected UiNode build() {
        HudModel hud = app.gameplay().hud();
        UiNode panel =
                column(
                                1,
                                // Сердца — полоса здоровья (каждое ≈ 10 HP), рядом точное число.
                                image(hud.hp.map(HudScreen::hearts)).size(16, 16),
                                text(hud.hp.map(String::valueOf)).style("tiny"),
                                row(
                                                1,
                                                image("sheld").size(6, 6),
                                                text(hud.shield.map(String::valueOf)).style("tiny"))
                                        .visible(hud.shield.map(s -> s > 0)),
                                row(
                                        1,
                                        image("money").size(6, 6),
                                        text(hud.coins.map(String::valueOf)).style("tiny")),
                                spacer(),
                                button(
                                                image("bag").size(16, 16),
                                                () -> app.screens().push(new InventoryScreen()))
                                        .pad(0),
                                button(
                                                image("pause").size(12, 12),
                                                () -> app.screens().push(new PauseScreen()))
                                        .pad(0))
                        .align(Align.topRight)
                        .expand()
                        .pad(1, 0, 1, 1);
        UiNode name =
                text(hud.levelName.combine(
                                app.texts().localeSignal(), (n, l) -> app.texts().level(n)))
                        .visible(hud.showLevelName)
                        .align(Align.topLeft)
                        .pad(4, 6, 0, 0);
        UiNode message =
                layers(
                                text(hud.message.combine(
                                                app.texts().localeSignal(),
                                                (lines, l) -> {
                                                    StringBuilder sb = new StringBuilder();
                                                    for (String line : lines) {
                                                        sb.append(sb.length() == 0 ? "" : "\n")
                                                                .append(app.texts().level(line));
                                                    }
                                                    return sb.toString();
                                                }))
                                        .style("sign"))
                        .background("sign")
                        .pad(3, 4, 2, 4)
                        .visible(hud.message.map(lines -> !lines.isEmpty()))
                        .align(Align.left)
                        .pad(0, 3, 0, 0);
        UiNode dev =
                text("DEV").style("tiny").visible(hud.cheats).align(Align.topLeft).pad(12, 6, 0, 0);
        UiNode debug =
                text(debugText)
                        .style("tiny")
                        .align(Align.bottomLeft)
                        .visible(debugText.map(t -> !t.isEmpty()));
        UiNode toast =
                layers(text(app.toast()).style("small"))
                        .background("panel")
                        .pad(1, 3, 1, 3)
                        .visible(app.toast().map(t -> !t.isEmpty()))
                        .align(Align.top)
                        .pad(18, 0, 0, 0);
        return layers(name, message, panel, dev, debug, toast);
    }

    /** Кадр сердец по здоровью: 1–4 сердца, каждое ≈ четверть максимума. */
    static String hearts(int hp) {
        if (hp <= 0) {
            return null;
        }
        int quarter = PlayerState.MAX_HP / 4;
        return "hp_" + Math.min(4, (hp + quarter - 1) / quarter);
    }

    @Override
    public void update(float delta) {
        if (!app.debug() || app.gameplay() == null) {
            debugText.set("");
            return;
        }
        GridPos at = app.gameplay().level().playerPos();
        debugText.set(
                Gdx.graphics.getFramesPerSecond()
                        + " FPS  "
                        + app.gameplay().level().state().definition().id()
                        + " ["
                        + at.x
                        + ", "
                        + at.y
                        + "]  "
                        + app.gameplay().level().world().entityCount()
                        + " ent");
    }
}
