package com.zaig100.dargon.ecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Сборка {@link World}.
 *
 * <pre>{@code
 * World world = new WorldBuilder()
 *         .component(GridPos.class, GridPos::new)
 *         .component(Hazard.class, Hazard::new)
 *         .system(new GridMovementSystem())
 *         .system(new HazardSystem())
 *         .resource(new GridIndex(w, h))
 *         .build();
 * }</pre>
 *
 * <p>Системы выполняются в порядке добавления.
 */
public final class WorldBuilder {

    private final Map<Class<?>, ComponentType<?>> types = new LinkedHashMap<>();
    private final List<GameSystem> systems = new ArrayList<>();
    private final Map<Class<?>, Object> resources = new HashMap<>();
    private int initialCapacity = 256;

    /** Регистрирует тип компонента и фабрику для новых экземпляров. */
    public <T extends Component> WorldBuilder component(Class<T> type, Supplier<T> factory) {
        if (types.containsKey(type)) {
            throw new IllegalArgumentException(
                    "Компонент уже зарегистрирован: " + type.getSimpleName());
        }
        types.put(type, new ComponentType<>(types.size(), type, factory));
        return this;
    }

    /** Добавляет систему в конец порядка выполнения. */
    public WorldBuilder system(GameSystem system) {
        systems.add(system);
        return this;
    }

    /** Регистрирует ресурс под ключом — его собственным классом. */
    public WorldBuilder resource(Object resource) {
        return putResource(resource.getClass(), resource);
    }

    /** Регистрирует ресурс под явным ключом (например, интерфейсом). */
    public <R> WorldBuilder resource(Class<R> key, R resource) {
        return putResource(key, resource);
    }

    /** Начальный размер массивов под сущности (дальше растут сами). */
    public WorldBuilder initialCapacity(int capacity) {
        this.initialCapacity = capacity;
        return this;
    }

    public World build() {
        return new World(new ArrayList<>(types.values()), systems, resources, initialCapacity);
    }

    private WorldBuilder putResource(Class<?> key, Object resource) {
        if (resources.put(key, resource) != null) {
            throw new IllegalArgumentException("Ресурс уже зарегистрирован: " + key.getName());
        }
        return this;
    }
}
