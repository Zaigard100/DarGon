package com.zaig100.dargon.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerMapping;
import java.util.EnumMap;
import java.util.Map;

/**
 * Геймпад (стандартная раскладка gdx-controllers): A — использовать, B — назад, Y — инвентарь, X —
 * выбросить, Start — пауза, крестовина и левый стик — шаги. Стик с мёртвой зоной даёт и диагонали
 * (как две нажатые стрелки).
 */
public final class GamepadInput extends ControllerAdapter {

    /** Отклонение стика, с которого считается нажатие. */
    static final float DEAD_ZONE = 0.5f;

    private final InputMapper mapper;

    /** Какие направления сейчас держит стик. */
    private final Map<Action, Boolean> stick = new EnumMap<>(Action.class);

    public GamepadInput(InputMapper mapper) {
        this.mapper = mapper;
    }

    /** Действие для кнопки по раскладке контроллера или {@code null}. */
    static Action action(ControllerMapping m, int button) {
        if (button == m.buttonA) {
            return Action.USE;
        }
        if (button == m.buttonB) {
            return Action.PAUSE;
        }
        if (button == m.buttonY) {
            return Action.INVENTORY;
        }
        if (button == m.buttonX) {
            return Action.DROP;
        }
        if (button == m.buttonStart) {
            return Action.PAUSE;
        }
        if (button == m.buttonDpadUp) {
            return Action.MOVE_UP;
        }
        if (button == m.buttonDpadDown) {
            return Action.MOVE_DOWN;
        }
        if (button == m.buttonDpadLeft) {
            return Action.MOVE_LEFT;
        }
        if (button == m.buttonDpadRight) {
            return Action.MOVE_RIGHT;
        }
        return null;
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        Action action = action(controller.getMapping(), buttonCode);
        if (action == null) {
            return false;
        }
        mapper.device().set(Device.GAMEPAD);
        mapper.actionDown(action);
        return true;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        Action action = action(controller.getMapping(), buttonCode);
        if (action == null) {
            return false;
        }
        mapper.actionUp(action);
        return true;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        ControllerMapping m = controller.getMapping();
        if (axisCode == m.axisLeftX) {
            axis(value, Action.MOVE_LEFT, Action.MOVE_RIGHT);
            return true;
        }
        if (axisCode == m.axisLeftY) {
            // Ось Y стика: вниз — положительная.
            axis(value, Action.MOVE_UP, Action.MOVE_DOWN);
            return true;
        }
        return false;
    }

    private void axis(float value, Action negative, Action positive) {
        set(negative, value < -DEAD_ZONE);
        set(positive, value > DEAD_ZONE);
    }

    private void set(Action action, boolean held) {
        boolean was = stick.getOrDefault(action, false);
        if (held == was) {
            return;
        }
        stick.put(action, held);
        if (held) {
            mapper.device().set(Device.GAMEPAD);
            mapper.actionDown(action);
        } else {
            mapper.actionUp(action);
        }
    }

    @Override
    public void disconnected(Controller controller) {
        for (Map.Entry<Action, Boolean> entry : stick.entrySet()) {
            if (entry.getValue()) {
                mapper.actionUp(entry.getKey());
            }
        }
        stick.clear();
    }
}
