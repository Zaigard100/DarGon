package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.custom;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.paragraph;
import static com.zaig100.dargon.ui.Ui.row;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.BuildInfo;
import com.zaig100.dargon.i18n.Localization;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.PackDefinition;
import com.zaig100.dargon.pack.PackLoader;
import com.zaig100.dargon.pack.PackRepository;
import com.zaig100.dargon.ui.FocusNavigator;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Паки: по одному на страницу — иконка, название, версия и автор, описание, состояние (выбран,
 * несовместим, повреждён). «Выбрать» пересобирает игру под пак. На Android пак добавляется
 * системным выбором файла, на ПК — копированием в папку паков (путь показан).
 */
final class PacksScreen extends UiScreen {

    private static final float WIDTH = 104;
    private static final float ICON = 24;

    private final Signal<List<PackRepository.Entry>> entries = Signal.of(List.of());
    private final Signal<Integer> page = Signal.of(0);

    /** Сообщение под паком: ошибка выбора или итог добавления. */
    private final Signal<String> status = Signal.of("");

    private final Map<PackRepository.Entry, Texture> icons = new HashMap<>();

    @Override
    protected UiNode build() {
        Localization texts = app.texts();
        if (entries.get().isEmpty()) {
            reload(app.data().pack().id());
        }
        ReadSignal<PackRepository.Entry> current =
                page.combine(entries, (p, list) -> list.get(Math.min(p, list.size() - 1)));
        ReadSignal<String> name = current.combine(texts.localeSignal(), (e, l) -> title(texts, e));
        ReadSignal<String> byline =
                current.combine(texts.localeSignal(), (e, l) -> byline(texts, e));
        ReadSignal<String> description =
                current.combine(
                        texts.localeSignal(),
                        (e, l) ->
                                e.info() == null
                                        ? ""
                                        : texts.packText(e.files(), e.info().description()));
        ReadSignal<String> state = current.combine(texts.localeSignal(), (e, l) -> state(texts, e));
        UiNode icon =
                custom(
                                () -> {
                                    PackIcon actor = new PackIcon();
                                    current.observe(e -> actor.texture = icon(e));
                                    return actor;
                                })
                        .size(ICON, ICON);
        UiNode add =
                app.platform().canImportPacks()
                        ? button(text(texts.tr("packs.add")).style("small"), this::importPack)
                        : paragraph(
                                        texts.localeSignal()
                                                .map(l -> texts.ui("packs.folder", folder())))
                                .style("tiny")
                                .size(WIDTH, -1);
        return layers(
                        Widgets.panel(
                                column(
                                        2,
                                        text(page.combine(
                                                        entries,
                                                        (p, list) ->
                                                                texts.ui(
                                                                        "packs.title",
                                                                        p + 1,
                                                                        list.size())))
                                                .style("button"),
                                        row(
                                                3,
                                                icon,
                                                column(
                                                        1,
                                                        text(name).style("small").align(Align.left),
                                                        text(byline)
                                                                .style("tiny")
                                                                .align(Align.left))),
                                        paragraph(description).style("tiny").size(WIDTH, -1),
                                        paragraph(state).style("hint").size(WIDTH, -1),
                                        paragraph(status)
                                                .style("warn")
                                                .size(WIDTH, -1)
                                                .visible(status.map(s -> !s.isEmpty())),
                                        row(
                                                4,
                                                button(text("<").style("small"), () -> turn(-1)),
                                                button(
                                                                text(texts.tr("packs.choose"))
                                                                        .style("small"),
                                                                this::choose)
                                                        .focused(),
                                                button(text(">").style("small"), () -> turn(1))),
                                        add,
                                        button(
                                                text(texts.tr("settings.back")).style("small"),
                                                this::back))))
                .background("dim")
                .expand();
    }

    /** Папка паков для подсказки (в шрифте нет «~», поэтому путь полностью). */
    private String folder() {
        return app.packs().userDir().file().getAbsolutePath();
    }

    private void reload(String showId) {
        try {
            app.packs().userDir().mkdirs(); // чтобы на ПК было куда класть .dgpack
        } catch (RuntimeException ignored) {
            // Нет прав — пусть игрок создаст сам; путь показан.
        }
        List<PackRepository.Entry> list = app.packs().list();
        entries.set(list);
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            if (showId != null && showId.equals(list.get(i).id())) {
                index = i;
            }
        }
        page.set(index);
    }

    private static String title(Localization texts, PackRepository.Entry e) {
        if (e.info() == null || e.info().name().isEmpty()) {
            return e.files().location();
        }
        return texts.packText(e.files(), e.info().name());
    }

    private static String byline(Localization texts, PackRepository.Entry e) {
        if (e.info() == null) {
            return "";
        }
        String author = e.info().author().isEmpty() ? "?" : e.info().author();
        String version = e.info().version().isEmpty() ? "?" : e.info().version();
        return texts.ui(e.builtin() ? "packs.bylineBuiltin" : "packs.byline", version, author);
    }

    private String state(Localization texts, PackRepository.Entry e) {
        if (!e.readable()) {
            return texts.ui("packs.broken", e.error());
        }
        PackDefinition.Incompatibility problem = e.info().check(BuildInfo.VERSION);
        if (problem != null) {
            return texts.ui("packs.incompatible." + problem.name(), e.info().game());
        }
        return e.id().equals(app.data().pack().id()) ? texts.ui("packs.current") : "";
    }

    private boolean selectable(PackRepository.Entry e) {
        return e.readable()
                && e.info().check(BuildInfo.VERSION) == null
                && !e.id().equals(app.data().pack().id());
    }

    /** Пак проверяется целиком (все уровни) до переключения — сломанный не выбирается. */
    private void choose() {
        PackRepository.Entry e = entries.get().get(page.get());
        if (!selectable(e)) {
            // Кнопка всегда доступна (фокус не прыгает): объясняем, почему этот пак не выбрать.
            String why = state(app.texts(), e);
            status.set(why.equals(app.texts().ui("packs.current")) ? "" : why);
            return;
        }
        try {
            PackLoader.load(app.engineFiles(), e, BuildInfo.VERSION);
        } catch (PackLoader.PackException ex) {
            status.set(app.texts().ui("packs.broken", ex.getMessage()));
            return;
        }
        app.openPack(e.id());
    }

    private void importPack() {
        status.set("");
        app.platform()
                .importPack(
                        error -> {
                            if (error != null) {
                                status.set(app.texts().ui("packs.addFailed", error));
                                return;
                            }
                            List<PackRepository.Entry> before = entries.get();
                            reload(null);
                            page.set(Math.max(0, entries.get().size() - 1));
                            status.set(
                                    entries.get().size() > before.size()
                                            ? app.texts().ui("packs.added")
                                            : "");
                        });
    }

    private void turn(int delta) {
        status.set("");
        page.set(Math.floorMod(page.get() + delta, entries.get().size()));
    }

    /** Иконка пака из его файлов; нет или не читается — {@code null}. */
    private Texture icon(PackRepository.Entry e) {
        if (icons.containsKey(e)) {
            return icons.get(e);
        }
        Texture texture = null;
        if (e.info() != null && !e.info().icon().isEmpty()) {
            try {
                byte[] png = e.files().bytes(e.info().icon());
                if (png != null) {
                    Pixmap pixmap = new Pixmap(png, 0, png.length);
                    texture = new Texture(pixmap);
                    pixmap.dispose();
                }
            } catch (RuntimeException ignored) {
                // Битая картинка — пак без иконки.
            }
        }
        icons.put(e, texture);
        return texture;
    }

    @Override
    protected void closed() {
        for (Texture t : icons.values()) {
            if (t != null) {
                t.dispose();
            }
        }
        icons.clear();
    }

    @Override
    public boolean onAction(Action action, FocusNavigator focus) {
        if (action == Action.MOVE_LEFT || action == Action.MOVE_RIGHT) {
            turn(action == Action.MOVE_LEFT ? -1 : 1);
            return true;
        }
        return super.onAction(action, focus);
    }

    /** Иконка пака (пиксельная, растягивается без сглаживания); без иконки — пусто. */
    private static final class PackIcon extends Actor {
        Texture texture;

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (texture != null) {
                batch.setColor(1, 1, 1, parentAlpha);
                batch.draw(texture, getX(), getY(), getWidth(), getHeight());
            }
        }
    }
}
