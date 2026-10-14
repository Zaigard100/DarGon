package com.zaig100.dargon.ui;

import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Subscription;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Подписки одного экрана (или элемента списка); при закрытии отменяются все сразу. */
public final class Bindings {

    private final List<Subscription> subscriptions = new ArrayList<>();

    public <T> void observe(ReadSignal<T> signal, Consumer<? super T> listener) {
        subscriptions.add(signal.observe(listener));
    }

    public void add(Subscription subscription) {
        subscriptions.add(subscription);
    }

    public void dispose() {
        subscriptions.forEach(Subscription::cancel);
        subscriptions.clear();
    }

    public int size() {
        return subscriptions.size();
    }
}
