package com.zaig100.dargon.ecs;

/**
 * Быстрый доступ к компонентам одного типа. Получается через {@link World#mapper} или инжектируется
 * в поле системы типа {@code ComponentMapper<T>}.
 *
 * <p>Все методы проверяют поколение сущности: для устаревшего идентификатора {@link #has} вернёт
 * {@code false}, а {@link #get} — бросит исключение, вместо того чтобы молча вернуть компонент
 * чужой сущности.
 */
public final class ComponentMapper<T extends Component> {

    private final World world;
    private final ComponentType<T> type;
    private final ComponentStore<T> store;

    ComponentMapper(World world, ComponentType<T> type, ComponentStore<T> store) {
        this.world = world;
        this.type = type;
        this.store = store;
    }

    /** Есть ли у сущности этот компонент. */
    public boolean has(int entity) {
        return world.hasComponent(entity, type.index);
    }

    /**
     * Компонент сущности.
     *
     * @throws IllegalStateException если сущность мертва или компонента нет
     */
    public T get(int entity) {
        T component = getOrNull(entity);
        if (component == null) {
            throw new IllegalStateException(
                    "У сущности " + Entity.toString(entity) + " нет " + type.type.getSimpleName());
        }
        return component;
    }

    /**
     * Компонент сущности или {@code null}. Компонент, удалённый в текущей системе, остаётся
     * доступным до точки синхронизации мира.
     */
    public T getOrNull(int entity) {
        return world.isCurrent(entity) ? store.get(Entity.index(entity)) : null;
    }

    /** Добавляет компонент (или возвращает существующий). То же, что {@link World#add}. */
    public T create(int entity) {
        return world.add(entity, type);
    }

    /** Удаляет компонент. То же, что {@link World#remove}. */
    public void remove(int entity) {
        world.remove(entity, type);
    }

    public Class<T> type() {
        return type.type;
    }
}
