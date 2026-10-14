package com.zaig100.dargon.ecs;

import com.zaig100.dargon.ecs.TestComponents.Frozen;
import com.zaig100.dargon.ecs.TestComponents.Health;
import com.zaig100.dargon.ecs.TestComponents.Position;
import com.zaig100.dargon.ecs.TestComponents.Velocity;
import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Грубый микро-бенчмарк ECS. Запуск: {@code ./gradlew :ecs:benchmark}.
 *
 * <p>Цель — заметить регрессии на порядки, а не мерить наносекунды: для точных замеров нужен JMH.
 */
@Tag("benchmark")
@SuppressWarnings("checkstyle:RegexpSinglelineJava") // результаты печатаются в консоль
class EcsBenchmark {

    private static final int WARMUP_TICKS = 2_000;
    private static final int MEASURED_TICKS = 2_000;

    @Test
    void iterateTwoComponents() {
        World world =
                TestComponents.builder()
                        .system(new AllocationTest.Move())
                        .initialCapacity(20_000)
                        .build();
        int count = 10_000;
        for (int i = 0; i < count; i++) {
            int e = world.create();
            world.add(e, Position.class);
            world.add(e, Velocity.class).dx = 1;
        }
        report("Обход 2 компонентов", count, measure(world));
    }

    @Test
    void compositionChurn() {
        World world =
                TestComponents.builder()
                        .system(new AllocationTest.Move())
                        .system(new AllocationTest.Toggle())
                        .initialCapacity(20_000)
                        .build();
        int count = 10_000;
        for (int i = 0; i < count; i++) {
            int e = world.create();
            world.add(e, Position.class);
            world.add(e, Velocity.class).dx = 1;
        }
        world.entities(Family.all(Frozen.class));
        report("Добавление/удаление компонента у всех", count, measure(world));
    }

    @Test
    void entityChurn() {
        World world =
                TestComponents.builder()
                        .system(new AllocationTest.Respawn())
                        .initialCapacity(20_000)
                        .build();
        int count = 10_000;
        for (int i = 0; i < count; i++) {
            world.add(world.create(), Health.class).hp = 1;
        }
        report("Удаление и создание всех сущностей", count, measure(world));
    }

    private static double measure(World world) {
        for (int i = 0; i < WARMUP_TICKS; i++) {
            world.update(1f / 60f);
        }
        long start = System.nanoTime();
        for (int i = 0; i < MEASURED_TICKS; i++) {
            world.update(1f / 60f);
        }
        return (System.nanoTime() - start) / (double) MEASURED_TICKS;
    }

    private static void report(String name, int entities, double nanosPerTick) {
        System.out.printf(
                Locale.ROOT,
                "%-42s %,6d сущностей: %8.1f мкс/тик, %6.1f нс/сущность%n",
                name,
                entities,
                nanosPerTick / 1_000.0,
                nanosPerTick / entities);
    }
}
