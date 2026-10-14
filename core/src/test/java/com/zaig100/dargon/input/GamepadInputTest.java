package com.zaig100.dargon.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GamepadInputTest {

    /** Раскладка: оси 0/1, кнопки A=0 B=1 X=2 Y=3 Start=7, крестовина 11..14. */
    private static final ControllerMapping MAPPING =
            new ControllerMapping(
                    0, 1, 2, 3, 0, 1, 2, 3, 6, 7, 4, 8, 5, 9, 11, 12, 13, 14, 10, 15) {};

    private static Controller controller() {
        return (Controller)
                Proxy.newProxyInstance(
                        Controller.class.getClassLoader(),
                        new Class<?>[] {Controller.class},
                        (proxy, method, args) ->
                                method.getName().equals("getMapping") ? MAPPING : null);
    }

    @Test
    void buttonsAndStickBecomeActions() {
        ActionState state = new ActionState();
        InputMapper mapper = new InputMapper(state);
        List<Action> ui = new ArrayList<>();
        GamepadInput pad = new GamepadInput(mapper);
        Controller c = controller();

        pad.buttonDown(c, 0);
        state.beginTick();
        assertTrue(state.wasPressed(Action.USE));
        assertEquals(Device.GAMEPAD, mapper.device().get());

        pad.axisMoved(c, 0, 0.3f);
        state.beginTick();
        assertFalse(state.isHeld(Action.MOVE_RIGHT), "мёртвая зона");
        pad.axisMoved(c, 0, 0.9f);
        pad.axisMoved(c, 1, 0.8f);
        state.beginTick();
        assertTrue(state.isHeld(Action.MOVE_RIGHT));
        assertTrue(state.isHeld(Action.MOVE_DOWN), "стик по диагонали — две стрелки");

        pad.disconnected(c);
        state.beginTick();
        assertFalse(state.isHeld(Action.MOVE_RIGHT), "отключение отпускает стик");

        mapper.setActionListener(action -> ui.add(action));
        pad.buttonDown(c, 1);
        assertEquals(List.of(Action.PAUSE), ui, "открытый экран забирает нажатия");
    }
}
