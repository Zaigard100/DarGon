package com.zaig100.dargon.ui;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.eachGrid;
import static com.zaig100.dargon.ui.Ui.row;
import static com.zaig100.dargon.ui.Ui.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.zaig100.dargon.ui.signal.Signal;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UiBuilderTest {

    private static Theme theme;

    @BeforeAll
    static void gdx() {
        new HeadlessApplication(new com.badlogic.gdx.ApplicationAdapter() {});
        // Заглушка OpenGL: вызовы ничего не делают, числа — нули.
        GL20 gl =
                (GL20)
                        Proxy.newProxyInstance(
                                GL20.class.getClassLoader(),
                                new Class<?>[] {GL20.class},
                                (proxy, method, args) -> {
                                    Class<?> type = method.getReturnType();
                                    if (type == int.class) {
                                        return 0;
                                    }
                                    if (type == boolean.class) {
                                        return false;
                                    }
                                    if (type == float.class) {
                                        return 0f;
                                    }
                                    return null;
                                });
        Gdx.gl = gl;
        Gdx.gl20 = gl;
        BitmapFont font = new BitmapFont();
        theme =
                new Theme() {
                    @Override
                    public BitmapFont font(String style) {
                        return font;
                    }

                    @Override
                    public Color color(String style) {
                        return Color.WHITE;
                    }

                    @Override
                    public Color focusColor() {
                        return Color.YELLOW;
                    }

                    @Override
                    public Drawable drawable(String name) {
                        return null;
                    }

                    @Override
                    public Drawable focusBackground() {
                        return null;
                    }
                };
    }

    /** Ставит экран на настоящую сцену (без сцены кнопки не считаются видимыми). */
    private static void stageRoot(UiView view) {
        Batch batch =
                (Batch)
                        Proxy.newProxyInstance(
                                Batch.class.getClassLoader(),
                                new Class<?>[] {Batch.class},
                                (proxy, method, args) ->
                                        method.getReturnType() == boolean.class ? false : null);
        Stage stage = new Stage(new ScreenViewport(), batch);
        com.badlogic.gdx.scenes.scene2d.Group area = new com.badlogic.gdx.scenes.scene2d.Group();
        area.setSize(200, 200);
        stage.addActor(area);
        view.attach(area);
        view.root().validate();
    }

    @Test
    void textFollowsSignalAndBindingsAreReleased() {
        Signal<Integer> coins = Signal.of(5);
        UiView view = new UiView(theme, column(2, text(coins.map(String::valueOf)).id("coins")));
        Label label = view.root().findActor("coins");
        assertEquals("5", label.getText().toString());
        coins.set(12);
        assertEquals("12", label.getText().toString());
        view.dispose();
        assertEquals(0, coins.listenerCount());
    }

    @Test
    void eachReusesWidgetsByKeyAndValue() {
        Signal<List<String>> items = Signal.of(List.of("a", "b"));
        UiView view = new UiView(theme, eachGrid(items, 2, 1, s -> s, s -> text(s)).id("list"));
        Table list = view.root().findActor("list");
        Label a = (Label) list.getChildren().get(0);
        Label b = (Label) list.getChildren().get(1);
        items.set(List.of("b", "a", "c"));
        assertSame(b, list.getChildren().get(0), "тот же ключ и значение — тот же виджет");
        assertSame(a, list.getChildren().get(1));
        assertEquals(3, list.getChildren().size);
        items.set(List.of("c"));
        assertNotSame(a, list.getChildren().get(0));
        view.dispose();
        assertEquals(0, items.listenerCount());
    }

    @Test
    void focusMovesByGeometryAndWraps() {
        List<String> pressed = new ArrayList<>();
        UiView view =
                new UiView(
                        theme,
                        column(
                                4,
                                button("one", () -> pressed.add("one")).focused(),
                                button("two", () -> pressed.add("two")),
                                row(
                                        4,
                                        button("left", () -> pressed.add("left")),
                                        button("right", () -> pressed.add("right")))));
        stageRoot(view);
        FocusNavigator focus = view.focus();
        focus.activate();
        focus.move(0, -1);
        focus.activate();
        focus.move(0, 1);
        focus.move(0, 1);
        focus.activate();
        assertEquals(List.of("one", "two"), pressed.subList(0, 2));
        assertTrue(
                List.of("left", "right").contains(pressed.get(2)),
                "вверх с первой — по кругу на самую дальнюю кнопку той же колонки: " + pressed);
        // Фокус теперь в строке: вправо — соседняя, ещё вправо — по кругу обратно.
        pressed.clear();
        focus.activate();
        String first = pressed.get(0);
        focus.move(1, 0);
        focus.activate();
        focus.move(1, 0);
        focus.activate();
        String other = first.equals("left") ? "right" : "left";
        assertEquals(List.of(first, other, first), pressed, "в строке вправо — по кругу");
        assertTrue(focus.focused().isFocused());
    }

    @Test
    void verticalMoveReachesOffsetButtonsInRow() {
        List<String> pressed = new ArrayList<>();
        UiView view =
                new UiView(
                        theme,
                        column(
                                2,
                                button("a long language button", () -> pressed.add("lang"))
                                        .focused(),
                                row(
                                        20,
                                        button("-", () -> pressed.add("minus")),
                                        text("x"),
                                        button("+", () -> pressed.add("plus"))),
                                button("back", () -> pressed.add("back"))));
        stageRoot(view);
        view.focus().move(0, -1);
        view.focus().activate();
        view.focus().move(0, -1);
        view.focus().activate();
        assertTrue(List.of("minus", "plus").contains(pressed.get(0)), "строка достижима вниз");
        assertEquals("back", pressed.get(1));
    }

    @Test
    void customActorsAreAlignedInLayers() {
        com.badlogic.gdx.scenes.scene2d.Actor corner = new com.badlogic.gdx.scenes.scene2d.Actor();
        UiView view =
                new UiView(
                        theme,
                        Ui.layers(
                                Ui.custom(() -> corner)
                                        .size(10, 10)
                                        .align(com.badlogic.gdx.utils.Align.bottomRight)));
        stageRoot(view);
        com.badlogic.gdx.math.Vector2 p =
                corner.localToStageCoordinates(new com.badlogic.gdx.math.Vector2());
        assertEquals(190, p.x, 0.01, "прижат вправо");
        assertEquals(0, p.y, 0.01, "прижат вниз");
    }

    @Test
    void hiddenNodesTakeNoSpace() {
        Signal<Boolean> shown = Signal.of(false);
        UiView view =
                new UiView(
                        theme,
                        row(0, text("a").id("a"), text("hidden text").visible(shown), text("b"))
                                .id("row"));
        stageRoot(view);
        com.badlogic.gdx.scenes.scene2d.ui.Table row = view.root().findActor("row");
        float narrow = row.getPrefWidth();
        shown.set(true);
        assertTrue(row.getPrefWidth() > narrow + 10, "показанный элемент добавляет ширину");
    }

    @Test
    void tooBigContentIsScaledDownNotCut() {
        UiView view = new UiView(theme, column(0, text("x").size(400, 100).id("wide")));
        stageRoot(view);
        com.badlogic.gdx.scenes.scene2d.Actor wide = view.root().findActor("wide");
        com.badlogic.gdx.math.Vector2 right =
                wide.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(wide.getWidth(), 0));
        assertTrue(right.x <= 200.5f, "правый край внутри области 200: " + right.x);
    }

    @Test
    void contentThatGrowsLaterIsScaledDownToo() {
        Signal<Boolean> shown = Signal.of(false);
        UiView view =
                new UiView(
                        theme,
                        Ui.layers(
                                column(
                                        0,
                                        row(
                                                0,
                                                text("x").size(150, 10).id("a"),
                                                text("y").size(150, 10).visible(shown)))));
        stageRoot(view);
        shown.set(true);
        view.root().validate();
        com.badlogic.gdx.scenes.scene2d.Actor a = view.root().findActor("a");
        com.badlogic.gdx.math.Vector2 left =
                a.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, 0));
        assertTrue(left.x >= -0.5f, "левый край внутри: " + left.x);
    }

    @Test
    void disabledButtonsAreSkipped() {
        Signal<Boolean> canContinue = Signal.of(false);
        List<String> pressed = new ArrayList<>();
        UiView view =
                new UiView(
                        theme,
                        column(
                                4,
                                button("continue", () -> pressed.add("continue"))
                                        .enabled(canContinue),
                                button("new", () -> pressed.add("new"))));
        stageRoot(view);
        view.focus().activate();
        canContinue.set(true);
        view.focus().move(0, 1);
        view.focus().activate();
        assertEquals(List.of("new", "continue"), pressed);
    }
}
