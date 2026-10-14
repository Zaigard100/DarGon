package com.zaig100.dargon.ecs;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Мир ECS: сущности, их компоненты, системы, ресурсы и события. Создаётся через {@link
 * WorldBuilder}.
 *
 * <h2>Когда применяются изменения</h2>
 *
 * <ul>
 *   <li>Добавленный компонент доступен сразу ({@link #add} возвращает его для заполнения).
 *   <li>Членство в {@link EntitySet} (а значит и в системах), удаление компонентов из хранилищ и
 *       удаление сущностей применяются в <b>точке синхронизации</b>: после каждой системы внутри
 *       {@link #update}. До неё удалённый компонент и компоненты удалённой сущности остаются
 *       доступны для чтения.
 *   <li>Вне {@link #update} (например, при загрузке уровня) синхронизация ленивая: она происходит
 *       при чтении {@link EntitySet}, при {@link #create()}, {@link #entityCount()}, {@link
 *       #sync()} и в начале {@link #update}. Поэтому сущность можно создать, добавить компоненты и
 *       заполнить их — подписчики наборов увидят уже заполненные данные. События, опубликованные
 *       вне {@code update}, тоже доставляются в ближайшей синхронизации.
 * </ul>
 *
 * <p>Благодаря этому системы могут удалять сущности и менять их состав прямо во время обхода, не
 * пропуская и не обрабатывая дважды ни одной сущности.
 *
 * <p>Мир не потокобезопасен: все вызовы — из одного (игрового) потока.
 */
public final class World {

    /** Защита от бесконечного цикла «синхронизация → событие → изменение → синхронизация». */
    private static final int MAX_SETTLE_ROUNDS = 1_000;

    private final ComponentType<?>[] types;
    private final Map<Class<?>, ComponentType<?>> typesByClass;
    private final ComponentStore<?>[] stores;
    private final ComponentMapper<?>[] mappers;
    private final int words;

    private int capacity;
    private int[] generations;
    private boolean[] alive;
    private boolean[] dying;
    private boolean[] dirtyFlags;
    private long[] composition;
    private long[] pendingRemoval;

    private final IntList freeIndices = new IntList();
    private int nextIndex;
    private int entityCount;
    private final IntList dirty = new IntList();

    private final List<EntitySet> sets = new ArrayList<>();
    private final Map<Family, EntitySet> setsByFamily = new HashMap<>();

    private final GameSystem[] systems;
    private final Map<Class<?>, GameSystem> systemsByClass = new HashMap<>();
    private final Map<Class<?>, Object> resources;
    private final EventBus events = new EventBus();

    private boolean processing;
    private boolean settling;
    private boolean disposed;

    World(
            List<ComponentType<?>> types,
            List<GameSystem> systems,
            Map<Class<?>, Object> resources,
            int initialCapacity) {
        this.types = types.toArray(new ComponentType<?>[0]);
        this.typesByClass = new HashMap<>();
        this.stores = new ComponentStore<?>[this.types.length];
        this.mappers = new ComponentMapper<?>[this.types.length];
        for (ComponentType<?> type : this.types) {
            typesByClass.put(type.type, type);
            stores[type.index] = createStore(type);
            mappers[type.index] = createMapper(type);
        }
        this.words = Math.max(1, (this.types.length + 63) >>> 6);
        this.resources = new HashMap<>(resources);
        allocate(Math.max(16, initialCapacity));

        this.systems = systems.toArray(new GameSystem[0]);
        for (GameSystem system : this.systems) {
            if (systemsByClass.put(system.getClass(), system) != null) {
                throw new IllegalArgumentException(
                        "Система " + system.getClass().getSimpleName() + " добавлена дважды");
            }
        }
        for (GameSystem system : this.systems) {
            system.bind(this);
            inject(system);
        }
        for (GameSystem system : this.systems) {
            system.initialize();
        }
    }

    // ------------------------------------------------------------------ сущности

    /** Создаёт пустую сущность и возвращает её идентификатор. */
    public int create() {
        checkNotDisposed();
        syncIfIdle(); // освобождает слоты удалённых сущностей и применяет изменения предыдущих
        int index;
        if (!freeIndices.isEmpty()) {
            index = freeIndices.pop();
        } else {
            if (nextIndex >= Entity.MAX_ENTITIES) {
                throw new IllegalStateException("Превышен лимит сущностей: " + Entity.MAX_ENTITIES);
            }
            index = nextIndex++;
            ensureCapacity(index + 1);
        }
        alive[index] = true;
        entityCount++;
        markDirty(index);
        return Entity.pack(index, generations[index]);
    }

    /**
     * Помечает сущность на удаление. Повторный вызов и вызов для устаревшего идентификатора ничего
     * не делают.
     *
     * @return {@code true}, если сущность была жива и теперь помечена
     */
    public boolean destroy(int entity) {
        if (!isAlive(entity)) {
            return false;
        }
        int index = Entity.index(entity);
        dying[index] = true;
        markDirty(index);
        return true;
    }

    /** Жива ли сущность: существует, поколение совпадает и она не помечена на удаление. */
    public boolean isAlive(int entity) {
        return isCurrent(entity) && !dying[Entity.index(entity)];
    }

    /** Число живых сущностей (включая помеченные на удаление до точки синхронизации). */
    public int entityCount() {
        syncIfIdle();
        return entityCount;
    }

    /**
     * Принудительная синхронизация вне {@link #update}: применяет отложенные изменения и доставляет
     * события. Внутри {@code update} синхронизация происходит только после систем, поэтому там
     * вызов запрещён.
     */
    public void sync() {
        checkNotDisposed();
        if (processing) {
            throw new IllegalStateException(
                    "Внутри update() синхронизация происходит автоматически после каждой системы");
        }
        settle();
    }

    // ------------------------------------------------------------------ компоненты

    /** Добавляет сущности компонент и возвращает его. Если компонент уже есть — возвращает его. */
    public <T extends Component> T add(int entity, Class<T> type) {
        return add(entity, typeOf(type));
    }

    /**
     * Удаляет компонент у сущности. Для мёртвой сущности или отсутствующего компонента — ничего.
     */
    public void remove(int entity, Class<? extends Component> type) {
        remove(entity, typeOf(type));
    }

    /** Есть ли у сущности компонент. */
    public boolean has(int entity, Class<? extends Component> type) {
        return hasComponent(entity, typeOf(type).index);
    }

    /**
     * Компонент сущности; бросает исключение, если его нет. Для частых обращений — {@link #mapper}.
     */
    public <T extends Component> T get(int entity, Class<T> type) {
        return mapper(type).get(entity);
    }

    /** Маппер для быстрого доступа к компонентам типа {@code type}. */
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentMapper<T> mapper(Class<T> type) {
        return (ComponentMapper<T>) mappers[typeOf(type).index];
    }

    // ------------------------------------------------------------------ выборки, системы, ресурсы

    /**
     * Живой набор сущностей для {@code family}. Для одинаковых описаний возвращается один объект.
     */
    public EntitySet entities(Family family) {
        syncIfIdle();
        EntitySet set = setsByFamily.get(family);
        if (set != null) {
            return set;
        }
        set =
                new EntitySet(
                        this,
                        family,
                        mask(family, family.all()),
                        mask(family, family.one()),
                        mask(family, family.excluded()));
        for (int index = 0; index < nextIndex; index++) {
            if (alive[index] && !dying[index] && set.matches(composition, index * words)) {
                set.insert(Entity.pack(index, generations[index]));
            }
        }
        sets.add(set);
        setsByFamily.put(family, set);
        return set;
    }

    /** Система по классу. */
    public <S extends GameSystem> S system(Class<S> type) {
        GameSystem system = systemsByClass.get(type);
        if (system == null) {
            throw new IllegalArgumentException("Система не зарегистрирована: " + type.getName());
        }
        return type.cast(system);
    }

    /** Ресурс мира по ключу (обычно — классу ресурса). */
    public <R> R resource(Class<R> key) {
        Object resource = resources.get(key);
        if (resource == null) {
            throw new IllegalArgumentException("Ресурс не зарегистрирован: " + key.getName());
        }
        return key.cast(resource);
    }

    public EventBus events() {
        return events;
    }

    /**
     * Заполняет поля объекта так же, как поля систем: {@code ComponentMapper<T>}, ресурсы, системы,
     * {@link World}, {@link EventBus}.
     */
    public void inject(Object target) {
        for (Class<?> c = target.getClass();
                c != null && c != Object.class;
                c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                    continue;
                }
                Object value = resolveInjection(field);
                if (value == null) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (field.get(target) == null) {
                        field.set(target, value);
                    }
                } catch (IllegalAccessException | RuntimeException e) {
                    throw new IllegalStateException("Не удалось заполнить поле " + field, e);
                }
            }
        }
    }

    // ------------------------------------------------------------------ тик

    /**
     * Один тик: системы по порядку, после каждой — точка синхронизации (применение изменений и
     * доставка событий).
     */
    public void update(float delta) {
        checkNotDisposed();
        if (processing) {
            throw new IllegalStateException("World.update() нельзя вызывать рекурсивно");
        }
        processing = true;
        try {
            settle();
            for (GameSystem system : systems) {
                if (system.isEnabled()) {
                    system.update(delta);
                    settle();
                }
            }
        } finally {
            processing = false;
        }
    }

    /** Освобождает системы (в обратном порядке). После этого мир использовать нельзя. */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (int i = systems.length - 1; i >= 0; i--) {
            systems[i].dispose();
        }
    }

    // ------------------------------------------------------------------ внутреннее

    boolean isCurrent(int entity) {
        if (entity < 0) {
            return false;
        }
        int index = Entity.index(entity);
        return index < nextIndex && alive[index] && generations[index] == Entity.generation(entity);
    }

    boolean hasComponent(int entity, int typeIndex) {
        if (!isCurrent(entity)) {
            return false;
        }
        int word = Entity.index(entity) * words + (typeIndex >>> 6);
        return (composition[word] & (1L << (typeIndex & 63))) != 0;
    }

    <T extends Component> T add(int entity, ComponentType<T> type) {
        if (!isAlive(entity)) {
            throw new IllegalStateException(
                    "Нельзя добавить "
                            + type.type.getSimpleName()
                            + ": сущность "
                            + Entity.toString(entity)
                            + " мертва или удаляется");
        }
        int index = Entity.index(entity);
        ComponentStore<T> store = store(type);
        int word = index * words + (type.index >>> 6);
        long bit = 1L << (type.index & 63);
        if ((composition[word] & bit) != 0) {
            return store.get(index);
        }
        composition[word] |= bit;
        T component;
        if ((pendingRemoval[word] & bit) != 0) {
            // Компонент удалили и снова добавили до точки синхронизации — переиспользуем его.
            pendingRemoval[word] &= ~bit;
            component = store.get(index);
            component.reset();
        } else {
            component = store.add(index);
        }
        markDirty(index);
        return component;
    }

    void remove(int entity, ComponentType<?> type) {
        if (!isAlive(entity)) {
            return;
        }
        int index = Entity.index(entity);
        int word = index * words + (type.index >>> 6);
        long bit = 1L << (type.index & 63);
        if ((composition[word] & bit) == 0) {
            return;
        }
        composition[word] &= ~bit;
        pendingRemoval[word] |= bit;
        markDirty(index);
    }

    /** Сколько компонентов типа лежит в пуле (для тестов). */
    int pooled(Class<? extends Component> type) {
        syncIfIdle();
        return stores[typeOf(type).index].pooled();
    }

    private void markDirty(int index) {
        if (!dirtyFlags[index]) {
            dirtyFlags[index] = true;
            dirty.add(index);
        }
    }

    /** Синхронизирует мир, если сейчас не идёт {@link #update} и есть что применять. */
    void syncIfIdle() {
        if (!processing && !settling && (!dirty.isEmpty() || events.hasPending())) {
            settle();
        }
    }

    private void settle() {
        if (settling) {
            return; // изменения из подписчиков и обработчиков подхватит текущий цикл
        }
        settling = true;
        try {
            int rounds = 0;
            do {
                if (++rounds > MAX_SETTLE_ROUNDS) {
                    throw new IllegalStateException(
                            "Мир не стабилизируется: изменения и события порождают друг друга по"
                                    + " кругу");
                }
                flush();
                events.dispatch();
            } while (!dirty.isEmpty() || events.hasPending());
        } finally {
            settling = false;
        }
    }

    /** Применяет изменения всех «грязных» сущностей: членство в наборах, удаления. */
    private void flush() {
        // Подписчики EntitySet могут менять мир прямо здесь: новые грязные сущности добавятся в
        // конец
        // списка и будут обработаны в этом же проходе.
        for (int cursor = 0; cursor < dirty.size(); cursor++) {
            int index = dirty.get(cursor);
            dirtyFlags[index] = false;
            if (!alive[index]) {
                continue;
            }
            if (dying[index]) {
                destroyNow(index);
            } else {
                refreshSets(index);
                applyPendingRemovals(index);
            }
        }
        dirty.clear();
    }

    private void refreshSets(int index) {
        int offset = index * words;
        for (int i = 0; i < sets.size(); i++) {
            EntitySet set = sets.get(i);
            boolean matches = set.matches(composition, offset);
            boolean contains = set.containsIndex(index);
            if (matches && !contains) {
                set.insert(Entity.pack(index, generations[index]));
            } else if (!matches && contains) {
                set.erase(index);
            }
        }
    }

    private void applyPendingRemovals(int index) {
        int offset = index * words;
        for (int w = 0; w < words; w++) {
            long bits = pendingRemoval[offset + w];
            pendingRemoval[offset + w] = 0;
            while (bits != 0) {
                stores[(w << 6) + Long.numberOfTrailingZeros(bits)].remove(index);
                bits &= bits - 1;
            }
        }
    }

    private void destroyNow(int index) {
        for (int i = 0; i < sets.size(); i++) {
            EntitySet set = sets.get(i);
            if (set.containsIndex(index)) {
                set.erase(index);
            }
        }
        int offset = index * words;
        for (int w = 0; w < words; w++) {
            long bits = composition[offset + w] | pendingRemoval[offset + w];
            composition[offset + w] = 0;
            pendingRemoval[offset + w] = 0;
            while (bits != 0) {
                stores[(w << 6) + Long.numberOfTrailingZeros(bits)].remove(index);
                bits &= bits - 1;
            }
        }
        alive[index] = false;
        dying[index] = false;
        generations[index] = (generations[index] + 1) & Entity.GENERATION_MASK;
        freeIndices.add(index);
        entityCount--;
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> ComponentType<T> typeOf(Class<T> type) {
        ComponentType<?> componentType = typesByClass.get(type);
        if (componentType == null) {
            throw new IllegalArgumentException(
                    "Компонент не зарегистрирован в WorldBuilder: " + type.getName());
        }
        return (ComponentType<T>) componentType;
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> ComponentStore<T> store(ComponentType<T> type) {
        return (ComponentStore<T>) stores[type.index];
    }

    private <T extends Component> ComponentStore<T> createStore(ComponentType<T> type) {
        return new ComponentStore<>(type);
    }

    private <T extends Component> ComponentMapper<T> createMapper(ComponentType<T> type) {
        return new ComponentMapper<>(this, type, store(type));
    }

    private long[] mask(Family family, Iterable<Class<? extends Component>> classes) {
        long[] mask = new long[words];
        for (Class<? extends Component> c : classes) {
            ComponentType<?> type = typesByClass.get(c);
            if (type == null) {
                throw new IllegalArgumentException(
                        "В " + family + " компонент не зарегистрирован: " + c.getName());
            }
            mask[type.index >>> 6] |= 1L << (type.index & 63);
        }
        return mask;
    }

    private Object resolveInjection(Field field) {
        Class<?> type = field.getType();
        if (type == ComponentMapper.class) {
            Type generic = field.getGenericType();
            if (generic instanceof ParameterizedType parameterized
                    && parameterized.getActualTypeArguments()[0] instanceof Class<?> argument
                    && Component.class.isAssignableFrom(argument)) {
                return mapper(argument.asSubclass(Component.class));
            }
            throw new IllegalStateException("Поле " + field + ": нужен ComponentMapper<Компонент>");
        }
        if (type == World.class) {
            return this;
        }
        if (type == EventBus.class) {
            return events;
        }
        Object resource = resources.get(type);
        return resource != null ? resource : systemsByClass.get(type);
    }

    private void allocate(int newCapacity) {
        capacity = newCapacity;
        generations = new int[capacity];
        alive = new boolean[capacity];
        dying = new boolean[capacity];
        dirtyFlags = new boolean[capacity];
        composition = new long[capacity * words];
        pendingRemoval = new long[capacity * words];
    }

    private void ensureCapacity(int required) {
        if (required <= capacity) {
            return;
        }
        int newCapacity = Math.min(Entity.MAX_ENTITIES, Math.max(required, capacity * 2));
        capacity = newCapacity;
        generations = Arrays.copyOf(generations, newCapacity);
        alive = Arrays.copyOf(alive, newCapacity);
        dying = Arrays.copyOf(dying, newCapacity);
        dirtyFlags = Arrays.copyOf(dirtyFlags, newCapacity);
        composition = Arrays.copyOf(composition, newCapacity * words);
        pendingRemoval = Arrays.copyOf(pendingRemoval, newCapacity * words);
    }

    private void checkNotDisposed() {
        if (disposed) {
            throw new IllegalStateException("Мир уже освобождён");
        }
    }
}
