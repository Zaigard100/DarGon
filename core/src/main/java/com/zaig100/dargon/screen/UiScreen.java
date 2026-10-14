package com.zaig100.dargon.screen;

import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.ui.FocusNavigator;
import com.zaig100.dargon.ui.UiNode;

/**
 * Экран интерфейса в стеке {@link Screens}. Описывает свой вид деревом узлов; стек собирает его,
 * пересобирает при смене масштаба и отписывает при закрытии.
 */
public abstract class UiScreen {

    /** Где экран лежит: на всём окне или поверх игрового окна 7×5. */
    public enum Area {
        FULL,
        GAME
    }

    protected App app;

    void attach(App app) {
        this.app = app;
    }

    /** Вид экрана. Вызывается при открытии и при пересборке. */
    protected abstract UiNode build();

    public Area area() {
        return Area.FULL;
    }

    /** Пока экран открыт, мир игры стоит. */
    public boolean pausesGame() {
        return true;
    }

    /** Экран принимает клавиши (иначе они идут в игру). */
    public boolean modal() {
        return true;
    }

    /**
     * Нажатие действия, пока экран сверху. По умолчанию: стрелки двигают фокус, «использовать»
     * нажимает кнопку, Esc закрывает экран.
     *
     * @return {@code true}, если обработано
     */
    public boolean onAction(Action action, FocusNavigator focus) {
        switch (action) {
            case MOVE_UP:
                focus.move(0, 1);
                return true;
            case MOVE_DOWN:
                focus.move(0, -1);
                return true;
            case MOVE_LEFT:
                focus.move(-1, 0);
                return true;
            case MOVE_RIGHT:
                focus.move(1, 0);
                return true;
            case USE:
                focus.activate();
                return true;
            case PAUSE:
                back();
                return true;
            default:
                return false;
        }
    }

    /** «Назад» (Esc, кнопка закрытия): по умолчанию закрыть экран. */
    protected void back() {
        app.screens().pop(this);
    }

    /** Каждый кадр, пока экран открыт. */
    public void update(float delta) {}

    /** Экран закрыт. */
    protected void closed() {}
}
