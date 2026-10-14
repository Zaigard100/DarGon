package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.custom;
import static com.zaig100.dargon.ui.Ui.layers;

import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.ui.UiNode;

/**
 * Сенсорное управление: крестовина слева, кнопка действия справа (у левшей — наоборот). На кнопке —
 * значок того, что произойдёт; если взаимодействовать не с чем, она полупрозрачная. Сумка и пауза —
 * на панели HUD. Тап по клетке поля работает как клик мышью.
 */
final class TouchScreen extends UiScreen {

    /** Размер крестовины для размеров 1..3, логические пиксели. */
    private static final float[] DPAD = {26, 33, 40};

    private static final float[] BUTTON = {18, 22, 26};
    private static final float MARGIN = 3;

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
        var settings = app.settings();
        int size = settings.touchSize.get() - 1;
        boolean left = settings.leftHanded.get();
        float alpha = settings.touchOpacity.get() / 10f;
        UiNode dpad =
                custom(
                                () -> {
                                    DPad pad = new DPad(app.assets(), app.input(), DPAD[size]);
                                    pad.getColor().a = alpha;
                                    return pad;
                                })
                        .size(DPAD[size], DPAD[size])
                        .align(left ? Align.bottomRight : Align.bottomLeft)
                        .pad(0, MARGIN, MARGIN, MARGIN);
        UiNode use =
                custom(
                                () ->
                                        new ActionButton(
                                                app.assets(),
                                                app.input(),
                                                app.gameplay().hud(),
                                                BUTTON[size],
                                                alpha))
                        .size(BUTTON[size], BUTTON[size])
                        .align(left ? Align.bottomLeft : Align.bottomRight)
                        .pad(0, MARGIN, MARGIN, MARGIN);
        return layers(dpad, use).visible(app.touchControls());
    }
}
