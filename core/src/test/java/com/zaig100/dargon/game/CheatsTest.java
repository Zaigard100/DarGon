package com.zaig100.dargon.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.GridPoint;
import com.zaig100.dargon.save.SaveCodec;
import com.zaig100.dargon.save.SaveException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheatsTest {

    @Test
    void noclipAndImmortalAndMarkSurvivesSave() throws SaveException {
        List<Progress> saves = new ArrayList<>();
        ActionState input = new ActionState();
        GameSession session =
                new GameSession(TestGame.data(), input, GameLog.NONE, saves::add, () -> 1L);
        session.newGame(GameMode.NORMAL);
        assertFalse(session.progress().cheats());

        session.cheats().noclip = true;
        session.cheats().immortal = true;
        session.cheats().apply(session.level());
        session.markCheats();
        // Уровень 01: справа от старта стена — со «сквозь стены» шаг проходит.
        input.press(Action.MOVE_RIGHT);
        input.release(Action.MOVE_RIGHT);
        session.level().tick();
        assertEquals(
                new GridPoint(1, 9),
                new GridPoint(session.level().playerPos().x, session.level().playerPos().y));
        session.level().health().hp = 1;
        session.level()
                .world()
                .events()
                .publish(GameEvents.Damage.hit(session.level().player(), 5, 5, "trap", false));
        session.level().tick();
        assertEquals(1, session.level().health().hp, "бессмертие");

        Progress loaded =
                SaveCodec.read(SaveCodec.write(saves.get(saves.size() - 1), 2), TestGame.data());
        assertTrue(loaded.cheats(), "пометка прохождения сохраняется");

        session.level().state().finish(LevelOutcome.exit("02"));
        session.advance();
        assertTrue(session.level().health().immortal, "инструменты действуют на всё прохождение");
    }
}
