package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** С сущностью можно взаимодействовать действием «использовать». */
public final class Interactable implements Component {

    /** Откуда можно использовать. */
    public enum Reach {
        /** Стоя на клетке сущности (лестница, предмет, телепорт, кнопка). */
        SELF,
        /** С соседней клетки (дверь, сундук, табличка, магазин). */
        ADJACENT
    }

    public Reach reach = Reach.SELF;

    @Override
    public void reset() {
        reach = Reach.SELF;
    }
}
