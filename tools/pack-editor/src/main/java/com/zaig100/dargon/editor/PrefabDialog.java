package com.zaig100.dargon.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.zaig100.dargon.editor.model.PackProject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Свои префабы и текстуры пака. Префабы правятся в черновике («Применить» проверяет их, сущности в
 * уровнях следуют за переименованием); текстуры копируются в пак сразу.
 */
final class PrefabDialog extends VisDialog {

    private final EditorApp app;
    private final PackProject project;
    private final PrefabsPanel prefabs;
    private final VisTable page = new VisTable();
    private final List<VisTextButton> tabs = new ArrayList<>();
    private final List<Actor> pages = new ArrayList<>();
    private final VisLabel error = new VisLabel("", "small");

    PrefabDialog(EditorApp app, PackProject project) {
        super("Префабы и текстуры пака");
        this.app = app;
        this.project = project;
        setModal(true);
        closeOnEscape();
        addCloseButton();
        prefabs = new PrefabsPanel(app.data(), app.assets(), project, () -> error.setText(""));
        pages.add(prefabs);
        pages.add(new TexturesPanel(app, project, app.assets()));
        VisTable bar = new VisTable();
        bar.defaults().padRight(4);
        String[] names = {"Префабы", "Текстуры"};
        for (int i = 0; i < names.length; i++) {
            int index = i;
            VisTextButton tab = new VisTextButton(names[i]);
            tab.addListener(Form.changed(() -> open(index)));
            tabs.add(tab);
            bar.add(tab);
        }
        getContentTable().add(bar).left().padBottom(6).row();
        getContentTable().add(page).width(920).height(480).top().left().row();
        error.setColor(new Color(1, 0.45f, 0.45f, 1));
        error.setWrap(true);
        getContentTable().add(error).width(920).row();
        VisTextButton apply = new VisTextButton("Применить");
        apply.addListener(Form.changed(this::apply));
        VisTextButton cancel = new VisTextButton("Закрыть без применения префабов");
        cancel.addListener(Form.changed(this::fadeOut));
        getButtonsTable().add(apply).pad(4);
        getButtonsTable().add(cancel).pad(4);
        open(0);
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

    private void apply() {
        String problems = prefabs.validateAll();
        if (!problems.isEmpty()) {
            error.setText(problems);
            open(0);
            return;
        }
        project.setPrefabs(prefabs.draft());
        for (Map.Entry<String, String> r : prefabs.renames().entrySet()) {
            project.renamePrefabInLevels(r.getKey(), r.getValue());
        }
        app.reloadData();
        app.selectionChanged();
        fadeOut();
    }

    @Override
    protected void result(Object object) {
        // Кнопки свои.
    }

    @Override
    public void fadeOut() {
        if (getStage() != null) {
            getStage().setKeyboardFocus(null);
        }
        super.fadeOut();
    }
}
