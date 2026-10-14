package com.zaig100.dargon.game.script;

import com.zaig100.dargon.ecs.Entity;
import com.zaig100.dargon.ecs.World;
import com.zaig100.dargon.game.component.EntityId;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.script.ScriptException;
import com.zaig100.dargon.script.ScriptObject;
import com.zaig100.dargon.script.Value;
import com.zaig100.dargon.script.Values;

/**
 * Сущность в скрипте. Хранит идентификатор с поколением: обращение к удалённой сущности даёт
 * понятную ошибку, а не чужой объект.
 *
 * <p>Поля: {@code x}, {@code y}, {@code id}, {@code enabled} (запись — тоже). Методы: {@code
 * enable()}, {@code disable()}, {@code remove()}, {@code moveTo(x, y)}.
 */
final class EntityHandle implements ScriptObject {

    private final World world;
    private final int entity;
    private final Runnable onChange;

    /**
     * @param onChange вызывается, когда скрипт меняет состав или положение сущности (проходимость
     *     могла измениться)
     */
    EntityHandle(World world, int entity, Runnable onChange) {
        this.world = world;
        this.entity = entity;
        this.onChange = onChange;
    }

    int entity() {
        return entity;
    }

    @Override
    public Value getMember(String name) {
        alive();
        switch (name) {
            case "x":
                return Values.of(pos().x);
            case "y":
                return Values.of(pos().y);
            case "id":
                EntityId id = world.mapper(EntityId.class).getOrNull(entity);
                return id == null ? Values.nil() : Values.of(id.id);
            case "enabled":
                return Values.of(!world.has(entity, Marker.Disabled.class));
            default:
                return ScriptObject.super.getMember(name);
        }
    }

    @Override
    public void setMember(String name, Value value) {
        alive();
        if (name.equals("enabled")) {
            setEnabled(value.isTruthy());
        } else {
            ScriptObject.super.setMember(name, value);
        }
    }

    @Override
    public Value callMethod(String name, Value[] args) {
        alive();
        switch (name) {
            case "enable":
                arity(name, args, 0);
                setEnabled(true);
                return Values.nil();
            case "disable":
                arity(name, args, 0);
                setEnabled(false);
                return Values.nil();
            case "remove":
                arity(name, args, 0);
                world.destroy(entity);
                onChange.run();
                return Values.nil();
            case "moveTo":
                arity(name, args, 2);
                pos().set(Values.integer(name, args, 0), Values.integer(name, args, 1));
                Motion motion = world.mapper(Motion.class).getOrNull(entity);
                if (motion != null) {
                    motion.snap();
                }
                onChange.run();
                return Values.nil();
            default:
                return ScriptObject.super.callMethod(name, args);
        }
    }

    private void setEnabled(boolean enabled) {
        if (enabled) {
            world.remove(entity, Marker.Disabled.class);
        } else if (!world.has(entity, Marker.Disabled.class)) {
            world.add(entity, Marker.Disabled.class);
        }
        onChange.run();
    }

    private GridPos pos() {
        GridPos pos = world.mapper(GridPos.class).getOrNull(entity);
        if (pos == null) {
            throw new ScriptException("у сущности " + display() + " нет положения");
        }
        return pos;
    }

    private void alive() {
        if (!world.isAlive(entity)) {
            throw new ScriptException("сущность " + Entity.toString(entity) + " уже удалена");
        }
    }

    private static void arity(String name, Value[] args, int expected) {
        if (args.length != expected) {
            throw new ScriptException(
                    name + "() ожидает аргументов: " + expected + ", передано " + args.length);
        }
    }

    @Override
    public String typeName() {
        return "entity";
    }

    @Override
    public String display() {
        EntityId id = world.isAlive(entity) ? world.mapper(EntityId.class).getOrNull(entity) : null;
        return id == null ? "<entity " + Entity.toString(entity) + ">" : "<entity " + id.id + ">";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EntityHandle h && h.entity == entity && h.world == world;
    }

    @Override
    public int hashCode() {
        return entity;
    }
}
