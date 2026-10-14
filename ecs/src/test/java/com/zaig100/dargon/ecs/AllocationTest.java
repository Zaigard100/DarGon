package com.zaig100.dargon.ecs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.zaig100.dargon.ecs.TestComponents.Frozen;
import com.zaig100.dargon.ecs.TestComponents.Health;
import com.zaig100.dargon.ecs.TestComponents.Position;
import com.zaig100.dargon.ecs.TestComponents.Velocity;
import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.Test;

/** Горячий цикл мира не должен выделять память: движение, смена состава, пересоздание сущностей. */
class AllocationTest {

    static final class Move extends IteratingSystem {
        private ComponentMapper<Position> positions;
        private ComponentMapper<Velocity> velocities;

        Move() {
            super(Family.all(Position.class, Velocity.class).exclude(Frozen.class));
        }

        @Override
        protected void process(int entity, float delta) {
            Position p = positions.get(entity);
            Velocity v = velocities.get(entity);
            p.x += v.dx;
            p.y += v.dy;
        }
    }

    /** Каждый тик то замораживает, то размораживает сущности (компоненты ходят через пул). */
    static final class Toggle extends IteratingSystem {
        private ComponentMapper<Frozen> frozen;
        private int tick;

        Toggle() {
            super(Family.all(Position.class));
        }

        @Override
        protected void begin(float delta) {
            tick++;
        }

        @Override
        protected void process(int entity, float delta) {
            if ((tick & 1) == 0) {
                frozen.create(entity);
            } else {
                frozen.remove(entity);
            }
        }
    }

    /** Убивает и пересоздаёт сущности со здоровьем (слоты и компоненты переиспользуются). */
    static final class Respawn extends IteratingSystem {
        private ComponentMapper<Health> health;

        Respawn() {
            super(Family.all(Health.class));
        }

        @Override
        protected void process(int entity, float delta) {
            world.destroy(entity);
            int fresh = world.create();
            health.create(fresh).hp = 3;
        }
    }

    @Test
    void steadyStateUpdateDoesNotAllocate() {
        com.sun.management.ThreadMXBean threads = threadBean();
        assumeTrue(threads != null && threads.isThreadAllocatedMemorySupported());

        World world =
                TestComponents.builder()
                        .system(new Move())
                        .system(new Toggle())
                        .system(new Respawn())
                        .initialCapacity(4096)
                        .build();
        for (int i = 0; i < 1_000; i++) {
            int e = world.create();
            world.add(e, Position.class);
            world.add(e, Velocity.class).dx = 1;
            world.add(world.create(), Health.class).hp = 3;
        }

        for (int i = 0; i < 2_000; i++) {
            world.update(1f / 60f); // прогрев: JIT, рост массивов, заполнение пулов
        }
        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < 1_000; i++) {
            world.update(1f / 60f);
        }
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;

        assertTrue(
                allocated < 4_096,
                "за 1000 тиков выделено " + allocated + " байт, ожидалось почти ноль");
    }

    private static com.sun.management.ThreadMXBean threadBean() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean instanceof com.sun.management.ThreadMXBean sun ? sun : null;
    }
}
