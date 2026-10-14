package com.zaig100.dargon.game.level;

import com.zaig100.dargon.ecs.Component;
import com.zaig100.dargon.level.ComponentData;
import com.zaig100.dargon.level.EntitySpec;
import com.zaig100.dargon.level.Prefab;
import com.zaig100.dargon.level.PrefabLibrary;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Имя компонента в префабе → класс компонента и функция, заполняющая его из данных. Добавить новый
 * компонент для префабов = зарегистрировать его здесь; загрузчик менять не нужно.
 */
public final class ComponentRegistry {

    /** Заполняет компонент из данных префаба (с подставленными параметрами). */
    @FunctionalInterface
    public interface Loader<T extends Component> {
        void load(T component, ComponentData data, EntitySpec entity);
    }

    /** Зарегистрированный компонент. */
    record Entry<T extends Component>(Class<T> type, Loader<T> loader) {}

    private final Map<String, Entry<?>> entries = new HashMap<>();

    /** Регистрирует компонент под именем, которое используется в префабах. */
    public <T extends Component> ComponentRegistry register(
            String name, Class<T> type, Loader<T> loader) {
        if (entries.putIfAbsent(name, new Entry<>(type, loader)) != null) {
            throw new IllegalArgumentException("Компонент уже зарегистрирован: " + name);
        }
        return this;
    }

    /** Компонент-метка без данных: создаётся, но не заполняется. */
    public <T extends Component> ComponentRegistry register(String name, Class<T> type) {
        return register(name, type, (component, data, entity) -> {});
    }

    Entry<?> get(String name) {
        return entries.get(name);
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    /** Компоненты, которые встречаются в префабах, но не зарегистрированы. */
    public Set<String> missing(PrefabLibrary prefabs) {
        Set<String> missing = new TreeSet<>();
        for (Prefab prefab : prefabs.all()) {
            for (String name : prefab.components().keySet()) {
                if (!entries.containsKey(name)) {
                    missing.add(name);
                }
            }
        }
        return missing;
    }
}
