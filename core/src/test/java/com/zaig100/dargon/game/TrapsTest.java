package com.zaig100.dargon.game;

import static com.zaig100.dargon.game.TestGame.json;
import static com.zaig100.dargon.game.TestGame.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.game.component.Emitter;
import com.zaig100.dargon.game.component.Fragile;
import com.zaig100.dargon.game.component.Projectile;
import com.zaig100.dargon.game.component.TriggeredCycle;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.Direction;
import org.junit.jupiter.api.Test;

/** Ловушки повторяют таймеры DarGon3. */
class TrapsTest {

    private static final String[] ROW = {"......"};

    private static int seconds(double s) {
        return (int) Math.round(s * 60);
    }

    @Test
    void spikesExtendAfterDelayAndHurtWhoStays() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"spikes\", \"id\": \"s\", \"at\": [1, 0], \"period\": 1.0 }",
                                ROW));
        game.tap(Action.MOVE_RIGHT);
        game.ticks(seconds(0.5));
        assertFalse(game.get("s", TriggeredCycle.class).active, "ещё не выдвинулись");
        assertFalse(game.level.isPlayerDead());
        game.ticks(seconds(0.6));
        assertTrue(game.get("s", TriggeredCycle.class).active);
        int hp = game.level.health().hp;
        assertTrue(hp >= 6 && hp <= 14, "шипы 26–34: " + hp);
        assertEquals("spikes", game.level.health().lastDamageKind);
        game.ticks(seconds(1.0));
        assertTrue(game.level.isPlayerDead(), "стоящего на шипах добивает второй удар");
    }

    @Test
    void spikesCanBeCrossedAndRetractAfterTwiceTheDelay() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"spikes\", \"id\": \"s\", \"at\": [1, 0], \"period\": 1.0 }",
                                ROW));
        game.hold(Action.MOVE_RIGHT);
        game.ticks(40);
        game.releaseAll(Action.MOVE_RIGHT);
        game.ticks(seconds(1.2));
        assertFalse(game.level.isPlayerDead(), "ушёл до срабатывания");
        assertTrue(game.get("s", TriggeredCycle.class).active);
        game.ticks(seconds(2.1));
        assertFalse(game.get("s", TriggeredCycle.class).active);
    }

    @Test
    void fragileFloorCracksWhileStoodOnAndThenBreaks() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"fragile_floor\", \"id\": \"f\", \"at\": [1, 0], \"period\": 1.0 }",
                                ROW));
        game.ticks(2);
        game.tap(Action.MOVE_RIGHT);
        game.ticks(1);
        assertEquals(1, game.get("f", Fragile.class).cracks, "первая трещина — сразу");
        game.ticks(seconds(1.05));
        assertEquals(2, game.get("f", Fragile.class).cracks);
        game.ticks(seconds(1.05));
        assertEquals(3, game.get("f", Fragile.class).cracks);
        game.ticks(2);
        assertTrue(game.level.isPlayerDead());
        assertEquals("fall", game.level.health().lastDamageKind);
    }

    @Test
    void flamethrowerGrowsFasterWhenLongerAndBurns() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"flamethrower\", \"id\": \"f\", \"at\": [5, 0], \"dir\": \"left\", \"length\": 2, \"period\": 1.0 }",
                                ROW));
        Emitter e = game.get("f", Emitter.class);
        game.ticks(seconds(1.0) + 1);
        assertEquals(1, e.stage);
        // Длина 1 — таймер идёт вдвое быстрее: следующая стадия через 0,5 с.
        game.ticks(seconds(0.5) + 1);
        assertEquals(2, e.stage);
        game.ticks(seconds(1.0 / 3) + 1);
        assertEquals(0, e.stage, "после максимума гаснет");
        game.teleport(3, 0);
        game.ticks(seconds(1.0) + seconds(0.5) + 2);
        int hp = game.level.health().hp;
        assertTrue(hp >= 6 && hp <= 14, "огонь 26–34: " + hp);
        assertEquals("fire", game.level.health().lastDamageKind);
    }

    @Test
    void crossbowShootsAtOnceAndArrowsHurt() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"crossbow\", \"at\": [5, 0], \"dir\": \"left\", \"period\": 100 }",
                                ROW));
        game.ticks(1);
        assertEquals(1, arrows(game));
        game.tickUntil(() -> game.level.health().hp < PlayerState.MAX_HP, 200);
        int lost = PlayerState.MAX_HP - game.level.health().hp;
        assertTrue(lost >= 17 && lost <= 23, "стрела 17–23: " + lost);
        assertEquals(0, arrows(game), "стрела исчезает при попадании");

        TestGame shielded =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"crossbow\", \"at\": [5, 0], \"dir\": \"left\", \"period\": 100 }",
                                ROW));
        shielded.level.health().shield = Balance.SHIELD;
        shielded.tickUntil(() -> shielded.level.health().hp < PlayerState.MAX_HP, 200);
        int body = PlayerState.MAX_HP - shielded.level.health().hp;
        int shield = Balance.SHIELD - shielded.level.health().shield;
        assertTrue(body >= 9 && body <= 12, "телу — половина: " + body);
        assertTrue(shield >= 4 && shield <= 5, "щиту — четверть: " + shield);
    }

    @Test
    void arrowsFlyThroughWallsAndExpire() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"crossbow\", \"at\": [3, 1], \"dir\": \"left\", \"period\": 100 }",
                                "......",
                                "#.#.##"));
        game.ticks(1);
        // Пролетает 2 × 6 клеток за 16 тиков каждую и исчезает.
        game.ticks(16 * 12 + 2);
        assertEquals(0, arrows(game));
        assertFalse(game.level.isPlayerDead());
    }

    @Test
    void smartCrossbowAimsOnlyWithClearLine() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"smart_crossbow\", \"id\": \"c\", \"at\": [5, 2], \"range\": 10, \"diagonal\": true }",
                                "......",
                                "......",
                                "......"));
        game.teleport(3, 0);
        game.ticks(1);
        assertEquals(Direction.UP_LEFT, game.get("c", Emitter.class).dir);
        game.teleport(2, 0);
        game.ticks(1);
        assertEquals(
                Direction.UP_LEFT, game.get("c", Emitter.class).dir, "не на линии: dx=3, dy=2");
        game.teleport(5, 0);
        game.level.state().setTile(5, 1, com.zaig100.dargon.level.TileType.WALL);
        game.ticks(1);
        assertEquals(Direction.UP_LEFT, game.get("c", Emitter.class).dir, "за стеной не целится");
    }

    @Test
    void zonesHealAndHurtOncePerPeriod() {
        TestGame heal =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"zone\", \"at\": [0, 0], \"width\": 2, \"height\": 1, \"effect\": \"heal\", \"period\": 1.0 }",
                                ROW));
        heal.level.health().hp = 37;
        heal.level.health().shield = 8;
        heal.ticks(seconds(1.0) + 1);
        assertEquals(38, heal.level.health().hp, "+1 HP за период");
        assertEquals(9, heal.level.health().shield, "щит, если есть, чинится");
        heal.ticks(seconds(5));
        assertEquals(PlayerState.MAX_HP, heal.level.health().hp, "не больше максимума");
        assertEquals(Balance.ZONE_SHIELD_REPAIR, heal.level.health().shield, "щит — не выше 10");

        TestGame hurt =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"zone\", \"at\": [0, 0], \"width\": 1, \"height\": 1, \"effect\": \"damage\", \"period\": 1.0 }",
                                ROW));
        hurt.level.health().shield = Balance.SHIELD;
        hurt.ticks(seconds(1.0) + 1);
        assertEquals(PlayerState.MAX_HP - 1, hurt.level.health().hp, "−1 HP за период сквозь щит");
        hurt.ticks(seconds(1.0) + 1);
        assertEquals(
                PlayerState.MAX_HP - 2,
                hurt.level.health().hp,
                "неуязвимость зону не останавливает");
        assertEquals(Balance.SHIELD, hurt.level.health().shield);
    }

    @Test
    void kamikazeChasesWithinDepthAndExplodes() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"kamikaze\", \"id\": \"k\", \"at\": [5, 0], \"radius\": 6 }",
                                ROW));
        game.level.health().shield = Balance.SHIELD;
        game.tickUntil(() -> game.level.health().hp < PlayerState.MAX_HP, 600);
        int body = PlayerState.MAX_HP - game.level.health().hp;
        assertTrue(body >= 9 && body <= 12, "взрыв 17–23, щит принимает часть: " + body);
        assertEquals("explosion", game.level.health().lastDamageKind);

        TestGame far =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"kamikaze\", \"id\": \"k\", \"at\": [5, 0], \"radius\": 6, \"depth\": 3 }",
                                ROW));
        far.ticks(600);
        assertEquals(PlayerState.MAX_HP, far.level.health().hp, "путь длиннее depth — не находит");
    }

    @Test
    void lethalHitWithShieldLeavesOneHpAndOneHpDiesSometimes() {
        TestGame game = json(map("[0, 0]", "", ROW));
        var h = game.level.health();
        h.hp = 5;
        h.shield = Balance.SHIELD_MAX;
        game.level
                .world()
                .events()
                .publish(GameEvents.Damage.hit(game.level.player(), 30, 30, "spikes", true));
        game.ticks(1);
        assertEquals(1, h.hp, "смертельный удар при щите оставляет 1 HP");
        int deaths = 0;
        for (int i = 0; i < 400; i++) {
            h.hp = 1;
            h.shield = Balance.SHIELD_MAX;
            h.invulnerable = 0;
            game.level
                    .world()
                    .events()
                    .publish(GameEvents.Damage.hit(game.level.player(), 30, 30, "spikes", true));
            game.ticks(1);
            if (h.isDead()) {
                deaths++;
            }
        }
        assertTrue(deaths > 15 && deaths < 70, "на 1 HP — около 10 % смертей: " + deaths);
    }

    @Test
    void fallKillsThroughShield() {
        TestGame game = json(map("[0, 0]", "", ROW));
        game.level.health().shield = Balance.SHIELD_MAX;
        game.level.world().events().publish(GameEvents.Damage.kill(game.level.player(), "fall"));
        game.ticks(1);
        assertTrue(game.level.isPlayerDead());
    }

    private static int arrows(TestGame game) {
        EntitySet set = game.level.world().entities(Family.all(Projectile.class));
        return set.size();
    }
}
