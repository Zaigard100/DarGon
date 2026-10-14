package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.grid;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.text;

import com.zaig100.dargon.ui.UiNode;
import java.util.ArrayList;
import java.util.List;

/** Разработчик: начать с любого уровня (без сохранения; прохождение помечается). */
final class LevelSelectScreen extends UiScreen {

    private static final int COLUMNS = 7;

    @Override
    protected UiNode build() {
        List<UiNode> levels = new ArrayList<>();
        for (String id : app.data().levels()) {
            var b =
                    button(text(shortName(id)).style("tiny"), () -> app.playLevel(id))
                            .pad(0, 1, 0, 1);
            levels.add(levels.isEmpty() ? b.focused() : b);
        }
        return layers(
                        Widgets.panel(
                                column(
                                        2,
                                        text(app.texts().tr("dev.levels")).style("button"),
                                        grid(COLUMNS, 1, levels.toArray(new UiNode[0])),
                                        button(
                                                text(app.texts().tr("settings.back"))
                                                        .style("small"),
                                                this::back))))
                .background("dim")
                .expand();
    }

    /** {@code shops/shop_05} → {@code $05}, {@code dev/test3} → {@code t3}. */
    static String shortName(String id) {
        if (id.startsWith("shops/shop_")) {
            return "$" + id.substring("shops/shop_".length());
        }
        if (id.startsWith("dev/test")) {
            return "t" + id.substring("dev/test".length());
        }
        return id;
    }
}
