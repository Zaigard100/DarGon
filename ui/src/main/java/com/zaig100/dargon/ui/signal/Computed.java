package com.zaig100.dargon.ui.signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Производный сигнал: значение считается из других сигналов. На источники подписывается только пока
 * есть свои подписчики — поэтому брошенный производный сигнал не держит источник.
 */
final class Computed<T> implements ReadSignal<T> {

    private final Supplier<T> compute;
    private final ReadSignal<?>[] sources;
    private final List<Consumer<? super T>> listeners = new ArrayList<>();
    private final List<Subscription> upstream = new ArrayList<>();
    private T cached;

    Computed(Supplier<T> compute, ReadSignal<?>... sources) {
        this.compute = compute;
        this.sources = sources;
    }

    @Override
    public T get() {
        return listeners.isEmpty() ? compute.get() : cached;
    }

    @Override
    public Subscription subscribe(Consumer<? super T> listener) {
        if (listeners.isEmpty()) {
            cached = compute.get();
            for (ReadSignal<?> source : sources) {
                upstream.add(source.subscribe(v -> recompute()));
            }
        }
        listeners.add(listener);
        return () -> {
            if (listeners.remove(listener) && listeners.isEmpty()) {
                upstream.forEach(Subscription::cancel);
                upstream.clear();
            }
        };
    }

    private void recompute() {
        T value = compute.get();
        if (Objects.equals(value, cached)) {
            return;
        }
        cached = value;
        for (Object listener : listeners.toArray()) {
            @SuppressWarnings("unchecked")
            Consumer<? super T> l = (Consumer<? super T>) listener;
            l.accept(value);
        }
    }
}
