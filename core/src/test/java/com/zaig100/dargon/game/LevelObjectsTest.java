package com.zaig100.dargon.game;

import static com.zaig100.dargon.game.TestGame.json;
import static com.zaig100.dargon.game.TestGame.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.game.item.Inventory;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.GridPoint;
import org.junit.jupiter.api.Test;

class LevelObjectsTest {

    private static final String[] ROW = {"....."};

    @Test
    void hiddenTrapHitsOnceAndDisappears() {
        TestGame game = json(map("[0, 0]", "{ \"prefab\": \"hidden_trap\", \"at\": [1, 0] }", ROW));
        game.tap(Action.MOVE_RIGHT);
        game.ticks(1);
        int hp = game.level.health().hp;
        assertTrue(hp >= 28 && hp <= 32, "урон ловушки 8–12: " + hp);
        assertEquals("trap", game.level.health().lastDamageKind);
        game.ticks(60);
        game.tap(Action.MOVE_LEFT);
        game.ticks(17);
        game.tap(Action.MOVE_RIGHT);
        game.ticks(17);
        assertEquals(hp, game.level.health().hp, "ловушка одноразовая");
        assertEquals(hp, game.player.hp, "здоровье переносится в состояние игрока");
    }

    @Test
    void shieldTakesBodyShareThenShieldShare() {
        TestGame game = json(map("[0, 0]", "{ \"prefab\": \"hidden_trap\", \"at\": [1, 0] }", ROW));
        game.level.health().shield = Balance.SHIELD;
        game.tap(Action.MOVE_RIGHT);
        game.ticks(1);
        int lost = PlayerState.MAX_HP - game.level.health().hp;
        assertTrue(lost >= 4 && lost <= 6, "телу — половина 8–12: " + lost);
        int shieldLost = Balance.SHIELD - game.level.health().shield;
        assertTrue(shieldLost >= 2 && shieldLost <= 3, "щиту — четверть (2–3): " + shieldLost);
        assertTrue(game.level.health().sinceDamage < 0.5f, "вспышка есть всегда");
    }

    @Test
    void invulnerabilityKeepsTrapsWaitingAndTrapsCanKill() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"hidden_trap\", \"at\": [1, 0] }, { \"prefab\": \"hidden_trap\", \"at\": [2, 0] }",
                                ROW));
        game.level.health().hp = 13;
        game.hold(Action.MOVE_RIGHT);
        game.ticks(17);
        // Вторая ловушка под неуязвимостью не сработала и не сгорела.
        int afterFirst = game.level.health().hp;
        assertTrue(afterFirst >= 1 && afterFirst <= 5, String.valueOf(afterFirst));
        game.releaseAll(Action.MOVE_RIGHT);
        game.ticks(60);
        assertTrue(game.level.isPlayerDead(), "после неуязвимости вторая ловушка добивает");
        game.ticks(100);
        assertEquals(new GridPoint(2, 0), game.pos(), "мёртвый игрок не ходит");
    }

    @Test
    void teleportMovesInstantly() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"teleport\", \"at\": [0, 0], \"target\": [4, 0] }",
                                ROW));
        game.tap(Action.USE);
        game.ticks(1);
        assertEquals(new GridPoint(4, 0), game.pos());
        assertFalse(game.moving());
    }

    @Test
    void pickupGoesToInventoryOnlyWhenThereIsRoom() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"item\", \"at\": [0, 0], \"item\": { \"id\": \"coins\", \"count\": 5 } }",
                                ROW));
        game.tap(Action.USE);
        game.ticks(1);
        assertEquals(5, game.player.coins, "монеты — сразу в кошелёк");
        assertEquals(0, game.player.inventory.count("coins"));

        TestGame full =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"item\", \"at\": [0, 0], \"item\": { \"id\": \"egg\", \"code\": \"inf\" } }",
                                ROW));
        for (int i = 0; full.player.inventory.add(ItemStack.of("key", 1)); i++) {
            assertTrue(i < 20);
        }
        full.tap(Action.USE);
        full.ticks(1);
        assertEquals(0, full.player.inventory.count("egg"));
        full.player.inventory.set(Inventory.SIZE - 1, null);
        full.tap(Action.USE);
        full.ticks(1);
        assertEquals(
                1, full.player.inventory.count("egg"), "предмет остался лежать и подобран позже");
    }

    @Test
    void stairsFinishLevel() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"stairs\", \"at\": [2, 0], \"next\": \"05\" }",
                                ROW));
        game.click(2, 0);
        game.tickUntil(() -> game.level.outcome() != LevelOutcome.NONE, 100);
        assertEquals(LevelOutcome.exit("05"), game.level.outcome());
        assertEquals(LevelOutcome.Kind.GAME_END, LevelOutcome.exit(null).kind());
    }
}
