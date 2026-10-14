package com.zaig100.dargon.input;

import com.zaig100.dargon.level.GridPoint;
import java.util.Arrays;

/**
 * Состояние действий для логики с фиксированным шагом. Устройства ввода сообщают о нажатиях в любой
 * момент кадра ({@link #press}, {@link #release}, {@link #click}), а логика в начале каждого тика
 * вызывает {@link #beginTick()} и видит согласованный снимок.
 *
 * <p>Нажатие «запоминается» до первого тика, который его увидит: короткое нажатие не теряется, если
 * в кадре не было ни одного тика, и не срабатывает дважды, если тиков было несколько.
 */
public final class ActionState {

    private static final Action[] ACTIONS = Action.values();

    private final int[] holders = new int[ACTIONS.length];
    private final boolean[] latched = new boolean[ACTIONS.length];
    private final boolean[] pressed = new boolean[ACTIONS.length];
    private final boolean[] heldInTick = new boolean[ACTIONS.length];
    private GridPoint latchedClick;
    private GridPoint click;

    /** Действие нажато (несколько устройств могут держать одно действие одновременно). */
    public void press(Action action) {
        holders[action.ordinal()]++;
        latched[action.ordinal()] = true;
    }

    /** Действие отпущено. */
    public void release(Action action) {
        int i = action.ordinal();
        if (holders[i] > 0) {
            holders[i]--;
        }
    }

    /** Клик (тап) по клетке поля. */
    public void click(GridPoint cell) {
        latchedClick = cell;
    }

    /** Сбрасывает всё: при смене уровня или открытии оверлея. */
    public void clear() {
        Arrays.fill(holders, 0);
        Arrays.fill(latched, false);
        Arrays.fill(pressed, false);
        Arrays.fill(heldInTick, false);
        latchedClick = null;
        click = null;
    }

    /** Снимок для очередного тика логики. */
    public void beginTick() {
        for (int i = 0; i < ACTIONS.length; i++) {
            pressed[i] = latched[i];
            heldInTick[i] = holders[i] > 0 || latched[i];
            latched[i] = false;
        }
        click = latchedClick;
        latchedClick = null;
    }

    /** Действие удерживается в этом тике (или было нажато с прошлого тика). */
    public boolean isHeld(Action action) {
        return heldInTick[action.ordinal()];
    }

    /** Действие было нажато с прошлого тика. */
    public boolean wasPressed(Action action) {
        return pressed[action.ordinal()];
    }

    /** Клетка, по которой кликнули с прошлого тика, или {@code null}. */
    public GridPoint clicked() {
        return click;
    }
}
