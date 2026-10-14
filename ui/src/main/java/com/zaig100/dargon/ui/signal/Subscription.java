package com.zaig100.dargon.ui.signal;

/** Подписка на сигнал; {@link #cancel()} отписывает. */
@FunctionalInterface
public interface Subscription {

    Subscription NONE = () -> {};

    void cancel();
}
