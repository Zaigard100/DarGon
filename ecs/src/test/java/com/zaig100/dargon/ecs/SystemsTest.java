package com.zaig100.dargon.ecs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.ecs.TestComponents.Frozen;
import com.zaig100.dargon.ecs.TestComponents.Health;
import com.zaig100.dargon.ecs.TestComponents.Position;
import com.zaig100.dargon.ecs.TestComponents.Velocity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SystemsTest {

    /** Удаляет каждую сущность, которую обрабатывает, и запоминает, кого обработал. */
    static final class DestroyAll extends IteratingSystem {
        final List<Integer> processed = new ArrayList<>();

        DestroyAll() {
            super(Family.all(Health.class));
        }

        @Override
        protected void process(int entity, float delta) {
            processed.add(entity);
            world.destroy(entity);
        }
    }

    @Test
    void destroyingDuringIterationProcessesEveryEntityExactlyOnce() {
        // Регрессия бага DarGon3, где при удалении во время обхода пропускался каждый второй
        // объект.
        DestroyAll system = new DestroyAll();
        World world = TestComponents.builder().system(system).build();
        Set<Integer> created = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            int e = world.create();
            world.add(e, Health.class);
            created.add(e);
        }

        world.update(1f);

        assertEquals(100, system.processed.size());
        assertEquals(created, new HashSet<>(system.processed));
        assertEquals(0, world.entityCount());
    }

    /** Убирает Velocity и добавляет Frozen во время обхода. */
    static final class Freeze extends IteratingSystem {
        int processed;

        Freeze() {
            super(Family.all(Velocity.class));
        }

        @Override
        protected void process(int entity, float delta) {
            processed++;
            world.remove(entity, Velocity.class);
            world.add(entity, Frozen.class);
            // До точки синхронизации удалённый компонент ещё читается.
            assertNotNull(world.mapper(Velocity.class).getOrNull(entity));
        }
    }

    @Test
    void compositionChangesApplyAfterSystem() {
        Freeze freeze = new Freeze();
        World world = TestComponents.builder().system(freeze).build();
        EntitySet frozen = world.entities(Family.all(Frozen.class));
        for (int i = 0; i < 10; i++) {
            world.add(world.create(), Velocity.class);
        }

        world.update(1f);
        assertEquals(10, freeze.processed);
        assertEquals(10, frozen.size());

        world.update(1f);
        assertEquals(10, freeze.processed, "без Velocity сущности больше не попадают в систему");
    }

    static final class Recorder extends GameSystem {
        private final String name;
        private final List<String> log;

        Recorder(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        protected void update(float delta) {
            log.add(name);
        }
    }

    static final class OtherRecorder extends GameSystem {
        private final List<String> log;

        OtherRecorder(List<String> log) {
            this.log = log;
        }

        @Override
        protected void update(float delta) {
            log.add("other");
        }
    }

    @Test
    void systemsRunInRegistrationOrderAndDisabledAreSkipped() {
        List<String> log = new ArrayList<>();
        Recorder first = new Recorder("first", log);
        OtherRecorder second = new OtherRecorder(log);
        World world = TestComponents.builder().system(second).system(first).build();

        world.update(1f);
        assertEquals(List.of("other", "first"), log);

        second.setEnabled(false);
        world.update(1f);
        assertEquals(List.of("other", "first", "first"), log);
    }

    static final class Counter extends IntervalSystem {
        int ticks;

        Counter() {
            super(0.5f);
        }

        @Override
        protected void tick() {
            ticks++;
        }
    }

    @Test
    void intervalSystemDoesNotLoseTime() {
        Counter counter = new Counter();
        World world = TestComponents.builder().system(counter).build();
        world.update(0.3f);
        assertEquals(0, counter.ticks);
        world.update(0.3f);
        assertEquals(1, counter.ticks);
        world.update(1.0f);
        assertEquals(3, counter.ticks);
    }

    static final class Grid {}

    static final class Injected extends GameSystem {
        ComponentMapper<Position> positions;
        Grid grid;
        Counter counter;
        boolean initializedWithDependencies;

        @Override
        protected void initialize() {
            initializedWithDependencies =
                    positions != null && grid != null && counter != null && world != null;
        }

        @Override
        protected void update(float delta) {}
    }

    @Test
    void dependenciesAreInjectedBeforeInitialize() {
        Grid grid = new Grid();
        Counter counter = new Counter();
        Injected injected = new Injected();
        World world =
                TestComponents.builder().resource(grid).system(counter).system(injected).build();

        assertTrue(injected.initializedWithDependencies);
        assertSame(grid, injected.grid);
        assertSame(counter, injected.counter);
        assertSame(world.mapper(Position.class), injected.positions);
        assertSame(grid, world.resource(Grid.class));
        assertSame(injected, world.system(Injected.class));
    }

    @Test
    void disposeRunsInReverseOrder() {
        List<String> log = new ArrayList<>();
        GameSystem a =
                new GameSystem() {
                    @Override
                    protected void update(float delta) {}

                    @Override
                    protected void dispose() {
                        log.add("a");
                    }
                };
        GameSystem b =
                new GameSystem() {
                    @Override
                    protected void update(float delta) {}

                    @Override
                    protected void dispose() {
                        log.add("b");
                    }
                };
        World world = TestComponents.builder().system(a).system(b).build();
        world.dispose();
        assertEquals(List.of("b", "a"), log);
        assertFalse(a.isEnabled() && log.isEmpty());
    }
}
