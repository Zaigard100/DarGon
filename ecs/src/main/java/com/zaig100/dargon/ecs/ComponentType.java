package com.zaig100.dargon.ecs;

import java.util.function.Supplier;

/** Зарегистрированный тип компонента: порядковый индекс (номер бита в составе) и фабрика. */
final class ComponentType<T extends Component> {

    final int index;
    final Class<T> type;
    final Supplier<T> factory;

    ComponentType(int index, Class<T> type, Supplier<T> factory) {
        this.index = index;
        this.type = type;
        this.factory = factory;
    }

    T create() {
        T component = factory.get();
        if (component == null || component.getClass() != type) {
            throw new IllegalStateException(
                    "Фабрика компонента " + type.getSimpleName() + " вернула неверный объект");
        }
        return component;
    }
}
