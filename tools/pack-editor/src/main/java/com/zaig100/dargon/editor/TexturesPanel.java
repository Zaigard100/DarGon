package com.zaig100.dargon.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.util.dialog.InputDialogAdapter;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisList;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import com.kotcrab.vis.ui.widget.file.FileTypeFilter;
import com.zaig100.dargon.editor.model.PackProject;
import com.zaig100.dargon.render.GameAssets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Свои картинки пака ({@code textures/<имя>.png}): список и предпросмотр. Полоса шириной в
 * несколько клеток 16×16 (или 16×32) — кадры анимации, как у игры. Имя игровой картинки заменяет
 * её. Изменения сразу в каталоге пака.
 */
final class TexturesPanel extends VisTable {

    private static final int FRAME = 16;

    private final EditorApp app;
    private final PackProject project;
    private final GameAssets assets;
    private final VisList<String> list = new VisList<>();
    private final Preview preview = new Preview();
    private final VisLabel info = new VisLabel("", "small");
    private List<String> names = List.of();

    TexturesPanel(EditorApp app, PackProject project, GameAssets assets) {
        this.app = app;
        this.project = project;
        this.assets = assets;
        top().left();
        VisTable left = new VisTable();
        left.top().left();
        left.add(Form.wheel(new VisScrollPane(list))).width(220).height(330).row();
        left.add(button("Добавить PNG…", () -> choose(null))).left().row();
        left.add(
                        button(
                                "Заменить файл…",
                                () -> {
                                    String name = selected();
                                    if (name != null) {
                                        choose(name);
                                    }
                                }))
                .left()
                .row();
        left.add(button("Удалить", this::removeTexture)).left().padTop(6).row();
        add(left).top().padRight(10);
        VisTable right = new VisTable();
        right.top().left();
        right.add(preview).size(384, 192).left().row();
        info.setWrap(true);
        right.add(info).width(560).left().padTop(6).row();
        right.add(
                        Form.hint(
                                "Картинка 16×16 — одна клетка. Полоса 16·N×16 (или ×32) — N кадров:"
                                        + " кадр выбирается полем frame у SpriteRef. Если имя совпадает с"
                                        + " игровой картинкой (например tileset, spike0), картинка игры"
                                        + " заменяется, пока выбран пак.",
                                560))
                .width(560)
                .padTop(6)
                .row();
        add(right).top();
        list.addListener(Form.changed(this::showSelected));
        refresh(null);
    }

    private String selected() {
        int i = list.getSelectedIndex();
        return i >= 0 && i < names.size() ? names.get(i) : null;
    }

    private void refresh(String select) {
        names = project.textures();
        Array<String> items = new Array<>();
        for (String name : names) {
            items.add(name + (assetsHasGame(name) ? "  (заменяет игровую)" : ""));
        }
        list.setItems(items);
        if (select != null && names.contains(select)) {
            list.setSelectedIndex(names.indexOf(select));
        }
        showSelected();
    }

    private boolean assetsHasGame(String name) {
        return assets.atlasNames().contains(name);
    }

    private void showSelected() {
        String name = selected();
        preview.show(name == null ? null : project.textureBytes(name));
        if (name == null) {
            info.setText(names.isEmpty() ? "Текстур пока нет." : "");
            return;
        }
        int w = preview.width;
        int h = preview.height;
        boolean strip = w % FRAME == 0 && (h == FRAME || h == FRAME * 2);
        info.setText(
                PackProject.texturePath(name)
                        + " — "
                        + w
                        + "×"
                        + h
                        + (strip ? ", кадров: " + w / FRAME : ", одна картинка")
                        + (assetsHasGame(name)
                                ? "\nЗаменяет игровую картинку «" + name + "»."
                                : ""));
    }

    private void choose(String replace) {
        FileChooser chooser = new FileChooser("Картинка PNG", FileChooser.Mode.OPEN);
        chooser.setSelectionMode(FileChooser.SelectionMode.FILES);
        FileTypeFilter filter = new FileTypeFilter(false);
        filter.addRule("PNG (*.png)", "png");
        chooser.setFileTypeFilter(filter);
        chooser.setListener(
                new FileChooserAdapter() {
                    @Override
                    public void selected(Array<FileHandle> files) {
                        Path png = files.first().file().toPath();
                        if (replace != null) {
                            store(png, replace);
                            return;
                        }
                        String base =
                                files.first()
                                        .nameWithoutExtension()
                                        .replaceAll("[^A-Za-z0-9_]", "_");
                        Dialogs.showInputDialog(
                                        getStage(),
                                        "Имя текстуры",
                                        "Имя (латиница, цифры, _): ",
                                        input -> input.matches("[A-Za-z0-9_]+"),
                                        new InputDialogAdapter() {
                                            @Override
                                            public void finished(String name) {
                                                store(png, name);
                                            }
                                        })
                                .setText(base);
                    }
                });
        getStage().addActor(chooser.fadeIn());
    }

    private void store(Path png, String name) {
        try {
            project.importTexture(png, name);
        } catch (IOException e) {
            Dialogs.showErrorDialog(getStage(), "Не удалось скопировать: " + e.getMessage());
            return;
        }
        app.reloadData();
        refresh(name);
    }

    private void removeTexture() {
        String name = selected();
        if (name == null) {
            return;
        }
        project.removeTexture(name);
        app.reloadData();
        refresh(null);
    }

    private static VisTextButton button(String text, Runnable action) {
        VisTextButton b = new VisTextButton(text);
        b.addListener(Form.changed(action));
        return b;
    }

    /** Предпросмотр: картинка крупно, пиксели без сглаживания, сетка кадров. */
    private static final class Preview extends Actor {
        private Texture texture;
        private byte[] png;
        int width;
        int height;

        void show(byte[] png) {
            this.png = png;
            if (texture != null) {
                texture.dispose();
                texture = null;
            }
            width = 0;
            height = 0;
            build();
        }

        private void build() {
            if (png == null) {
                return;
            }
            try {
                Pixmap pixmap = new Pixmap(png, 0, png.length);
                texture = new Texture(pixmap);
                width = pixmap.getWidth();
                height = pixmap.getHeight();
                pixmap.dispose();
            } catch (RuntimeException e) {
                texture = null;
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (texture == null) {
                build(); // после переключения вкладок текстура освобождена
            }
            if (texture == null) {
                return;
            }
            float scale = Math.max(1, (int) Math.min(getWidth() / width, getHeight() / height));
            batch.setColor(1, 1, 1, parentAlpha);
            batch.draw(
                    texture,
                    getX(),
                    getY() + getHeight() - height * scale,
                    width * scale,
                    height * scale);
        }

        @Override
        protected void setStage(com.badlogic.gdx.scenes.scene2d.Stage stage) {
            if (stage == null && texture != null) {
                texture.dispose();
                texture = null;
            }
            super.setStage(stage);
        }
    }
}
