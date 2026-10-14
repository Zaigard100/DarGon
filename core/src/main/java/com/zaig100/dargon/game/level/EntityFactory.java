package com.zaig100.dargon.game.level;

import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.ecs.Component;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.level.ComponentData;
import com.zaig100.dargon.level.EntitySpec;
import com.zaig100.dargon.level.JsonValues;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.level.LevelFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Создаёт сущности ECS по описанию уровня: для каждого компонента префаба находит загрузчик в
 * {@link ComponentRegistry}. Общие для всех сущностей вещи (позиция, id, теги, выключенность,
 * обработчики) добавляет {@link Decorator}.
 */
public final class EntityFactory {

    /** Добавляет сущности то, что не описано компонентами префаба. */
    @FunctionalInterface
    public interface Decorator {
        void decorate(World world, int entity, EntitySpec spec);
    }

    private final ComponentRegistry registry;
    private final List<Decorator> decorators = new ArrayList<>();

    public EntityFactory(ComponentRegistry registry) {
        this.registry = registry;
    }

    public EntityFactory decorator(Decorator decorator) {
        decorators.add(decorator);
        return this;
    }

    /** Создаёт все сущности уровня в порядке файла. Возвращает их идентификаторы. */
    public int[] spawnAll(World world, LevelDefinition level) {
        int[] result = new int[level.entities().size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = spawn(world, level.id(), level.entities().get(i));
        }
        return result;
    }

    /**
     * Создаёт одну сущность.
     *
     * @param file уровень — для сообщений об ошибках
     * @throws LevelFormatException если компонент не зарегистрирован или данные некорректны;
     *     частично созданная сущность удаляется
     */
    public int spawn(World world, String file, EntitySpec spec) {
        int entity = world.create();
        try {
            for (Map.Entry<String, JsonValue> component : spec.components().entrySet()) {
                String path =
                        JsonValues.child(
                                JsonValues.child(spec.path(), "components"), component.getKey());
                ComponentRegistry.Entry<?> entry = registry.get(component.getKey());
                if (entry == null) {
                    throw new LevelFormatException(
                            file,
                            path,
                            "компонент '"
                                    + component.getKey()
                                    + "' из префаба '"
                                    + spec.prefab().id()
                                    + "' не зарегистрирован в игре");
                }
                load(
                        world,
                        entity,
                        entry,
                        new ComponentData(component.getValue(), file, path),
                        spec);
            }
            for (Decorator decorator : decorators) {
                decorator.decorate(world, entity, spec);
            }
        } catch (RuntimeException e) {
            world.destroy(entity);
            throw e;
        }
        return entity;
    }

    private static <T extends Component> void load(
            World world,
            int entity,
            ComponentRegistry.Entry<T> entry,
            ComponentData data,
            EntitySpec spec) {
        T component = world.add(entity, entry.type());
        entry.loader().load(component, data, spec);
    }
}
