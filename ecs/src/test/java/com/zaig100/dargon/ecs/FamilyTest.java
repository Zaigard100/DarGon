package com.zaig100.dargon.ecs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.ecs.TestComponents.Frozen;
import com.zaig100.dargon.ecs.TestComponents.Health;
import com.zaig100.dargon.ecs.TestComponents.Position;
import com.zaig100.dargon.ecs.TestComponents.Velocity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FamilyTest {

    private final World world = TestComponents.builder().build();

    @Test
    void allOneExcludeAreRespected() {
        EntitySet moving =
                world.entities(Family.all(Position.class, Velocity.class).exclude(Frozen.class));
        EntitySet damageable = world.entities(Family.one(Health.class, Frozen.class));

        int walker = world.create();
        world.add(walker, Position.class);
        world.add(walker, Velocity.class);

        int statue = world.create();
        world.add(statue, Position.class);
        world.add(statue, Velocity.class);
        world.add(statue, Frozen.class);

        int rock = world.create();
        world.add(rock, Position.class);

        assertTrue(moving.contains(walker));
        assertFalse(moving.contains(statue), "exclude(Frozen)");
        assertFalse(moving.contains(rock), "нет Velocity");
        assertEquals(1, moving.size());

        assertTrue(damageable.contains(statue), "one(Health, Frozen)");
        assertFalse(damageable.contains(walker));
    }

    @Test
    void membershipFollowsCompositionChanges() {
        EntitySet set = world.entities(Family.all(Position.class).exclude(Frozen.class));
        int e = world.create();
        assertFalse(set.contains(e));

        world.add(e, Position.class);
        assertTrue(set.contains(e));

        world.add(e, Frozen.class);
        assertFalse(set.contains(e));

        world.remove(e, Frozen.class);
        assertTrue(set.contains(e));

        world.destroy(e);
        assertFalse(set.contains(e));
        assertEquals(0, set.size());
    }

    @Test
    void setCreatedLaterContainsExistingEntities() {
        int e = world.create();
        world.add(e, Health.class);
        EntitySet set = world.entities(Family.all(Health.class));
        assertTrue(set.contains(e));
    }

    @Test
    void equalFamiliesShareOneSet() {
        EntitySet a = world.entities(Family.all(Position.class, Velocity.class));
        EntitySet b = world.entities(Family.all(Velocity.class).with(Position.class));
        assertSame(a, b);
    }

    @Test
    void everythingContainsEmptyEntities() {
        EntitySet all = world.entities(Family.everything());
        int e = world.create();
        assertTrue(all.contains(e));
    }

    @Test
    void familyWithUnregisteredComponentIsRejected() {
        final class Unknown implements Component {
            @Override
            public void reset() {}
        }
        assertThrows(
                IllegalArgumentException.class, () -> world.entities(Family.all(Unknown.class)));
    }

    @Test
    void listenersSeeAddAndRemoveAndCanStillReadComponents() {
        EntitySet set = world.entities(Family.all(Position.class));
        ComponentMapper<Position> positions = world.mapper(Position.class);
        List<String> log = new ArrayList<>();
        set.addListener(
                new EntitySet.Listener() {
                    @Override
                    public void added(int entity) {
                        log.add("+" + positions.get(entity).x);
                    }

                    @Override
                    public void removed(int entity) {
                        log.add("-" + positions.get(entity).x);
                    }
                });

        int e = world.create();
        world.add(e, Position.class).set(5, 0);
        assertTrue(log.isEmpty(), "вне update синхронизация ленивая");
        assertEquals(1, set.size());
        assertEquals(List.of("+5"), log, "подписчик видит уже заполненный компонент");

        world.remove(e, Position.class);
        assertFalse(set.contains(e));
        assertEquals(List.of("+5", "-5"), log, "при удалении компонент ещё доступен");
    }
}
