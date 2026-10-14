package com.zaig100.dargon.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Переводит клавиатуру и мышь в действия. Нажатие сначала предлагается {@link ActionListener}
 * (открытому экрану интерфейса); если он его не взял — уходит в {@link ActionState} игры. Клики по
 * полю передаются {@link PointerHandler}.
 */
public final class InputMapper extends InputAdapter {

    /** Обработчик клика (тапа) в экранных координатах. */
    @FunctionalInterface
    public interface PointerHandler {
        void click(int screenX, int screenY);
    }

    /** Получатель нажатий действий раньше игры (интерфейс). */
    @FunctionalInterface
    public interface ActionListener {
        /**
         * @return {@code true}, если нажатие обработано и игре не нужно
         */
        boolean onAction(Action action);
    }

    private final ActionState state;
    private final Map<Integer, Action> keys = new HashMap<>();
    private PointerHandler pointer = (x, y) -> {};
    private ActionListener listener = action -> false;
    private IntConsumer capture;
    private final Signal<Device> device = Signal.of(Device.KEYBOARD);

    public InputMapper(ActionState state) {
        this.state = state;
    }

    /** Раскладка: действие → клавиши. */
    public void setBindings(Map<Action, List<Integer>> bindings) {
        keys.clear();
        bindings.forEach((action, codes) -> codes.forEach(code -> keys.put(code, action)));
    }

    /** Клавиши действия (для подсказок и экрана настроек). */
    public Action actionOf(int keycode) {
        return keys.get(keycode);
    }

    public void setPointerHandler(PointerHandler pointer) {
        this.pointer = pointer;
    }

    public void setActionListener(ActionListener listener) {
        this.listener = listener;
    }

    /** Следующее нажатие любой клавиши уйдёт сюда (переназначение управления). */
    public void captureNextKey(IntConsumer capture) {
        this.capture = capture;
    }

    public boolean isCapturing() {
        return capture != null;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (capture != null) {
            IntConsumer c = capture;
            capture = null;
            c.accept(keycode);
            return true;
        }
        Action action = keys.get(keycode);
        if (action == null) {
            return false;
        }
        device.set(Device.KEYBOARD);
        actionDown(action);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        Action action = keys.get(keycode);
        if (action == null) {
            return false;
        }
        actionUp(action);
        return true;
    }

    /** Нажатие действия с любого устройства: сначала интерфейсу, иначе игре. */
    public void actionDown(Action action) {
        if (!listener.onAction(action)) {
            state.press(action);
        }
    }

    public void actionUp(Action action) {
        state.release(action);
    }

    /** Последнее использованное устройство (для подсказок). */
    public Signal<Device> device() {
        return device;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointerIndex, int button) {
        if (button != Input.Buttons.LEFT) {
            return false;
        }
        device.set(Device.POINTER);
        pointer.click(screenX, screenY);
        return true;
    }
}
