package com.zaig100.dargon.ecs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.ecs.TestComponents.Health;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventBusTest {

    record Damage(int target, int amount) {}

    record Died(int entity) {}

    /** Публикует урон всем с Health и пишет в лог момент публикации. */
    static final class Hurt extends IteratingSystem {
        final List<String> log;

        Hurt(List<String> log) {
            super(Family.all(Health.class));
            this.log = log;
        }

        @Override
        protected void process(int entity, float delta) {
            events.publish(new Damage(entity, 5));
            log.add("publish");
        }
    }

    /** Применяет урон, удаляет умерших, публикует Died. */
    static final class Damager extends GameSystem {
        final List<String> log;

        Damager(List<String> log) {
            this.log = log;
        }

        @Override
        protected void initialize() {
            events.subscribe(
                    Damage.class,
                    d -> {
                        log.add("damage");
                        Health h = world.get(d.target(), Health.class);
                        h.hp -= d.amount();
                        if (h.hp <= 0 && world.destroy(d.target())) {
                            events.publish(new Died(d.target()));
                        }
                    });
            events.subscribe(Died.class, d -> log.add("died"));
        }

        @Override
        protected void update(float delta) {
            log.add("damager.update");
        }
    }

    @Test
    void eventsAreDeliveredRightAfterPublishingSystemNotDuringIteration() {
        List<String> log = new ArrayList<>();
        World world =
                TestComponents.builder().system(new Hurt(log)).system(new Damager(log)).build();
        for (int i = 0; i < 2; i++) {
            world.add(world.create(), Health.class).hp = 5;
        }

        world.update(1f);

        assertEquals(
                List.of("publish", "publish", "damage", "damage", "died", "died", "damager.update"),
                log,
                "обработчики — после всей системы-публикатора; порождённые Died — следом, в той же точке");
        assertEquals(0, world.entityCount());
    }

    @Test
    void eventsPublishedOutsideUpdateAreDeliveredOnSync() {
        World world = TestComponents.builder().build();
        List<Object> received = new ArrayList<>();
        world.events().subscribe(Died.class, received::add);

        world.events().publish(new Died(1));
        assertTrue(received.isEmpty());
        world.sync();
        assertEquals(List.of(new Died(1)), received);
    }

    @Test
    void endlessEventLoopIsDetected() {
        World world = TestComponents.builder().build();
        EventBus bus = world.events();
        bus.subscribe(Died.class, d -> bus.publish(new Died(d.entity() + 1)));
        bus.publish(new Died(0));
        assertThrows(IllegalStateException.class, world::sync);
    }

    @Test
    void handlersAreMatchedByExactClass() {
        World world = TestComponents.builder().build();
        List<String> log = new ArrayList<>();
        world.events().subscribe(Damage.class, d -> log.add("damage"));
        world.events().publish(new Died(0));
        world.sync();
        assertTrue(log.isEmpty());
    }
}
