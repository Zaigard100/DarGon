package com.zaig100.dargon.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.input.ActionState;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    @Test
    void restartUsesStateFromLevelEntry() {
        GameSession session = new GameSession(TestGame.data(), new ActionState());
        LevelRuntime level = session.newGame();
        assertEquals("01", level.state().definition().id());
        level.health().hp = 0;
        level.state().player().coins = 7;
        level.state().player().inventory.add(ItemStack.of("potion", 1));
        level.tick();

        LevelRuntime again = session.restart();
        assertEquals("01", again.state().definition().id());
        assertEquals(PlayerState.MAX_HP, again.health().hp);
        assertEquals(0, again.state().player().coins);
        assertEquals(3, again.state().player().inventory.count("potion"));
    }

    @Test
    void advanceCarriesPlayerToNextLevel() {
        GameSession session = new GameSession(TestGame.data(), new ActionState());
        LevelRuntime level = session.newGame();
        assertNull(session.advance(), "уровень ещё идёт");
        level.health().hp = 2;
        level.health().shield = 7;
        level.tick();
        level.state().finish(LevelOutcome.exit("02"));

        LevelRuntime next = session.advance();
        assertEquals("02", next.state().definition().id());
        assertEquals(2, next.health().hp);
        assertEquals(
                7,
                next.health().shield,
                "щит держится до поломки и переходит на следующий уровень");
    }
}
