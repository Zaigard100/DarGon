package com.zaig100.dargon.ecs;

/**
 * Система, которая обрабатывает каждую сущность своего {@link Family}.
 *
 * <p>Внутри {@link #process} можно свободно удалять сущности и менять их состав — набор изменится
 * только после окончания системы, поэтому каждая сущность будет обработана ровно один раз.
 */
public abstract class IteratingSystem extends GameSystem {

    private final Family family;

    /** Сущности системы. Доступны начиная с {@link #initialize()}. */
    protected EntitySet entities;

    protected IteratingSystem(Family family) {
        this.family = family;
    }

    @Override
    void bind(World world) {
        super.bind(world);
        entities = world.entities(family);
    }

    @Override
    protected void update(float delta) {
        begin(delta);
        for (int i = 0; i < entities.size(); i++) {
            process(entities.get(i), delta);
        }
        end(delta);
    }

    /** Перед обходом сущностей. */
    protected void begin(float delta) {}

    /** Обработка одной сущности. */
    protected abstract void process(int entity, float delta);

    /** После обхода сущностей. */
    protected void end(float delta) {}

    public Family family() {
        return family;
    }
}
