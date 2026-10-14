package com.zaig100.dargon.game;

import static com.zaig100.dargon.game.TestGame.json;
import static com.zaig100.dargon.game.TestGame.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.GridPoint;
import org.junit.jupiter.api.Test;

class PlayerMovementTest {

    private static final String[] OPEN = {"....", "....", "....", "#..."};

    @Test
    void stepTakesSixteenTicksAndCellChangesAtOnce() {
        TestGame game = json(map("[1, 1]", "", OPEN));
        game.tap(Action.MOVE_RIGHT);
        game.ticks(1);
        // Клетка меняется сразу, картинка догоняет 16 тиков (1 пиксель за тик).
        assertEquals(new GridPoint(2, 1), game.pos());
        assertTrue(game.moving());
        game.ticks(14);
        assertTrue(game.moving());
        game.ticks(1);
        assertFalse(game.moving());
    }

    @Test
    void holdingRepeatsStepsAfterEachFinishes() {
        TestGame game = json(map("[0, 0]", "", OPEN));
        game.hold(Action.MOVE_RIGHT);
        game.ticks(16);
        assertEquals(new GridPoint(1, 0), game.pos());
        game.ticks(1);
        assertEquals(new GridPoint(2, 0), game.pos());
    }

    @Test
    void wallsAndMapBorderBlock() {
        TestGame game = json(map("[0, 2]", "", OPEN));
        game.hold(Action.MOVE_DOWN);
        game.ticks(20);
        assertEquals(new GridPoint(0, 2), game.pos());
        game.releaseAll(Action.MOVE_DOWN);
        game.hold(Action.MOVE_LEFT);
        game.ticks(20);
        assertEquals(new GridPoint(0, 2), game.pos());
    }

    @Test
    void twoKeysGiveDiagonalStepCheckingOnlyTarget() {
        // Обе соседние клетки — стены, но диагональ свободна: в DarGon3 так можно пройти.
        TestGame game = json(map("[0, 0]", "", ".#", "#."));
        game.tap(Action.MOVE_RIGHT);
        game.tap(Action.MOVE_DOWN);
        game.ticks(1);
        assertEquals(new GridPoint(1, 1), game.pos());
        game.ticks(15);
        assertFalse(game.moving(), "диагональный шаг длится те же 16 тиков");
    }

    @Test
    void downAndRightWinOverOppositeKeys() {
        TestGame game = json(map("[1, 1]", "", OPEN));
        game.hold(Action.MOVE_UP, Action.MOVE_DOWN, Action.MOVE_LEFT, Action.MOVE_RIGHT);
        game.ticks(1);
        assertEquals(new GridPoint(2, 2), game.pos());
    }

    @Test
    void tapBetweenFramesIsNotLostAndNotRepeated() {
        TestGame game = json(map("[0, 0]", "", OPEN));
        game.tap(Action.MOVE_RIGHT);
        game.ticks(40);
        assertEquals(new GridPoint(1, 0), game.pos());
    }

    @Test
    void pressDuringStepIsBufferedOnce() {
        TestGame game = json(map("[0, 0]", "", OPEN));
        game.tap(Action.MOVE_RIGHT);
        game.ticks(5);
        game.tap(Action.MOVE_DOWN);
        game.tap(Action.MOVE_RIGHT);
        game.ticks(11);
        assertFalse(game.moving());
        game.ticks(1);
        assertEquals(
                new GridPoint(2, 1), game.pos(), "две стрелки во время шага — диагональ после");
        game.ticks(100);
        assertEquals(new GridPoint(2, 1), game.pos(), "запомненный шаг выполняется один раз");
    }

    @Test
    void clickWalksAlongShortestPath() {
        TestGame game = json(map("[0, 0]", "", ".#..", ".#..", "....", "...."));
        game.click(3, 0);
        game.tickUntil(() -> game.pos().equals(new GridPoint(3, 0)) && !game.moving(), 200);
    }

    @Test
    void keyboardCancelsClickPath() {
        TestGame game = json(map("[0, 0]", "", OPEN));
        game.click(3, 0);
        game.ticks(1);
        // Нажатие во время шага отменяет путь и выполняется после текущего шага.
        game.tap(Action.MOVE_DOWN);
        game.ticks(100);
        assertEquals(new GridPoint(1, 1), game.pos());
    }

    @Test
    void inDarknessClicksOnlyWithinLight() {
        TestGame game = json(map("[0, 0]", "", OPEN));
        game.level.state().setDark(true);
        game.click(3, 0);
        game.ticks(60);
        assertEquals(new GridPoint(0, 0), game.pos(), "в темноту не идём");
        game.click(1, 1);
        game.tickUntil(() -> game.pos().equals(new GridPoint(1, 1)) && !game.moving(), 100);
        game.level.state().setDark(false);
        game.click(3, 0);
        game.tickUntil(() -> game.pos().equals(new GridPoint(3, 0)) && !game.moving(), 200);
    }
}
