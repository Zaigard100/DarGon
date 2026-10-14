package com.zaig100.dargon.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.zaig100.dargon.editor.model.PackDraft;
import com.zaig100.dargon.editor.model.PackProject;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.JsonPrinter;
import com.zaig100.dargon.level.LevelFormatException;
import com.zaig100.dargon.level.PackDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Свойства пака — три вкладки: основное (метаданные, название и описание на двух языках), режимы
 * (стартовые наборы), достижения. Правки копятся в черновике: «Применить» проверяет пак тем же
 * разбором, что и игра, «Отмена» всё отбрасывает.
 */
final class PackDialog extends VisDialog {

    private static final float WIDTH = 900;
    private static final float HEIGHT = 520;

    private final EditorApp app;
    private final PackDraft draft;
    private final GameData engine;
    private final VisTable page = new VisTable();
    private final List<VisTextButton> tabs = new ArrayList<>();
    private final List<Actor> pages = new ArrayList<>();
    private final VisLabel error = new VisLabel("");

    PackDialog(EditorApp app, PackProject project, GameData engine) {
        super("Свойства пака");
        this.app = app;
        this.draft = new PackDraft(project);
        this.engine = engine;
        setModal(true);
        closeOnEscape();
        addCloseButton();
        Runnable changed = this::validateDraft;
        pages.add(scroll(general()));
        pages.add(scroll(new ModesPanel(draft, engine.items(), changed)));
        pages.add(new AchievementsPanel(draft, engine.items(), changed));
        VisTable bar = new VisTable();
        bar.defaults().padRight(4);
        String[] names = {"Основное", "Режимы", "Достижения"};
        for (int i = 0; i < names.length; i++) {
            int index = i;
            VisTextButton tab = new VisTextButton(names[i]);
            tab.addListener(Form.changed(() -> open(index)));
            tabs.add(tab);
            bar.add(tab);
        }
        getContentTable().add(bar).left().padBottom(6).row();
        getContentTable().add(page).width(WIDTH).height(HEIGHT).top().left().row();
        error.setColor(new Color(1, 0.45f, 0.45f, 1));
        error.setWrap(true);
        getContentTable().add(error).width(WIDTH).padTop(4).row();
        VisTextButton apply = new VisTextButton("Применить");
        apply.addListener(Form.changed(this::apply));
        VisTextButton cancel = new VisTextButton("Отмена");
        cancel.addListener(Form.changed(this::fadeOut));
        getButtonsTable().add(apply).pad(4);
        getButtonsTable().add(cancel).pad(4);
        open(0);
        validateDraft();
        pack();
        centerWindow();
    }

    private void open(int index) {
        page.clearChildren();
        page.top().left();
        page.add(pages.get(index)).expand().fill();
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).setColor(i == index ? new Color(1, 0.85f, 0.4f, 1) : Color.WHITE);
        }
    }

    private static VisScrollPane scroll(Actor content) {
        VisScrollPane s = Form.wheel(new VisScrollPane(content));
        s.setFadeScrollBars(false);
        s.setScrollingDisabled(true, false);
        return s;
    }

    private VisTable general() {
        Map<String, Object> pack = draft.pack();
        VisTable t = new VisTable();
        t.top().left();
        t.defaults().left().pad(3);
        field(
                t,
                pack,
                "id",
                "id пака",
                "Латиница, цифры и _. По id отделяются сохранения и достижения: у разных паков он должен быть разным.");
        for (String key : List.of("name", "description")) {
            for (String lang : List.of("ru", "en")) {
                VisTextArea area = new VisTextArea(draft.resolve(pack.get(key), lang));
                Form.rows(area, key.equals("name") ? 1 : 3);
                area.addListener(
                        Form.changed(
                                () -> {
                                    draft.setText(pack, key, "pack." + key, lang, area.getText());
                                    validateDraft();
                                }));
                t.add(
                                new VisLabel(
                                        (key.equals("name") ? "Название" : "Описание")
                                                + " ("
                                                + lang
                                                + ")"))
                        .width(170)
                        .top();
                t.add(area).width(560).row();
            }
        }
        field(t, pack, "author", "Автор", null);
        field(t, pack, "version", "Версия пака", "Например 1.0 — видна игрокам в списке паков.");
        field(
                t,
                pack,
                "game",
                "Мин. версия игры",
                "Пак не откроется в игре старше этой версии, например 4.0.0.");
        field(
                t,
                pack,
                "icon",
                "Иконка (PNG)",
                "Файл в каталоге пака, например icon.png; квадратная, пиксельная.");
        return t;
    }

    private void field(
            VisTable t, Map<String, Object> pack, String key, String label, String hint) {
        t.add(new VisLabel(label)).width(170);
        t.add(
                        Form.text(
                                String.valueOf(pack.getOrDefault(key, "")),
                                v -> {
                                    if (v.isBlank() && !key.equals("id")) {
                                        pack.remove(key);
                                    } else {
                                        pack.put(key, v.strip());
                                    }
                                    validateDraft();
                                }))
                .width(560)
                .row();
        if (hint != null) {
            t.add();
            t.add(Form.hint(hint, 560)).width(560).row();
        }
    }

    /** Разбор пака, как в игре (без уровней): ошибки — внизу окна сразу. */
    private boolean validateDraft() {
        try {
            PackDefinition.parse(
                    JsonPrinter.write(draft.pack()), PackDefinition.FILE, engine.items());
            error.setText("");
            return true;
        } catch (LevelFormatException e) {
            error.setText(e.getMessage());
            return false;
        }
    }

    private void apply() {
        if (!validateDraft()) {
            return;
        }
        draft.apply();
        app.packEdited();
        fadeOut();
    }

    @Override
    protected void result(Object object) {
        // Кнопки свои — стандартный результат не нужен.
    }

    /** Чтобы фокус не оставался в полях закрытого диалога. */
    @Override
    public void fadeOut() {
        if (getStage() != null) {
            getStage().setKeyboardFocus(null);
        }
        super.fadeOut();
    }
}
