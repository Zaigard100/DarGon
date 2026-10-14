package com.zaig100.dargon.game;

import static com.zaig100.dargon.game.TestGame.json;
import static com.zaig100.dargon.game.TestGame.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.ecs.Entity;
import com.zaig100.dargon.game.component.Container;
import com.zaig100.dargon.game.component.Door;
import com.zaig100.dargon.game.component.Lock;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.item.Inventory;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.game.item.PlayerActions;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.GridPoint;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InteractionTest {

    private static final String[] ROW = {"....."};

    private static ItemStack key(String id) {
        return new ItemStack("key", 1, Map.of("key", id, "color", "#FF0000"));
    }

    private static void use(TestGame game) {
        game.tap(Action.USE);
        game.ticks(1);
    }

    @Test
    void doorOpensWithKeyFromNeighbourCellAndStaysOpen() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"door\", \"id\": \"d\", \"at\": [2, 0], \"key\": \"red\" }",
                                ROW));
        game.hold(Action.MOVE_RIGHT);
        game.ticks(40);
        assertEquals(new GridPoint(1, 0), game.pos(), "закрытая дверь непроходима");
        game.releaseAll(Action.MOVE_RIGHT);
        use(game);
        assertFalse(game.get("d", Door.class).open, "без ключа не открыть");
        game.player.inventory.add(key("red"));
        use(game);
        assertTrue(game.get("d", Door.class).open);
        assertFalse(game.level.world().has(game.entity("d"), Marker.Blocking.class));
        assertEquals(1, game.player.inventory.count("key"), "ключ не тратится");
        use(game);
        assertTrue(game.get("d", Door.class).open, "открытая дверь остаётся открытой");
    }

    @Test
    void chestUnlocksThenGivesWhatFits() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"chest\", \"id\": \"c\", \"at\": [1, 0], \"locked\": true, \"key\": \"k\","
                                        + " \"items\": [ { \"id\": \"coins\", \"count\": 30 }, { \"id\": \"potion\", \"count\": 2 } ] }",
                                ROW));
        use(game);
        assertTrue(game.get("c", Lock.class).locked);
        game.player.inventory.add(key("k"));
        use(game);
        assertFalse(game.get("c", Lock.class).locked, "первое использование с ключом отпирает");
        assertEquals(0, game.player.coins);
        use(game);
        assertEquals(30, game.player.coins);
        assertEquals(5, game.player.inventory.count("potion"));
        assertTrue(game.get("c", Container.class).items.isEmpty());
    }

    @Test
    void tabletShowsTextUntilPlayerMoves() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"tablet\", \"at\": [1, 1], \"text\": [\"a\", \"b\"] }",
                                "...",
                                "..."));
        use(game);
        assertEquals(List.of("a", "b"), game.level.state().message());
        game.tap(Action.MOVE_DOWN);
        game.ticks(1);
        assertTrue(game.level.state().message().isEmpty());
    }

    @Test
    void shopHasLimitedStockAndNeedsRoom() {
        TestGame game =
                json(
                        map(
                                "[0, 0]",
                                "{ \"prefab\": \"shop\", \"id\": \"s\", \"at\": [1, 0], \"offers\": ["
                                        + " { \"item\": { \"id\": \"potion\" }, \"price\": 5, \"stock\": 3 } ] }",
                                ROW));
        use(game);
        int shop = game.level.state().openShop();
        assertEquals(game.entity("s"), shop);
        PlayerActions actions = new PlayerActions(game.level.world(), TestGame.data().items());
        assertFalse(actions.buy(shop, 0), "нет денег");
        game.player.coins = 12;
        assertTrue(actions.buy(shop, 0));
        assertTrue(actions.buy(shop, 0));
        assertEquals(2, game.player.coins);
        assertEquals(5, game.player.inventory.count("potion"));
        game.player.coins = 100;
        Inventory backup = game.player.inventory.copy();
        for (int i = 0; i < Inventory.SIZE; i++) {
            game.player.inventory.set(i, key("x" + i));
        }
        assertFalse(actions.buy(shop, 0), "инвентарь полон — покупка не проходит");
        assertEquals(100, game.player.coins);
        for (int i = 0; i < Inventory.SIZE; i++) {
            game.player.inventory.set(i, backup.get(i));
        }
        assertTrue(actions.buy(shop, 0), "третья — последняя в запасе");
        assertFalse(actions.buy(shop, 0), "товар кончился");
        game.level.state().setOpenShop(Entity.NONE);
    }

    @Test
    void itemEffects() {
        TestGame game = json(map("[0, 0]", "", ROW));
        game.level.state().setDark(true);
        PlayerActions actions = new PlayerActions(game.level.world(), TestGame.data().items());
        // Стартовый инвентарь: зелья (0), щиты (1), факел (2).
        assertFalse(actions.use(0), "при полном здоровье зелье не пьётся");
        game.level.health().hp = 20;
        assertTrue(actions.use(0));
        assertEquals(35, game.level.health().hp, "зелье +15");
        assertEquals(2, game.player.inventory.count("potion"));
        assertTrue(actions.use(1));
        assertEquals(Balance.SHIELD, game.level.health().shield);
        assertTrue(actions.use(1), "щиты суммируются");
        assertEquals(Balance.SHIELD_MAX, game.level.health().shield);
        game.player.inventory.add(ItemStack.of("shield", 1));
        assertFalse(actions.use(1), "выше 30 не набрать");
        assertTrue(actions.use(2));
        assertFalse(game.level.state().isDark());
        assertEquals(0f, game.level.state().torchProgress(), "факел разгорается не мгновенно");
        game.ticks(90);
        assertEquals(0.5f, game.level.state().torchProgress(), 0.02f);
        game.ticks(100);
        assertEquals(1f, game.level.state().torchProgress());
        assertEquals(0, game.player.inventory.count("torch"));

        game.player.inventory.add(new ItemStack("speed_potion", 1, Map.of("power", 2.0)));
        game.player.inventory.add(new ItemStack("trap_sight", 1, Map.of()));
        int speed = slot(game, "speed_potion");
        assertTrue(actions.use(speed));
        assertEquals(2f, game.level.world().get(game.level.player(), Motion.class).speed);
        assertTrue(actions.use(slot(game, "trap_sight")));
        assertTrue(game.level.state().trapSight());
    }

    @Test
    void infEggMakesImmortalForThisLevel() {
        TestGame game = json(map("[0, 0]", "{ \"prefab\": \"hidden_trap\", \"at\": [1, 0] }", ROW));
        game.player.inventory.add(new ItemStack("egg", 1, Map.of("code", "inf")));
        PlayerActions actions = new PlayerActions(game.level.world(), TestGame.data().items());
        assertTrue(actions.use(slot(game, "egg")));
        assertEquals(0, game.player.inventory.count("egg"), "кубик тратится");
        game.tap(Action.MOVE_RIGHT);
        game.ticks(2);
        assertEquals(PlayerState.MAX_HP, game.level.health().hp, "бессмертие на этом уровне");
    }

    @Test
    void droppedItemStaysAndCanBePickedUp() {
        TestGame game = json(map("[0, 0]", "", ROW));
        PlayerActions actions = new PlayerActions(game.level.world(), TestGame.data().items());
        assertTrue(actions.drop(0));
        assertEquals(2, game.player.inventory.count("potion"));
        game.ticks(5);
        use(game);
        assertEquals(3, game.player.inventory.count("potion"));
    }

    private static int slot(TestGame game, String id) {
        for (int i = 0; i < Inventory.SIZE; i++) {
            ItemStack stack = game.player.inventory.get(i);
            if (stack != null && stack.id().equals(id)) {
                return i;
            }
        }
        throw new AssertionError("нет " + id);
    }
}
