package com.zaig100.dargon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Signal;
import com.zaig100.dargon.ui.signal.Subscription;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignalTest {

    @Test
    void notifiesOnlyOnRealChange() {
        Signal<Integer> hp = Signal.of(4);
        List<Integer> seen = new ArrayList<>();
        Subscription s = hp.subscribe(seen::add);
        hp.set(4);
        hp.set(3);
        hp.set(3);
        s.cancel();
        hp.set(2);
        assertEquals(List.of(3), seen);
        assertEquals(0, hp.listenerCount());
    }

    @Test
    void mappedSignalSubscribesUpstreamOnlyWhileObserved() {
        Signal<Integer> hp = Signal.of(4);
        ReadSignal<String> region = hp.map(v -> "hp_" + v);
        assertEquals("hp_4", region.get());
        assertEquals(
                0, hp.listenerCount(), "без подписчиков производный сигнал не держит источник");
        List<String> seen = new ArrayList<>();
        Subscription s = region.observe(seen::add);
        hp.set(2);
        hp.set(2);
        assertEquals(List.of("hp_4", "hp_2"), seen);
        s.cancel();
        assertEquals(0, hp.listenerCount());
    }

    @Test
    void combineRecomputesFromEitherSource() {
        Signal<Integer> a = Signal.of(1);
        Signal<Integer> b = Signal.of(2);
        ReadSignal<Integer> sum = a.combine(b, Integer::sum);
        List<Integer> seen = new ArrayList<>();
        sum.observe(seen::add);
        a.set(5);
        b.set(0);
        assertEquals(List.of(3, 7, 5), seen);
    }
}
