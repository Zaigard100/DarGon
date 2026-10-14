package com.zaig100.dargon.ecs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.ecs.TestComponents.Health;
import com.zaig100.dargon.ecs.TestComponents.Position;
import org.junit.jupiter.api.Test;

class EntityLifecycleTest {

    private final World world = TestComponents.builder().build();

    @Test
    void createdEntityIsAliveAndCounted() {
        int a = world.create();
        int b = world.create();
        assertTrue(world.isAlive(a));
        assertTrue(world.isAlive(b));
        assertNotEquals(a, b);
        assertEquals(2, world.entityCount());
    }

    @Test
    void destroyedSlotIsReusedWithNewGeneration() {
        int first = world.create();
        world.destroy(first);
        int second = world.create();

        assertEquals(Entity.index(first), Entity.index(second), "слот должен переиспользоваться");
        assertNotEquals(Entity.generation(first), Entity.generation(second));
        assertFalse(world.isAlive(first), "устаревший идентификатор не должен считаться живым");
        assertTrue(world.isAlive(second));
        assertEquals(1, world.entityCount());
    }

    @Test
    void staleIdentifierDoesNotSeeComponentsOfNewOwner() {
        int stale = world.create();
        world.destroy(stale);
        int fresh = world.create();
        world.add(fresh, Position.class).set(3, 4);

        ComponentMapper<Position> positions = world.mapper(Position.class);
        assertFalse(positions.has(stale));
        assertNull(positions.getOrNull(stale));
        assertThrows(IllegalStateException.class, () -> positions.get(stale));
        assertEquals(3, positions.get(fresh).x);
    }

    @Test
    void destroyIsIdempotent() {
        int e = world.create();
        assertTrue(world.destroy(e));
        assertFalse(world.destroy(e));
        assertEquals(0, world.entityCount());
    }

    @Test
    void addReturnsSameComponentWhenAlreadyPresent() {
        int e = world.create();
        Position p = world.add(e, Position.class).set(1, 2);
        assertSame(p, world.add(e, Position.class));
        assertEquals(1, world.get(e, Position.class).x);
    }

    @Test
    void addToDeadEntityFails() {
        int e = world.create();
        world.destroy(e);
        assertThrows(IllegalStateException.class, () -> world.add(e, Position.class));
    }

    @Test
    void unregisteredComponentIsRejected() {
        final class Unknown implements Component {
            @Override
            public void reset() {}
        }
        int e = world.create();
        assertThrows(IllegalArgumentException.class, () -> world.add(e, Unknown.class));
    }

    @Test
    void removedComponentsReturnToPoolResetAndAreReused() {
        int e = world.create();
        Health h = world.add(e, Health.class);
        h.hp = 7;
        world.remove(e, Health.class);

        assertEquals(1, world.pooled(Health.class));
        assertEquals(0, h.hp, "компонент должен быть сброшен при возврате в пул");
        assertFalse(world.has(e, Health.class));

        int other = world.create();
        assertSame(h, world.add(other, Health.class), "компонент должен взяться из пула");
        assertEquals(0, world.pooled(Health.class));
    }

    @Test
    void destroyingEntityReturnsAllItsComponentsToPool() {
        int e = world.create();
        world.add(e, Health.class).hp = 3;
        world.add(e, Position.class);
        world.destroy(e);
        assertEquals(1, world.pooled(Health.class));
        assertEquals(1, world.pooled(Position.class));
    }

    @Test
    void manyEntitiesGrowStorage() {
        int[] ids = new int[5_000];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = world.create();
            world.add(ids[i], Position.class).set(i, -i);
        }
        for (int i = 0; i < ids.length; i++) {
            assertEquals(i, world.get(ids[i], Position.class).x);
        }
        assertEquals(ids.length, world.entityCount());
    }
}
