package com.zaig100.dargon.ecs;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Описание выборки сущностей по составу компонентов.
 *
 * <ul>
 *   <li>{@code all} — сущность должна иметь все перечисленные компоненты;
 *   <li>{@code one} — хотя бы один из перечисленных (если список не пуст);
 *   <li>{@code exclude} — ни одного из перечисленных.
 * </ul>
 *
 * <pre>{@code
 * Family.all(GridPos.class, Hazard.class).exclude(Disabled.class)
 * Family.one(Pickup.class, Container.class)
 * }</pre>
 *
 * <p>Неизменяемый объект; одинаковые описания равны, и мир держит для них один общий {@link
 * EntitySet}.
 */
public final class Family {

    private static final Family EVERYTHING = new Family(Set.of(), Set.of(), Set.of());

    private final Set<Class<? extends Component>> all;
    private final Set<Class<? extends Component>> one;
    private final Set<Class<? extends Component>> exclude;

    private Family(
            Set<Class<? extends Component>> all,
            Set<Class<? extends Component>> one,
            Set<Class<? extends Component>> exclude) {
        this.all = all;
        this.one = one;
        this.exclude = exclude;
    }

    /** Все живые сущности. */
    public static Family everything() {
        return EVERYTHING;
    }

    @SafeVarargs
    public static Family all(Class<? extends Component>... types) {
        return EVERYTHING.with(types);
    }

    @SafeVarargs
    public static Family one(Class<? extends Component>... types) {
        return EVERYTHING.withOne(types);
    }

    /** Добавляет обязательные компоненты. */
    @SafeVarargs
    public final Family with(Class<? extends Component>... types) {
        Set<Class<? extends Component>> result = new LinkedHashSet<>(all);
        for (Class<? extends Component> type : types) {
            result.add(type);
        }
        return new Family(Collections.unmodifiableSet(result), one, exclude);
    }

    /** Добавляет компоненты в группу «хотя бы один из». */
    @SafeVarargs
    public final Family withOne(Class<? extends Component>... types) {
        Set<Class<? extends Component>> result = new LinkedHashSet<>(one);
        for (Class<? extends Component> type : types) {
            result.add(type);
        }
        return new Family(all, Collections.unmodifiableSet(result), exclude);
    }

    /** Добавляет запрещённые компоненты. */
    @SafeVarargs
    public final Family exclude(Class<? extends Component>... types) {
        Set<Class<? extends Component>> result = new LinkedHashSet<>(exclude);
        for (Class<? extends Component> type : types) {
            result.add(type);
        }
        return new Family(all, one, Collections.unmodifiableSet(result));
    }

    Set<Class<? extends Component>> all() {
        return all;
    }

    Set<Class<? extends Component>> one() {
        return one;
    }

    Set<Class<? extends Component>> excluded() {
        return exclude;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Family other
                && all.equals(other.all)
                && one.equals(other.one)
                && exclude.equals(other.exclude);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * all.hashCode() + one.hashCode()) + exclude.hashCode();
    }

    @Override
    public String toString() {
        return "Family{all="
                + names(all)
                + ", one="
                + names(one)
                + ", exclude="
                + names(exclude)
                + "}";
    }

    private static String names(Set<Class<? extends Component>> types) {
        StringBuilder sb = new StringBuilder("[");
        for (Class<? extends Component> type : types) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(type.getSimpleName());
        }
        return sb.append(']').toString();
    }
}
