package com.zaig100.dargon.ecs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Шина событий мира.
 *
 * <p>Событие — любой объект (обычно {@code record}). {@link #publish} только ставит его в очередь;
 * подписчики вызываются миром в фиксированной точке — сразу после системы, которая опубликовала
 * событие. Поэтому обработчики никогда не выполняются посреди обхода сущностей.
 *
 * <p>События доставляются в порядке публикации. Если обработчик публикует новые события, они
 * доставляются в той же точке после текущих. Подписка — по точному классу события.
 */
public final class EventBus {

    /** Защита от бесконечных цепочек «событие порождает событие». */
    static final int MAX_EVENTS_PER_DISPATCH = 100_000;

    private final Map<Class<?>, List<Consumer<Object>>> handlers = new HashMap<>();
    private final ArrayDeque<Object> queue = new ArrayDeque<>();

    /** Подписывает обработчик на события типа {@code type}. */
    @SuppressWarnings("unchecked")
    public <E> void subscribe(Class<E> type, Consumer<? super E> handler) {
        handlers.computeIfAbsent(type, k -> new ArrayList<>()).add((Consumer<Object>) handler);
    }

    /** Ставит событие в очередь на доставку. */
    public void publish(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("event == null");
        }
        queue.add(event);
    }

    /** Есть ли недоставленные события. */
    public boolean hasPending() {
        return !queue.isEmpty();
    }

    void dispatch() {
        int delivered = 0;
        Object event;
        while ((event = queue.poll()) != null) {
            if (++delivered > MAX_EVENTS_PER_DISPATCH) {
                queue.clear();
                throw new IllegalStateException(
                        "Больше "
                                + MAX_EVENTS_PER_DISPATCH
                                + " событий за одну доставку: вероятно, обработчики публикуют"
                                + " события по кругу (последнее: "
                                + event.getClass().getSimpleName()
                                + ")");
            }
            List<Consumer<Object>> list = handlers.get(event.getClass());
            if (list == null) {
                continue;
            }
            for (int i = 0; i < list.size(); i++) {
                list.get(i).accept(event);
            }
        }
    }
}
