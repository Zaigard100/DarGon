package com.zaig100.dargon.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaig100.dargon.input.Action;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Каждый уровень выдерживает минуту случайной игры: без исключений и ошибок скриптов. */
class LevelSmokeTest {

    private static final Action[] ACTIONS = {
        Action.MOVE_UP, Action.MOVE_DOWN, Action.MOVE_LEFT, Action.MOVE_RIGHT, Action.USE
    };

    static Stream<String> levels() {
        return TestGame.data().levels().stream();
    }

    @ParameterizedTest
    @MethodSource("levels")
    void randomPlay(String id) {
        TestGame game = TestGame.level(id);
        Random random = new Random(id.hashCode());
        // Бессмертие, чтобы случайный игрок успел потрогать побольше объектов.
        game.level.health().immortal = true;
        for (int i = 0; i < 60 * 60; i++) {
            if (i % 8 == 0) {
                game.tap(ACTIONS[random.nextInt(ACTIONS.length)]);
            }
            game.ticks(1);
            if (game.level.outcome() != LevelOutcome.NONE) {
                break;
            }
        }
        assertEquals(List.of(), game.errors, id);
    }
}
