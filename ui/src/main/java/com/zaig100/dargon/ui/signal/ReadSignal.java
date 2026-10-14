package com.zaig100.dargon.ui.signal;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Значение, за изменениями которого можно следить. Интерфейс подписывается на сигналы, а не
 * опрашивает игру каждый кадр: меняется только то, что привязано к изменившемуся значению.
 */
public interface ReadSignal<T> {

    T get();

    /** Вызывает {@code listener} при каждом изменении (не сразу). */
    Subscription subscribe(Consumer<? super T> listener);

    /** Вызывает {@code listener} сразу с текущим значением и затем при каждом изменении. */
    default Subscription observe(Consumer<? super T> listener) {
        listener.accept(get());
        return subscribe(listener);
    }

    /** Производный сигнал: {@code hp.map(v -> "hp_" + v)}. */
    default <R> ReadSignal<R> map(Function<? super T, ? extends R> mapper) {
        return new Computed<>(() -> mapper.apply(get()), this);
    }

    /** Сигнал из двух. */
    default <U, R> ReadSignal<R> combine(
            ReadSignal<U> other, BiFunction<? super T, ? super U, ? extends R> mapper) {
        return new Computed<>(() -> mapper.apply(get(), other.get()), this, other);
    }

    /** Постоянное значение. */
    static <T> ReadSignal<T> constant(T value) {
        return new ReadSignal<>() {
            @Override
            public T get() {
                return value;
            }

            @Override
            public Subscription subscribe(Consumer<? super T> listener) {
                return Subscription.NONE;
            }
        };
    }
}
