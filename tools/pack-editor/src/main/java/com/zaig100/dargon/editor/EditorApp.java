package com.zaig100.dargon.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.layout.FlowGroup;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.util.dialog.InputDialogAdapter;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisList;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import com.zaig100.dargon.editor.model.LevelDoc;
import com.zaig100.dargon.editor.model.PackExporter;
import com.zaig100.dargon.editor.model.PackProject;
import com.zaig100.dargon.editor.model.ProjectCheck;
import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.Prefab;
import com.zaig100.dargon.level.Problem;
import com.zaig100.dargon.level.Problems;
import com.zaig100.dargon.level.WallEdge;
import com.zaig100.dargon.render.GameAssets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Редактор паков: слева — уровни и палитра префабов, в центре — холст уровня, справа — свойства,
 * снизу — проблемы. Проект — каталог пака; «Играть» запускает игру с паком прямо из каталога.
 */
final class EditorApp extends ApplicationAdapter {

    private static final String PREFS = "dargon-editor";
    private static final String TITLE = "DarGon — редактор паков";
    private static final float[] UI_SCALES = {0.75f, 0.875f, 1f, 1.125f, 1.25f, 1.5f, 1.75f, 2f};

    /** Границы панелей (доля) — запоминаются между запусками. */
    private static final String[] SPLITS = {
        "split.left", "split.right", "split.bottom", "split.levels", "split.palette"
    };

    private static final float[] SPLIT_DEFAULTS = {0.2f, 0.72f, 0.86f, 0.33f, 0.5f};

    private final Path startProject;
    private Stage stage;
    private GameAssets assets;
    private GameData engine;

    /** Данные проекта: игра и пак (свои префабы); пак не разобрался — только игра. */
    private GameData data;

    private AssetReader engineFiles;
    private Preferences prefs;

    private PackProject project;
    private LevelView view;
    private Inspector inspector;
    private final GameWindow game = new GameWindow();

    private VisList<String> levelList;
    private final List<String> levelIds = new ArrayList<>();
    private IconList palette;
    private final List<String> paletteIds = new ArrayList<>();
    private IconList entityList;
    private VisLabel hint;
    private VisTextButton cursorButton;
    private VisLabel scaleLabel;
    private float uiScale = 1;
    private final VisSplitPane[] splits = new VisSplitPane[SPLITS.length];

    /** Окно скриптов (одно) и точки остановки: файл скрипта → строки. Живут до выхода. */
    private ScriptWindow scripts;

    private final Map<String, java.util.Set<Integer>> breakpoints = new java.util.HashMap<>();
    private VisList<String> problemList;
    private final List<ProjectCheck.Item> problems = new ArrayList<>();
    private List<ProjectCheck.Item> fullCheck = List.of();
    private List<ProjectCheck.Item> live = List.of();
    private VisLabel status;
    private VisLabel cursor;
    private final List<VisTextButton> toolButtons = new ArrayList<>();
    private VisTable center;
    private FlowGroup edgeBar;
    private final Container<FlowGroup> edgeHolder = new Container<>();
    private final List<Button> edgeButtons = new ArrayList<>();
    private VisTable welcome;
    private boolean ignoreListEvents;

    EditorApp(Path startProject) {
        this.startProject = startProject;
    }

    // ------------------------------------------------------------------ запуск

    @Override
    public void create() {
        VisUI.setSkipGdxVersionCheck(true); // собран под 1.14.1, работает и с 1.14.2
        FileChooser.setDefaultPrefsName("com.zaig100.dargon.editor");
        prefs = Gdx.app.getPreferences(PREFS);
        uiScale = prefs.getFloat("uiScale", 1f);
        assets = new GameAssets();
        assets.load();
        engineFiles =
                path -> {
                    FileHandle file = Gdx.files.internal(path);
                    return file.exists() ? file.readString("UTF-8") : null;
                };
        engine = GameData.loadBuiltin(engineFiles);
        data = engine;
        buildUi();
        Path toOpen = startProject;
        if (toOpen == null && !prefs.getString("last", "").isEmpty()) {
            toOpen = Path.of(prefs.getString("last"));
        }
        if (toOpen != null && Files.isRegularFile(toOpen.resolve("pack.json"))) {
            open(toOpen);
        } else {
            showWelcome();
        }
    }

    /**
     * Интерфейс целиком: скин (шрифты под масштаб), сцена, виджеты. При смене масштаба собирается
     * заново; проект, уровень, инструмент и вид холста сохраняются.
     */
    private void buildUi() {
        LevelView old = view;
        if (scripts != null) {
            scripts.shutdown();
            scripts = null;
        }
        if (stage != null) {
            saveSplits();
            stage.dispose();
            VisUI.dispose();
        }
        float scale = Math.max(1f, Gdx.graphics.getBackBufferScale());
        EditorSkin.load(scale, uiScale);
        ScreenViewport viewport = new ScreenViewport();
        viewport.setUnitsPerPixel(1 / uiScale);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);
        // Виджеты VisUI — только после загрузки скина.
        levelList = new VisList<>();
        palette = new IconList(i -> i < paletteIds.size() ? prefabIcon(paletteIds.get(i)) : null);
        entityList = new IconList(this::entityIcon);
        problemList = new VisList<>();
        status = new VisLabel("");
        cursor = new VisLabel("");
        hint = new VisLabel("", "small");
        hint.setWrap(true);
        scaleLabel = new VisLabel("");
        toolButtons.clear();
        view = new LevelView(this, assets);
        if (old != null) {
            view.takeStateFrom(old);
            old.dispose();
        }
        inspector = new Inspector(this);
        buildLayout();
        stage.addListener(new Shortcuts());
        stage.getRoot().addCaptureListener(new FieldUndo());
        scaleLabel.setText(Math.round(uiScale * 100) + "%");
        if (project != null) {
            refreshLevels();
            refreshEntities();
            updateProblems();
            selectionChanged();
        } else if (old != null) {
            showWelcome();
        }
        updateTools();
    }

    private void setUiScale(int step) {
        int i = 0;
        for (int k = 0; k < UI_SCALES.length; k++) {
            if (Math.abs(UI_SCALES[k] - uiScale) < 0.01f) {
                i = k;
            }
        }
        int next = step == 0 ? 2 : Math.max(0, Math.min(UI_SCALES.length - 1, i + step));
        if (UI_SCALES[next] == uiScale) {
            return;
        }
        uiScale = UI_SCALES[next];
        prefs.putFloat("uiScale", uiScale).flush();
        // Не посреди обработки щелчка: сцена, которая его обрабатывает, будет удалена.
        Gdx.app.postRunnable(this::buildUi);
    }

    private VisSplitPane split(int index, Actor first, Actor second, boolean vertical) {
        VisSplitPane pane =
                new VisSplitPane(
                        first, second, vertical, vertical ? "wide-vertical" : "wide-horizontal");
        pane.setSplitAmount(prefs.getFloat(SPLITS[index], SPLIT_DEFAULTS[index]));
        pane.setMinSplitAmount(0.05f);
        pane.setMaxSplitAmount(0.95f);
        splits[index] = pane;
        return pane;
    }

    private void saveSplits() {
        for (int i = 0; i < splits.length; i++) {
            if (splits[i] != null) {
                prefs.putFloat(SPLITS[i], splits[i].getSplit());
            }
        }
        prefs.flush();
    }

    private void buildLayout() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.top().left();
        stage.addActor(root);

        FlowGroup toolbar = flow();
        toolbar.addActor(button("Новый пак", this::newPack));
        toolbar.addActor(button("Открыть…", this::chooseProject));
        toolbar.addActor(button("Сохранить (Ctrl+S)", this::save));
        toolbar.addActor(button("Отменить", this::undo));
        toolbar.addActor(button("Повторить", this::redo));
        toolbar.addActor(button("Свойства пака…", this::packProperties));
        toolbar.addActor(
                button(
                        "Префабы и текстуры…",
                        () -> {
                            if (project != null) {
                                stage.addActor(new PrefabDialog(this, project).fadeIn());
                            }
                        }));
        toolbar.addActor(button("Скрипты (F6)", () -> openScripts(-1, null)));
        toolbar.addActor(button("Проверить (F7)", this::check));
        VisTextButton play = button("Играть (F5)", this::play);
        play.setColor(new Color(0.6f, 1f, 0.6f, 1));
        toolbar.addActor(play);
        toolbar.addActor(button("Экспорт .dgpack…", this::export));
        toolbar.addActor(button("Справка (F1)", this::help));
        VisTable scaleBox = new VisTable();
        scaleBox.defaults().padLeft(3);
        scaleBox.add(new VisLabel("Масштаб:")).padLeft(12);
        scaleBox.add(button("−", () -> setUiScale(-1)));
        scaleBox.add(scaleLabel);
        scaleBox.add(button("+", () -> setUiScale(1)));
        toolbar.addActor(scaleBox);
        root.add(toolbar).growX().minWidth(0).pad(3).row();

        // Слева: уровни, что поставить (префабы), что уже стоит на карте.
        VisTable levelsBox = new VisTable();
        levelsBox.top().left();
        levelsBox.defaults().left().pad(2);
        levelsBox.add(new VisLabel("Уровни")).row();
        VisScrollPane levels = Form.wheel(new VisScrollPane(levelList));
        levels.setFadeScrollBars(false);
        levelsBox.add(levels).expand().fill().row();
        FlowGroup levelButtons = flow();
        levelButtons.addActor(button("Новый", this::addLevel));
        levelButtons.addActor(button("Удалить", this::removeLevel));
        levelButtons.addActor(button("Выше", () -> moveLevel(-1)));
        levelButtons.addActor(button("Ниже", () -> moveLevel(1)));
        levelButtons.addActor(button("Имя…", this::renameLevel));
        levelsBox.add(levelButtons).growX().minWidth(0).row();

        VisTable paletteBox = new VisTable();
        paletteBox.top().left();
        paletteBox.defaults().left().pad(2);
        VisTable paletteHead = new VisTable();
        paletteHead.add(new VisLabel("Поставить на карту")).left().expandX();
        cursorButton = button("Курсор (Esc)", this::stopPlacing);
        paletteHead.add(cursorButton).right();
        paletteBox.add(paletteHead).growX().minWidth(0).row();
        paletteBox.add(new VisLabel("выберите и щёлкните по клетке", "small")).row();
        VisScrollPane pal = Form.wheel(new VisScrollPane(palette));
        pal.setFadeScrollBars(false);
        paletteBox.add(pal).expand().fill().row();

        VisTable entitiesBox = new VisTable();
        entitiesBox.top().left();
        entitiesBox.defaults().left().pad(2);
        entitiesBox.add(new VisLabel("На карте")).row();
        VisScrollPane ents = Form.wheel(new VisScrollPane(entityList));
        ents.setFadeScrollBars(false);
        entitiesBox.add(ents).expand().fill().row();
        FlowGroup entityButtons = flow();
        entityButtons.addActor(button("Копия (Ctrl+D)", this::duplicateSelected));
        entityButtons.addActor(button("Удалить (Del)", this::deleteSelected));
        entitiesBox.add(entityButtons).growX().minWidth(0).row();
        entityList.addListener(
                changed(
                        () -> {
                            int i = entityList.getSelectedIndex();
                            if (!ignoreListEvents && i >= 0) {
                                setTool(LevelView.Tool.OBJECTS);
                                view.select(i);
                                view.reveal(i);
                            }
                        }));
        levelList.addListener(
                changed(
                        () -> {
                            if (!ignoreListEvents && levelList.getSelectedIndex() >= 0) {
                                showLevel(levelIds.get(levelList.getSelectedIndex()));
                            }
                        }));
        palette.addListener(
                changed(
                        () -> {
                            if (!ignoreListEvents && palette.getSelectedIndex() >= 0) {
                                view.setPrefab(paletteIds.get(palette.getSelectedIndex()));
                                updateTools();
                            }
                        }));
        fillPalette();

        // В центре: инструменты и холст.
        center = new VisTable();
        toolButton("Пол/стены (1)", LevelView.Tool.TILES);
        toolButton("Объекты (2)", LevelView.Tool.OBJECTS);
        toolButton("Старт героя (3)", LevelView.Tool.SPAWN);
        toolButton("Края стен (4)", LevelView.Tool.EDGES);
        edgeBar = buildEdgeBar();
        edgeHolder.fillX().left();
        fillCenter();

        VisScrollPane inspectorPane = Form.wheel(new VisScrollPane(inspector));
        inspectorPane.setFadeScrollBars(false);
        inspectorPane.setScrollingDisabled(true, false);

        // Снизу: проблемы.
        VisScrollPane problemPane = Form.wheel(new VisScrollPane(problemList));
        problemPane.setFadeScrollBars(false);

        // Все границы панелей перетаскиваются мышью.
        VisSplitPane left = split(3, levelsBox, split(4, paletteBox, entitiesBox, true), true);
        VisSplitPane middle = split(0, left, split(1, center, inspectorPane, false), false);
        root.add(split(2, middle, problemPane, true)).expand().fill().row();
        root.add(status).left().pad(3);
        problemList.addListener(
                changed(
                        () -> {
                            int i = problemList.getSelectedIndex();
                            if (!ignoreListEvents && i >= 0 && i < problems.size()) {
                                goTo(problems.get(i));
                            }
                        }));
        updateTools();
    }

    /** Центр: инструменты, палитра краёв (у инструмента краёв) и холст. */
    private void fillCenter() {
        center.clearChildren();
        FlowGroup tools = flow();
        for (VisTextButton b : toolButtons) {
            tools.addActor(b);
        }
        tools.addActor(button("−", () -> view.zoomStep(-1)));
        tools.addActor(button("+", () -> view.zoomStep(1)));
        tools.addActor(button("Вся карта", view::fit));
        tools.addActor(cursor);
        center.add(tools).growX().minWidth(0).pad(2).row();
        center.add(hint).growX().minWidth(0).left().pad(0, 4, 2, 4).row();
        center.add(edgeHolder).growX().minWidth(0).row();
        center.add(view).expand().fill().minSize(0);
    }

    /** Края стен: «авто», «без края» и восемь краёв тайлсета картинками. */
    private FlowGroup buildEdgeBar() {
        FlowGroup bar = flow();
        bar.addActor(new VisLabel("Край:"));
        edgeButtons.clear();
        edgeButton(bar, '.', null, "Авто");
        edgeButton(bar, '#', null, "Без края");
        for (WallEdge e : WallEdge.values()) {
            if (e != WallEdge.NONE) {
                edgeButton(
                        bar, e.symbol(), assets.tile(e.column, e.row), String.valueOf(e.symbol()));
            }
        }
        return bar;
    }

    private void edgeButton(FlowGroup bar, char symbol, TextureRegion tile, String label) {
        Button b;
        if (tile == null) {
            b = new VisTextButton(label);
        } else {
            VisImageButton ib = new VisImageButton(new TextureRegionDrawable(tile));
            ib.getImageCell().size(32, 32);
            b = ib;
        }
        b.setUserObject(symbol);
        b.addListener(
                changed(
                        () -> {
                            view.setEdge(symbol);
                            updateTools();
                        }));
        edgeButtons.add(b);
        bar.addActor(b);
    }

    private void fillPalette() {
        Array<String> items = new Array<>();
        paletteIds.clear();
        List<String> ids = new ArrayList<>(data.packPrefabs());
        for (String id : new java.util.TreeSet<>(data.prefabs().ids())) {
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        for (String id : ids) {
            Prefab p = data.prefabs().get(id);
            String d = p.description() == null ? "" : p.description().split("[:.(]", 2)[0];
            items.add((data.packPrefabs().contains(id) ? "[пак] " : "") + id + " — " + d);
            paletteIds.add(id);
        }
        ignoreListEvents = true;
        palette.getSelection().setRequired(false);
        palette.setItems(items);
        palette.getSelection().clear();
        ignoreListEvents = false;
    }

    private VisTextButton toolButton(String label, LevelView.Tool tool) {
        VisTextButton b = button(label, () -> setTool(tool));
        b.setUserObject(tool);
        toolButtons.add(b);
        return b;
    }

    private void setTool(LevelView.Tool tool) {
        view.setTool(tool);
        updateTools();
    }

    private void updateTools() {
        for (VisTextButton b : toolButtons) {
            b.setColor(
                    b.getUserObject() == view.tool() ? new Color(1, 0.85f, 0.4f, 1) : Color.WHITE);
        }
        for (Button b : edgeButtons) {
            b.setColor(
                    b.getUserObject().equals(view.edge())
                            ? new Color(1, 0.85f, 0.4f, 1)
                            : Color.WHITE);
        }
        edgeHolder.setActor(view.tool() == LevelView.Tool.EDGES ? edgeBar : null);
        hint.setText(toolHint());
        // «Курсор» подсвечен, когда ничего не ставится: щелчки только выделяют и двигают.
        cursorButton.setColor(
                view.tool() == LevelView.Tool.OBJECTS && view.prefab() == null
                        ? new Color(1, 0.85f, 0.4f, 1)
                        : Color.WHITE);
    }

    /** Подсказка над холстом: что делает мышь сейчас. */
    private String toolHint() {
        String common =
                "  Колесо — масштаб, средняя кнопка или пробел + мышь — сдвиг. F1 — справка.";
        return switch (view.tool()) {
            case TILES -> "Пол и стены: ЛКМ — пол, ПКМ — стена, можно вести мышью." + common;
            case SPAWN -> "Старт героя: ЛКМ по клетке — здесь герой появится." + common;
            case EDGES ->
                    "Края стен: выберите край ниже; ЛКМ — поставить на стену, ПКМ — снова «авто»."
                            + common;
            case OBJECTS ->
                    view.isAreaPrefab(view.prefab())
                            ? (view.isPlacingArea()
                                            ? "Область «"
                                                    + view.prefab()
                                                    + "»: щёлкните в противоположный угол. ПКМ или"
                                                    + " Esc — отменить."
                                            : "Область «"
                                                    + view.prefab()
                                                    + "»: щёлкните в один угол, потом в"
                                                    + " противоположный (или протяните мышью)."
                                                    + " ПКМ или Esc — снова курсор.")
                                    + common
                            : view.prefab() == null
                                    ? "Объекты: выберите слева в «Поставить на карту», что поставить, и"
                                            + " щёлкните по пустой клетке. Щелчок по объекту — выделить"
                                            + " (свойства справа), перетащить — сдвинуть, Del — удалить."
                                            + common
                                    : "Ставится «"
                                            + view.prefab()
                                            + "»: щёлкните по пустой клетке (можно несколько раз). Щелчок"
                                            + " по объекту — выделить, перетащить — сдвинуть. Esc или ПКМ —"
                                            + " снова курсор."
                                            + common;
        };
    }

    private void showWelcome() {
        welcome = new VisTable();
        welcome.add(new VisLabel("Откройте каталог пака или создайте новый пак"))
                .padBottom(10)
                .row();
        VisTable b = new VisTable();
        b.defaults().pad(4);
        b.add(button("Новый пак…", this::newPack));
        b.add(button("Открыть пак…", this::chooseProject));
        b.add(button("Открыть кампанию DarGon", this::openBuiltinCopyHint));
        welcome.add(b);
        center.clearChildren();
        center.add(welcome).expand();
    }

    private void openBuiltinCopyHint() {
        Dialogs.showOKDialog(
                stage,
                "Кампания DarGon",
                "Встроенная кампания лежит в репозитории игры: assets/packs/dargon.\n"
                        + "Откройте этот каталог через «Открыть пак…» — или скопируйте его, чтобы"
                        + " сделать свой пак на его основе (поменяйте id в свойствах пака).");
    }

    // ------------------------------------------------------------------ проект

    private void open(Path dir) {
        try {
            PackProject next = PackProject.open(dir);
            project = next;
        } catch (IOException | RuntimeException e) {
            Dialogs.showErrorDialog(stage, "Пак не открылся: " + e.getMessage());
            return;
        }
        prefs.putString("last", dir.toAbsolutePath().toString()).flush();
        if (welcome != null) {
            fillCenter();
            welcome = null;
        }
        fullCheck = List.of();
        reloadData();
        refreshLevels();
        List<String> ids = project.levelIds();
        String first =
                ids.contains(String.valueOf(project.pack().get("start")))
                        ? String.valueOf(project.pack().get("start"))
                        : ids.isEmpty() ? null : ids.get(0);
        showLevel(first);
        status("Открыт пак " + project.id() + " (" + dir + ")");
        updateTitle();
    }

    /** Перед тем как бросить проект: сохранить? {@code then} — если можно продолжать. */
    private void confirmDiscard(Runnable then) {
        if (project == null || !project.isDirty()) {
            then.run();
            return;
        }
        Dialogs.showOptionDialog(
                stage,
                "Несохранённые изменения",
                "Сохранить изменения в паке «" + project.id() + "»?",
                Dialogs.OptionDialogType.YES_NO_CANCEL,
                new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        if (save()) {
                            then.run();
                        }
                    }

                    @Override
                    public void no() {
                        then.run();
                    }
                });
    }

    private void chooseProject() {
        confirmDiscard(
                () -> {
                    FileChooser chooser = new FileChooser("Каталог пака", FileChooser.Mode.OPEN);
                    chooser.setSelectionMode(FileChooser.SelectionMode.DIRECTORIES);
                    if (project != null) {
                        chooser.setDirectory(project.dir().getParent().toFile());
                    }
                    chooser.setListener(
                            new FileChooserAdapter() {
                                @Override
                                public void selected(Array<FileHandle> files) {
                                    Path dir = files.first().file().toPath();
                                    if (!Files.isRegularFile(dir.resolve("pack.json"))) {
                                        Dialogs.showErrorDialog(
                                                stage, "В каталоге " + dir + " нет pack.json");
                                        return;
                                    }
                                    open(dir);
                                }
                            });
                    stage.addActor(chooser.fadeIn());
                });
    }

    private void newPack() {
        confirmDiscard(
                () -> {
                    FileChooser chooser =
                            new FileChooser("Где создать пак (каталог)", FileChooser.Mode.OPEN);
                    chooser.setSelectionMode(FileChooser.SelectionMode.DIRECTORIES);
                    chooser.setListener(
                            new FileChooserAdapter() {
                                @Override
                                public void selected(Array<FileHandle> files) {
                                    askNewPack(files.first().file().toPath());
                                }
                            });
                    stage.addActor(chooser.fadeIn());
                });
    }

    private void askNewPack(Path parent) {
        Dialogs.showInputDialog(
                stage,
                "Новый пак",
                "id пака (латиница, цифры, _): ",
                new InputValidator() {
                    @Override
                    public boolean validateInput(String input) {
                        return input.matches("[a-z][a-z0-9_]*")
                                && !Files.exists(parent.resolve(input));
                    }
                },
                new InputDialogAdapter() {
                    @Override
                    public void finished(String id) {
                        try {
                            PackProject.create(parent.resolve(id), id, id);
                            open(parent.resolve(id));
                            packProperties();
                        } catch (IOException e) {
                            Dialogs.showErrorDialog(
                                    stage, "Не удалось создать пак: " + e.getMessage());
                        }
                    }
                });
    }

    boolean save() {
        if (project == null) {
            return true;
        }
        try {
            project.save();
            status("Сохранено: " + project.dir());
            updateTitle();
            refreshLevels();
            return true;
        } catch (IOException | RuntimeException e) {
            Dialogs.showErrorDialog(stage, "Не сохранилось: " + e.getMessage());
            return false;
        }
    }

    private void packProperties() {
        if (project != null) {
            stage.addActor(new PackDialog(this, project, data).fadeIn());
        }
    }

    /** Свойства пака изменились (диалог). */
    void packEdited() {
        refreshLevels();
        updateTitle();
        selectionChanged();
    }

    // ------------------------------------------------------------------ уровни

    void refreshLevels() {
        if (project == null) {
            return;
        }
        ignoreListEvents = true;
        levelIds.clear();
        levelIds.addAll(project.levelIds());
        Array<String> items = new Array<>();
        String start = String.valueOf(project.pack().get("start"));
        for (String id : levelIds) {
            LevelDoc doc = project.level(id);
            String name = levelName(id);
            items.add(
                    id
                            + (name.isEmpty() ? "" : " — " + name)
                            + (id.equals(start) ? "  [старт]" : "")
                            + (doc != null && doc.isDirty() ? " *" : "")
                            + (doc == null ? " (нет файла)" : ""));
        }
        levelList.setItems(items);
        LevelDoc current = view.level();
        if (current != null) {
            levelList.setSelectedIndex(levelIds.indexOf(current.id()));
        }
        ignoreListEvents = false;
    }

    private String levelName(String id) {
        LevelDoc doc = project.level(id);
        if (doc == null || !(doc.root().get("name") instanceof String name)) {
            return "";
        }
        if (name.startsWith("@")) {
            String t = project.text("ru", name.substring(1));
            return t == null ? "" : t;
        }
        return name;
    }

    private void showLevel(String id) {
        LevelDoc doc = id == null ? null : project.level(id);
        if (scripts != null && (scripts.level() != doc || !scripts.levelId().equals(id))) {
            scripts.shutdown();
            scripts.remove();
            scripts = null;
        }
        view.setLevel(doc);
        live = List.of();
        refreshLevels();
        refreshEntities();
        updateProblems();
        selectionChanged();
    }

    private void addLevel() {
        if (project == null) {
            return;
        }
        Dialogs.showInputDialog(
                stage,
                "Новый уровень",
                "id уровня (например 05 или shops/shop_1): ",
                levelIdValidator(),
                new InputDialogAdapter() {
                    @Override
                    public void finished(String id) {
                        String after = view.level() == null ? null : view.level().id();
                        project.addLevel(id, after);
                        showLevel(id);
                    }
                });
    }

    private InputValidator levelIdValidator() {
        return input ->
                input.matches("[A-Za-z0-9_]+(/[A-Za-z0-9_]+)*")
                        && !project.levelIds().contains(input);
    }

    private void removeLevel() {
        LevelDoc doc = view.level();
        if (project == null || doc == null) {
            return;
        }
        Dialogs.showOptionDialog(
                stage,
                "Удалить уровень",
                "Удалить уровень " + doc.id() + "? Файл удалится при сохранении.",
                Dialogs.OptionDialogType.YES_CANCEL,
                new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        project.removeLevel(doc.id());
                        List<String> ids = project.levelIds();
                        showLevel(ids.isEmpty() ? null : ids.get(0));
                    }
                });
    }

    private void moveLevel(int delta) {
        if (project != null && view.level() != null) {
            project.moveLevel(view.level().id(), delta);
            refreshLevels();
        }
    }

    private void renameLevel() {
        LevelDoc doc = view.level();
        if (project == null || doc == null) {
            return;
        }
        Dialogs.showInputDialog(
                        stage,
                        "Переименовать уровень",
                        "Новый id (ссылки на уровень обновятся): ",
                        levelIdValidator(),
                        new InputDialogAdapter() {
                            @Override
                            public void finished(String id) {
                                project.renameLevel(doc.id(), id, data.prefabs());
                                showLevel(id);
                            }
                        })
                .setText(doc.id());
    }

    // ------------------------------------------------------------------ правки

    /** Выделение или уровень изменились — свойства справа заново. */
    void selectionChanged() {
        inspector.show(project, view.level(), view.selected());
        int i = view.selected();
        ignoreListEvents = true;
        if (i >= 0 && i < entityList.getItems().size) {
            entityList.setSelectedIndex(i);
        } else {
            entityList.getSelection().clear();
        }
        ignoreListEvents = false;
    }

    /** «На карте»: сущности уровня по порядку — префаб, id, клетка. */
    private void refreshEntities() {
        LevelDoc doc = view.level();
        Array<String> items = new Array<>();
        if (doc != null) {
            for (Map<String, Object> e : doc.entities()) {
                int[] at = LevelDoc.at(e);
                Prefab p = data.prefabs().get(String.valueOf(e.get("prefab")));
                String d =
                        p == null || p.description() == null
                                ? ""
                                : p.description().split("[:.(]", 2)[0];
                items.add(
                        e.get("prefab")
                                + (e.get("id") == null ? "" : " «" + e.get("id") + "»")
                                + "  ["
                                + at[0]
                                + ", "
                                + at[1]
                                + "]"
                                + (d.isEmpty() ? "" : " — " + d));
            }
        }
        ignoreListEvents = true;
        entityList.getSelection().setRequired(false);
        entityList.setItems(items);
        int i = view.selected();
        if (i >= 0 && i < items.size) {
            entityList.setSelectedIndex(i);
        } else {
            entityList.getSelection().clear();
        }
        ignoreListEvents = false;
    }

    /** Правка из свойств: холст перерисуется сам; обновить список (звёздочка «изменён»). */
    void levelEdited() {
        updateTitle();
    }

    /**
     * Новая сущность: обязательные параметры без значения по умолчанию — сразу разумные значения,
     * чтобы она не была «сломанной» с первой секунды.
     */
    @SuppressWarnings("unchecked")
    void prefill(LevelDoc doc, Map<String, Object> e) {
        Prefab p = data.prefabs().get(String.valueOf(e.get("prefab")));
        if (p == null) {
            return;
        }
        for (com.zaig100.dargon.level.ParamSpec spec : p.params().values()) {
            if (!spec.isRequired() || e.containsKey(spec.name())) {
                continue;
            }
            Object value =
                    switch (spec.type()) {
                        case INT ->
                                (long) Math.max(1, spec.min() == null ? 1 : spec.min().intValue());
                        case NUMBER -> Math.max(1.0, spec.min() == null ? 1.0 : spec.min());
                        case BOOL -> false;
                        case DIRECTION, DIRECTION8 -> "right";
                        case COLOR -> "#FFFFFF";
                        case POINT -> LevelDoc.at(e).length == 2 ? e.get("at") : null;
                        case LEVEL -> nextLevel(doc.id());
                        case STRING ->
                                spec.values() != null && !spec.values().isEmpty()
                                        ? spec.values().get(0)
                                        : spec.name().equals("key") ? "key1" : "";
                        case ITEM ->
                                new java.util.LinkedHashMap<>(Map.of("id", "coins", "count", 1L));
                        case ITEMS, OFFERS -> new ArrayList<>();
                        case TEXT -> {
                            String key =
                                    project.freeKey(
                                            "level." + doc.id().replace('/', '.') + "." + p.id());
                            project.text("ru", key, "...");
                            project.text("en", key, "...");
                            yield "@" + key;
                        }
                        default -> null;
                    };
            if (value != null) {
                e.put(
                        spec.name(),
                        value instanceof List<?> l ? new ArrayList<>((List<Object>) l) : value);
            }
        }
    }

    /** Следующий уровень пака после {@code id} (для лестниц), иначе он сам. */
    private String nextLevel(String id) {
        List<String> ids = project.levelIds();
        int i = ids.indexOf(id);
        return i >= 0 && i + 1 < ids.size() ? ids.get(i + 1) : id;
    }

    void deleteSelected() {
        view.deleteSelected();
    }

    void duplicateSelected() {
        view.duplicateSelected();
    }

    private void undo() {
        if (view.level() != null && view.level().undo()) {
            view.select(Math.min(view.selected(), view.level().entities().size() - 1));
            status("Отменено");
        }
    }

    private void redo() {
        if (view.level() != null && view.level().redo()) {
            view.select(Math.min(view.selected(), view.level().entities().size() - 1));
            status("Повторено");
        }
    }

    // ------------------------------------------------------------------ проблемы

    /** Разбор текущего уровня после правки (холст пересобирает уровень). */
    void liveProblems(LevelDoc doc, Problems parsed) {
        List<ProjectCheck.Item> items = new ArrayList<>();
        for (Problem p : parsed.list()) {
            items.add(new ProjectCheck.Item(p, doc.id(), entityIndex(p.path())));
        }
        live = items;
        updateProblems();
        updateTitle();
        refreshEntities();
        refreshLevelsLater();
    }

    private boolean levelsRefreshQueued;

    private void refreshLevelsLater() {
        if (!levelsRefreshQueued) {
            levelsRefreshQueued = true;
            Gdx.app.postRunnable(
                    () -> {
                        levelsRefreshQueued = false;
                        refreshLevels();
                    });
        }
    }

    private static int entityIndex(String path) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("^entities\\[(\\d+)]")
                        .matcher(path == null ? "" : path);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private void check() {
        if (project == null) {
            return;
        }
        fullCheck = ProjectCheck.run(engineFiles, project, assets::has);
        live = List.of();
        updateProblems();
        long errors = fullCheck.stream().filter(ProjectCheck.Item::isError).count();
        status(
                fullCheck.isEmpty()
                        ? "Проверка: всё в порядке"
                        : "Проверка: ошибок "
                                + errors
                                + ", предупреждений "
                                + (fullCheck.size() - errors));
    }

    private void updateProblems() {
        String current = view.level() == null ? null : view.level().id();
        problems.clear();
        problems.addAll(live);
        for (ProjectCheck.Item item : fullCheck) {
            // Текущий уровень уже разобран заново — старые сообщения о нём не показываем.
            if (live.isEmpty() || item.level() == null || !item.level().equals(current)) {
                problems.add(item);
            }
        }
        Array<String> lines = new Array<>();
        List<int[]> cells = new ArrayList<>();
        for (ProjectCheck.Item item : problems) {
            Problem p = item.problem();
            lines.add(
                    (p.isError() ? "ОШИБКА  " : "внимание  ")
                            + p.file()
                            + (p.path() == null || p.path().isEmpty() ? "" : " " + p.path())
                            + ": "
                            + p.message());
            if (current != null
                    && current.equals(item.level())
                    && item.entity() >= 0
                    && item.entity() < view.level().entities().size()) {
                cells.add(LevelDoc.at(view.level().entities().get(item.entity())));
            }
        }
        ignoreListEvents = true;
        problemList.setItems(lines);
        problemList.getSelection().setRequired(false);
        problemList.getSelection().clear();
        ignoreListEvents = false;
        view.setProblemCells(cells);
    }

    private void goTo(ProjectCheck.Item item) {
        if (item.level() == null) {
            if (item.problem().file().equals("pack.json")) {
                packProperties();
            }
            return;
        }
        if (view.level() == null || !view.level().id().equals(item.level())) {
            showLevel(item.level());
        }
        if (item.entity() >= 0) {
            view.select(item.entity());
        }
    }

    // ------------------------------------------------------------------ игра и экспорт

    private void play() {
        LevelDoc level = view.level();
        if (project == null || level == null) {
            return;
        }
        if (!save()) {
            return;
        }
        List<ProjectCheck.Item> items = ProjectCheck.run(engineFiles, project, assets::has);
        if (ProjectCheck.hasErrors(items)) {
            fullCheck = items;
            live = List.of();
            updateProblems();
            status("В паке есть ошибки — игра его не откроет. Исправьте их (список внизу).");
            return;
        }
        game.open(project.dir(), level.id(), error -> status("Пробный запуск упал: " + error));
        status(
                "Игра запущена: уровень "
                        + level.id()
                        + " (окно игры можно закрыть в любой момент)");
    }

    private void export() {
        if (project == null || !save()) {
            return;
        }
        fullCheck = ProjectCheck.run(engineFiles, project, assets::has);
        live = List.of();
        updateProblems();
        if (ProjectCheck.hasErrors(fullCheck)) {
            Dialogs.showErrorDialog(
                    stage, "В паке есть ошибки — исправьте их перед экспортом (список внизу).");
            return;
        }
        FileChooser chooser = new FileChooser("Сохранить пак как", FileChooser.Mode.SAVE);
        chooser.setSelectionMode(FileChooser.SelectionMode.FILES);
        chooser.setDirectory(project.dir().getParent().toFile());
        chooser.setDefaultFileName(
                project.id() + "-" + project.pack().getOrDefault("version", "1.0") + ".dgpack");
        chooser.setListener(
                new FileChooserAdapter() {
                    @Override
                    public void selected(Array<FileHandle> files) {
                        Path target = files.first().file().toPath();
                        if (!target.getFileName().toString().endsWith(".dgpack")) {
                            target = target.resolveSibling(target.getFileName() + ".dgpack");
                        }
                        try {
                            PackExporter.export(project.dir(), target);
                            Dialogs.showOKDialog(
                                    stage,
                                    "Экспорт",
                                    "Пак сохранён:\n"
                                            + target
                                            + "\n\nНа ПК положите файл в ~/.dargon/packs, на"
                                            + " Android — «Паки» → «Добавить пак».");
                        } catch (IOException e) {
                            Dialogs.showErrorDialog(
                                    stage, "Не удалось сохранить: " + e.getMessage());
                        }
                    }
                });
        stage.addActor(chooser.fadeIn());
    }

    // ------------------------------------------------------------------ строка состояния

    void status(String text) {
        status.setText(text);
    }

    void hover(int x, int y) {
        LevelDoc doc = view.level();
        String what = doc.inBounds(x, y) ? (doc.isFloor(x, y) ? "пол" : "стена") : "за картой";
        char edge = doc.edgeAt(x, y);
        if (edge != '.') {
            what += ", край вручную: " + (edge == '#' ? "нет" : String.valueOf(edge));
        }
        cursor.setText("[" + x + ", " + y + "] " + what + "  · ×" + view.zoom());
    }

    private void updateTitle() {
        if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
            return;
        }
        String title =
                project == null
                        ? TITLE
                        : TITLE + " — " + project.id() + (project.isDirty() ? " *" : "");
        g.getWindow().setTitle(title);
    }

    /** Окно закрывают: несохранённое — спросить. */
    boolean requestClose() {
        if (project == null || !project.isDirty()) {
            return true;
        }
        confirmDiscard(() -> Gdx.app.exit());
        return false;
    }

    // ------------------------------------------------------------------ цикл

    @Override
    public void render() {
        ScreenUtils.clear(0.12f, 0.12f, 0.13f, 1);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (scripts != null) {
            scripts.shutdown();
        }
        saveSplits();
        view.dispose();
        stage.dispose();
        assets.dispose();
        VisUI.dispose();
    }

    // ------------------------------------------------------------------ клавиши

    private final class Shortcuts extends InputListener {
        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            // Поля VisUI — свой класс, не наследник TextField: проверяем оба. Иначе ввод в поле
            // свойств срабатывал как горячие клавиши (цифры — инструменты, стирание — удаление).
            Actor focus = stage.getKeyboardFocus();
            boolean typing =
                    focus instanceof TextField
                            || focus instanceof com.kotcrab.vis.ui.widget.VisTextField;
            if (UIUtils.ctrl()) {
                switch (keycode) {
                    case Keys.S -> save();
                    case Keys.Z -> {
                        if (typing) {
                            return false;
                        }
                        if (UIUtils.shift()) {
                            redo();
                        } else {
                            undo();
                        }
                    }
                    case Keys.Y -> {
                        if (typing) {
                            return false;
                        }
                        redo();
                    }
                    case Keys.D -> {
                        if (typing) {
                            return false;
                        }
                        duplicateSelected();
                    }
                    case Keys.O -> chooseProject();
                    case Keys.EQUALS, Keys.PLUS, Keys.NUMPAD_ADD -> setUiScale(1);
                    case Keys.MINUS, Keys.NUMPAD_SUBTRACT -> setUiScale(-1);
                    case Keys.NUM_0, Keys.NUMPAD_0 -> setUiScale(0);
                    default -> {
                        return false;
                    }
                }
                return true;
            }
            switch (keycode) {
                case Keys.F1 -> help();
                case Keys.F5 -> play();
                case Keys.F6 -> openScripts(view.selected(), null);
                case Keys.F7 -> check();
                default -> {
                    if (typing) {
                        return false;
                    }
                    switch (keycode) {
                        case Keys.NUM_1 -> setTool(LevelView.Tool.TILES);
                        case Keys.NUM_2 -> setTool(LevelView.Tool.OBJECTS);
                        case Keys.NUM_3 -> setTool(LevelView.Tool.SPAWN);
                        case Keys.NUM_4 -> setTool(LevelView.Tool.EDGES);
                        case Keys.FORWARD_DEL -> deleteSelected();
                        case Keys.SPACE -> view.setSpaceHeld(true);
                        case Keys.ESCAPE -> {
                            // Сначала отменяется начатая область, потом — выбор префаба.
                            if (view.cancelArea()) {
                                updateTools();
                            } else {
                                view.select(-1);
                                stopPlacing();
                            }
                        }
                        case Keys.EQUALS, Keys.PLUS, Keys.NUMPAD_ADD -> view.zoomStep(1);
                        case Keys.MINUS, Keys.NUMPAD_SUBTRACT -> view.zoomStep(-1);
                        case Keys.F -> view.fit();
                        default -> {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public boolean keyUp(InputEvent event, int keycode) {
            if (keycode == Keys.SPACE) {
                view.setSpaceHeld(false);
            }
            return false;
        }
    }

    /**
     * Ctrl+Z / Ctrl+Y в поле ввода. У полей VisUI своя «отмена»: в только что открытом поле она
     * откатывает текст к пустому, и пустое значение уходило в уровень (стирался id, ключ…). Поэтому
     * перехватываем раньше поля: в главном окне — отмена и повтор правок уровня, в диалогах и окне
     * скриптов — ничего.
     */
    private final class FieldUndo extends InputListener {
        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            if (!UIUtils.ctrl() || (keycode != Keys.Z && keycode != Keys.Y)) {
                return false;
            }
            Actor target = event.getTarget();
            if (!(target instanceof com.kotcrab.vis.ui.widget.VisTextField)
                    && !(target instanceof TextField)) {
                return false;
            }
            event.cancel();
            boolean inWindow = false;
            for (Actor a = target; a != null; a = a.getParent()) {
                inWindow |= a instanceof com.badlogic.gdx.scenes.scene2d.ui.Window;
            }
            if (!inWindow) {
                if (keycode == Keys.Y || UIUtils.shift()) {
                    redo();
                } else {
                    undo();
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------ мелочи

    /**
     * Окно скриптов текущего уровня.
     *
     * @param entity объект, чей обработчик открыть, или -1 — скрипт уровня
     * @param key поле обработчика ({@code onUse}) или {@code null} — первое
     */
    void openScripts(int entity, String key) {
        LevelDoc doc = view.level();
        if (project == null || doc == null) {
            return;
        }
        if (scripts == null) {
            scripts = new ScriptWindow(this, project, doc, breakpoints);
            stage.addActor(scripts.fadeIn());
        } else {
            scripts.toFront();
        }
        scripts.select(entity, key);
    }

    /** Начали или закончили область — обновить подсказку. */
    void areaChanged() {
        updateTools();
    }

    void scriptWindowClosed(ScriptWindow window) {
        if (scripts == window) {
            scripts = null;
        }
    }

    /** Перестать ставить префаб: щелчок по пустой клетке больше ничего не ставит. */
    void stopPlacing() {
        view.setPrefab(null);
        ignoreListEvents = true;
        palette.getSelection().clear();
        ignoreListEvents = false;
        updateTools();
    }

    /** Ряд кнопок, который переносится на следующую строку, если не помещается. */
    private static FlowGroup flow() {
        return new FlowGroup(false, 3);
    }

    private void help() {
        HelpDialog.show(stage);
    }

    /** Картинка префаба для списков и «призрака» на холсте; нет картинки — {@code null}. */
    TextureRegion prefabIcon(String id) {
        return spriteOf(data.prefabs().get(id), null);
    }

    private TextureRegion entityIcon(int index) {
        LevelDoc doc = view.level();
        if (doc == null || index >= doc.entities().size()) {
            return null;
        }
        Map<String, Object> e = doc.entities().get(index);
        return spriteOf(data.prefabs().get(String.valueOf(e.get("prefab"))), e);
    }

    /** SpriteRef префаба: имя картинки и кадр, ссылки на параметры — из сущности или умолчаний. */
    private TextureRegion spriteOf(Prefab p, Map<String, Object> entity) {
        if (p == null || !p.hasComponent("SpriteRef")) {
            return null;
        }
        JsonValue sprite = p.components().get("SpriteRef");
        Object region = paramValue(p, entity, sprite.get("region"));
        Object frame = paramValue(p, entity, sprite.get("frame"));
        if (!(region instanceof String name) || !assets.has(name)) {
            return null;
        }
        try {
            return assets.frame(name, frame instanceof Number n ? n.intValue() : 0);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Object paramValue(Prefab p, Map<String, Object> entity, JsonValue v) {
        if (v == null) {
            return null;
        }
        if (v.isString() && v.asString().startsWith(Prefab.PARAM_REF)) {
            String name = v.asString().substring(Prefab.PARAM_REF.length());
            if (entity != null && entity.containsKey(name)) {
                return entity.get(name);
            }
            com.zaig100.dargon.level.ParamSpec spec = p.params().get(name);
            return spec == null ? null : paramValue(p, null, spec.defaultValue());
        }
        return v.isString() ? v.asString() : v.isNumber() ? (Object) v.asLong() : null;
    }

    private static VisTextButton button(String text, Runnable action) {
        VisTextButton b = new VisTextButton(text);
        b.addListener(changed(action));
        return b;
    }

    static ChangeListener changed(Runnable r) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r.run();
            }
        };
    }

    /** Данные проекта (игра + пак). */
    GameData data() {
        return data;
    }

    /**
     * Пересобрать данные проекта: после открытия пака и правок префабов и текстур. Палитра и
     * картинки — заново.
     */
    void reloadData() {
        data = engine;
        String error = null;
        if (project != null) {
            try {
                data = GameData.load(engineFiles, project.reader());
            } catch (RuntimeException e) {
                error = e.getMessage();
            }
            java.util.Map<String, byte[]> textures = new java.util.LinkedHashMap<>();
            for (String name : project.textures()) {
                byte[] png = project.textureBytes(name);
                if (png != null) {
                    textures.put(name, png);
                }
            }
            try {
                assets.usePackTextures(textures);
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
                assets.usePackTextures(java.util.Map.of());
            }
        }
        fillPalette();
        view.dataChanged();
        if (error != null) {
            status("Пак не разобрался (показаны только префабы игры): " + error);
        }
    }

    /** Картинки атласа игры — для выбора в префабах. */
    GameAssets assets() {
        return assets;
    }

    Stage stage() {
        return stage;
    }

    /** Сущности уровня — для диалогов. */
    List<Map<String, Object>> entities() {
        return view.level() == null ? List.of() : view.level().entities();
    }
}
