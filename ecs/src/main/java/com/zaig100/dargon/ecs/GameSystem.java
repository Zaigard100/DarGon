package com.zaig100.dargon.ecs;

/**
 * Система — логика, которая каждый тик обрабатывает данные мира.
 *
 * <p>Системы выполняются в порядке регистрации в {@link WorldBuilder}. После каждой системы мир
 * применяет отложенные изменения (состав сущностей, удаления) и доставляет опубликованные события.
 *
 * <p>Поля типа {@code ComponentMapper<T>}, ресурсов мира, других систем, {@link World} и {@link
 * EventBus} заполняются автоматически при сборке мира, если они не {@code final}, не {@code static}
 * и ещё равны {@code null}.
 */
public abstract class GameSystem {

    /** Мир, которому принадлежит система. Доступен начиная с {@link #initialize()}. */
    protected World world;

    /** Шина событий мира. Доступна начиная с {@link #initialize()}. */
    protected EventBus events;

    private boolean enabled = true;

    void bind(World world) {
        this.world = world;
        this.events = world.events();
    }

    /** Вызывается один раз после сборки мира и внедрения зависимостей. */
    protected void initialize() {}

    /** Один тик системы. Вызывается только миром. */
    protected abstract void update(float delta);

    /** Вызывается при {@link World#dispose()} в порядке, обратном регистрации. */
    protected void dispose() {}

    public boolean isEnabled() {
        return enabled;
    }

    /** Выключенная система пропускается в {@link World#update}. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
