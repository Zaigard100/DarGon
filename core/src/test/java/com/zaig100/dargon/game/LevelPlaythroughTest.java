package com.zaig100.dargon.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.LevelExit;
import com.zaig100.dargon.game.level.Components;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Уровни проходятся: игрок кликает по лестнице и доходит до неё живым. */
class LevelPlaythroughTest {

    @Test
    void everyPrefabComponentIsRegistered() {
        assertEquals(Set.of(), Components.registry().missing(TestGame.data().prefabs()));
    }

    @Test
    void componentCatalogMatchesRegistry() {
        assertEquals(Components.registry().names(), TestGame.data().components().all().keySet());
    }

    @Test
    void everyLevelStarts() {
        for (String id : TestGame.data().levels()) {
            TestGame game = TestGame.level(id);
            game.ticks(2);
            assertFalse(game.level.isPlayerDead(), id);
        }
    }

    @ParameterizedTest
    @CsvSource({"01, 02", "02, 03", "03, 04"})
    void levelIsCompletedByClickingStairs(String id, String next) {
        TestGame game = TestGame.level(id);
        GridPos stairs = exitTo(game, next);
        // В темноте клик дальше круга света не работает — проходим «с факелом».
        game.level.state().setDark(false);
        game.click(stairs.x, stairs.y);
        game.tickUntil(() -> game.level.outcome() != LevelOutcome.NONE, 60 * 60);
        assertEquals(LevelOutcome.exit(next), game.level.outcome());
        assertTrue(game.player.hp > 0);
    }

    private static GridPos exitTo(TestGame game, String next) {
        EntitySet exits = game.level.world().entities(Family.all(LevelExit.class, GridPos.class));
        for (int i = 0; i < exits.size(); i++) {
            int e = exits.get(i);
            if (next.equals(game.level.world().get(e, LevelExit.class).next)) {
                return game.level.world().get(e, GridPos.class);
            }
        }
        throw new AssertionError("нет лестницы на " + next);
    }
}
