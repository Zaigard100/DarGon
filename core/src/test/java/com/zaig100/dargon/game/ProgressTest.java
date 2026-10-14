package com.zaig100.dargon.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.input.ActionState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Правила смерти и сохранения: перезапуск, откат после трёх смертей, хардкор. */
class ProgressTest {

    private final List<Progress> saves = new ArrayList<>();

    private GameSession session() {
        return new GameSession(
                TestGame.data(), new ActionState(), GameLog.NONE, saves::add, () -> 1000L);
    }

    @Test
    void twoDeathsRestartLevelThirdRollsBackToCheckpoint() {
        GameSession session = session();
        session.enter("05", PlayerState.newGame(TestGame.data().items()));
        // 05 — контрольная точка; идём на 06 с монетами.
        session.level().state().player().coins = 40;
        session.level().state().finish(LevelOutcome.exit("06"));
        session.advance();
        assertEquals("06", session.level().state().definition().id());

        assertEquals(Progress.Death.RESTART, session.died());
        assertEquals(2, session.progress().attemptsLeft());
        session.afterDeath();
        assertEquals("06", session.level().state().definition().id());
        assertEquals(40, session.level().state().player().coins, "заново — с тем, с чем вошёл");

        assertEquals(Progress.Death.RESTART, session.died());
        session.afterDeath();
        assertEquals(Progress.Death.ROLLBACK, session.died());
        session.afterDeath();
        assertEquals("05", session.level().state().definition().id());
        assertEquals(
                0, session.level().state().player().coins, "откат — со снимком контрольной точки");
        assertEquals(0, session.progress().deathsInRow());
    }

    @Test
    void passingALevelResetsTheCounter() {
        GameSession session = session();
        session.enter("06", PlayerState.newGame(TestGame.data().items()));
        session.died();
        session.died();
        session.afterDeath();
        session.level().state().finish(LevelOutcome.exit("07"));
        session.advance();
        assertEquals(0, session.progress().deathsInRow());
        assertEquals(3, session.progress().attemptsLeft());
    }

    @Test
    void beforeFirstCheckpointRollbackGoesToStartWithStartingKit() {
        GameSession session = session();
        session.newGame(GameMode.NORMAL);
        session.level().state().player().inventory.add(ItemStack.of("key", 1));
        session.level().state().finish(LevelOutcome.exit("02"));
        session.advance();
        for (int i = 0; i < 3; i++) {
            session.died();
        }
        session.afterDeath();
        assertEquals("01", session.level().state().definition().id());
        assertEquals(0, session.level().state().player().inventory.count("key"));
    }

    @Test
    void hardcoreRollsBackOnAnyDeathAndResumesOnlyFromCheckpoint() {
        GameSession session = session();
        session.newGame(GameMode.HARDCORE);
        session.level().state().finish(LevelOutcome.exit("02"));
        session.advance();
        assertEquals(
                "01",
                session.progress().resumeTarget().level(),
                "продолжить — только с контрольной точки");
        assertEquals(Progress.Death.ROLLBACK, session.died());
        assertEquals(
                "01",
                session.progress().restartTarget().level(),
                "«заново» в хардкоре — тоже откат");
    }

    @Test
    void everyEntryAndDeathIsSaved() {
        GameSession session = session();
        session.newGame(GameMode.NORMAL);
        session.died();
        session.afterDeath();
        assertEquals(3, saves.size(), "вход, смерть, повторный вход");
    }

    @Test
    void casualAlwaysRestartsAndHealsToHalfOnNewLevel() {
        GameSession session = session();
        session.newGame(GameMode.CASUAL);
        assertEquals(4, session.level().state().player().inventory.count("potion"));
        for (int i = 0; i < 5; i++) {
            assertEquals(Progress.Death.RESTART, session.died());
        }
        session.afterDeath();
        session.level().health().hp = 5;
        session.level().tick();
        session.level().state().finish(LevelOutcome.exit("02"));
        session.advance();
        assertEquals(20, session.level().health().hp, "казуал лечит до половины при переходе");
    }

    @Test
    void hardcoreStartsWithMinimalKit() {
        GameSession session = session();
        session.newGame(GameMode.HARDCORE);
        var inventory = session.level().state().player().inventory;
        assertEquals(1, inventory.count("potion"));
        assertEquals(0, inventory.count("shield"));
        assertEquals(1, inventory.count("torch"));
    }
}
