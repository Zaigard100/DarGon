package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.grid;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.ui.FocusNavigator;
import com.zaig100.dargon.ui.Ui;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.ArrayList;
import java.util.List;

/**
 * Переназначение клавиш: выбрать действие и нажать новую клавишу (Esc — отмена). Новая клавиша
 * становится основной и снимается с других действий.
 */
final class ControlsScreen extends UiScreen {

    private static final Action[] ACTIONS = {
        Action.MOVE_UP,
        Action.MOVE_DOWN,
        Action.MOVE_LEFT,
        Action.MOVE_RIGHT,
        Action.USE,
        Action.INVENTORY,
        Action.DROP,
        Action.PAUSE
    };

    /** Действие, для которого ждём клавишу, или {@code null}. */
    private final Signal<Action> waiting = Signal.of(null);

    @Override
    protected UiNode build() {
        var texts = app.texts();
        List<UiNode> cells = new ArrayList<>();
        for (Action action : ACTIONS) {
            cells.add(text(texts.tr("action." + action.name())).style("tiny").align(Align.left));
            Ui.ButtonNode key =
                    button(
                                    text(app.settings()
                                                    .keys
                                                    .combine(
                                                            waiting,
                                                            (keys, w) ->
                                                                    w == action
                                                                            ? "..."
                                                                            : keyNames(
                                                                                    keys.get(
                                                                                            action))))
                                            .style("tiny"),
                                    () -> listen(action))
                            .align(Align.left);
            cells.add(action == ACTIONS[0] ? key.focused() : key);
        }
        return layers(
                        Widgets.panel(
                                column(
                                        2,
                                        text(texts.tr("controls.title")).style("button"),
                                        grid(4, 1, cells.toArray(new UiNode[0])),
                                        text(waiting.combine(
                                                        texts.localeSignal(),
                                                        (w, l) ->
                                                                w == null
                                                                        ? ""
                                                                        : texts.ui(
                                                                                "controls.press")))
                                                .style("hint"),
                                        button(
                                                texts.tr("controls.reset"),
                                                app.settings()::resetKeys),
                                        button(texts.tr("controls.back"), this::back))))
                .background("dim")
                .expand();
    }

    private void listen(Action action) {
        waiting.set(action);
        app.input()
                .captureNextKey(
                        keycode -> {
                            if (keycode != Input.Keys.ESCAPE) {
                                app.settings().bind(action, keycode);
                            }
                            waiting.set(null);
                        });
    }

    /** Основная клавиша действия (остальные — запасные, как WASD и стрелки). */
    private static String keyNames(List<Integer> codes) {
        if (codes == null || codes.isEmpty()) {
            return "—";
        }
        String name = Input.Keys.toString(codes.get(0));
        return codes.size() > 1 ? name + " +" + (codes.size() - 1) : name;
    }

    @Override
    public boolean onAction(Action action, FocusNavigator focus) {
        return action == Action.INVENTORY || super.onAction(action, focus);
    }
}
