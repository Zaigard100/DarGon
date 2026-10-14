package com.zaig100.dargon.ui.signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Изменяемый сигнал. Подписчики вызываются, только если значение действительно изменилось. */
public final class Signal<T> implements ReadSignal<T> {

    private T value;
    private final List<Consumer<? super T>> listeners = new ArrayList<>();
    private boolean notifying;

    public Signal(T value) {
        this.value = value;
    }

    public static <T> Signal<T> of(T value) {
        return new Signal<>(value);
    }

    @Override
    public T get() {
        return value;
    }

    public void set(T newValue) {
        if (Objects.equals(value, newValue)) {
            return;
        }
        value = newValue;
        notifyListeners();
    }

    /** Оповещает подписчиков, даже если значение то же (изменился изменяемый объект внутри). */
    public void touch() {
        notifyListeners();
    }

    private void notifyListeners() {
        if (notifying) {
            throw new IllegalStateException("Сигнал изменён из собственного подписчика");
        }
        notifying = true;
        try {
            // Снимок: подписчик может отписаться во время оповещения.
            for (Object listener : listeners.toArray()) {
                @SuppressWarnings("unchecked")
                Consumer<? super T> l = (Consumer<? super T>) listener;
                l.accept(value);
            }
        } finally {
            notifying = false;
        }
    }

    @Override
    public Subscription subscribe(Consumer<? super T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** Число подписчиков (для тестов на утечки подписок). */
    public int listenerCount() {
        return listeners.size();
    }
}
