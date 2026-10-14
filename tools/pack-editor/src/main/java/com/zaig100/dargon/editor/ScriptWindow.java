package com.zaig100.dargon.editor;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.layout.FlowGroup;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisList;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.zaig100.dargon.editor.model.LevelDoc;
import com.zaig100.dargon.editor.model.PackProject;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.script.ScriptHost;
import com.zaig100.dargon.level.LevelDefinition;
import com.zaig100.dargon.level.Prefab;
import com.zaig100.dargon.level.Problem;
import com.zaig100.dargon.level.Problems;
import com.zaig100.dargon.script.ScriptEngine;
import com.zaig100.dargon.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Скрипты уровня: файл скрипта и обработчики объектов в одном окне — с подсветкой, проверкой на
 * лету, точками остановки и запуском уровня в песочнице под отладчиком.
 */
final class ScriptWindow extends VisWindow {

    /**
     * Скрипт, который можно открыть: файл уровня, общий файл пака или обработчик объекта.
     *
     * @param entity номер объекта в уровне ({@link #LEVEL_SCRIPT}, {@link #SHARED} — файлы)
     * @param ref сам объект уровня: номер мог сдвинуться (удалили объект) или объект заменён
     *     (отмена в главном окне) — тогда запись устарела
     */
    private record Entry(
            String label,
            String file,
            String path,
            int entity,
            String key,
            Map<String, Object> ref) {
        boolean isLevelScript() {
            return entity == LEVEL_SCRIPT;
        }

        boolean isShared() {
            return entity == SHARED;
        }

        /** Файл .dgs, а не обработчик в уровне. */
        boolean isFile() {
            return entity < 0;
        }
    }

    private static final int LEVEL_SCRIPT = -1;
    private static final int SHARED = -2;

    private static final Pattern LOCATION = Pattern.compile("^(.*):(\\d+):(\\d+): ");
    private static final int CONSOLE_LIMIT = 400;

    private final EditorApp app;
    private final PackProject project;
    private final LevelDoc doc;
    private final String levelId;
    private final Map<String, Set<Integer>> breakpoints;

    private final List<Entry> entries = new ArrayList<>();
    private final VisList<String> fileList = new VisList<>();
    private final CodeEditor editor = new CodeEditor();
    private final VisLabel check = new VisLabel("", "small");
    private final VisLabel state = new VisLabel("");
    private final VisTable console = new VisTable();
    private final VisScrollPane consolePane;
    private final VisTextField input = new VisTextField();
    private final VisList<String> stack = new VisList<>();
    private final VisTable variables = new VisTable();
    private final VisCheckBox time = new VisCheckBox("Время идёт");
    private final SandboxView sandbox;
    private final List<VisTextButton> pausedOnly = new ArrayList<>();
    private final List<String> history = new ArrayList<>();
    private int historyIndex;

    private Entry current;
    private Set<String> shownUses = Set.of();
    private boolean loading;
    private boolean checkpointed;
    private DebugSession session;
    private DebugSession.Pause shownPause;
    private float ticks;

    /** Скрипты правились после запуска песочницы: она работает со старым текстом. */
    private boolean editedSinceStart;

    ScriptWindow(
            EditorApp app,
            PackProject project,
            LevelDoc doc,
            Map<String, Set<Integer>> breakpoints) {
        super("Скрипты уровня " + doc.id());
        this.app = app;
        this.project = project;
        this.doc = doc;
        this.levelId = doc.id();
        this.breakpoints = breakpoints;
        sandbox = new SandboxView(app.assets(), app.data().items(), () -> session);
        addCloseButton();
        setResizable(true);
        setModal(false);

        // Слева — файлы, в центре — код, справа — отладка, снизу — консоль.
        VisTable files = new VisTable();
        files.top().left();
        files.defaults().left().pad(2);
        files.add(new VisLabel("Скрипты")).row();
        VisScrollPane filePane = Form.wheel(new VisScrollPane(fileList));
        filePane.setFadeScrollBars(false);
        files.add(filePane).grow().row();
        files.add(button("Новый общий…", this::newShared)).row();

        FlowGroup bar = new FlowGroup(false, 3);
        bar.addActor(button("Запуск (F9)", this::start));
        bar.addActor(button("Стоп", this::stop));
        pausedOnly.add(button("Продолжить (F8)", () -> resume(DebugSession.Mode.RUN)));
        pausedOnly.add(button("Шаг (F10)", () -> resume(DebugSession.Mode.OVER)));
        pausedOnly.add(button("Внутрь (F11)", () -> resume(DebugSession.Mode.INTO)));
        pausedOnly.add(button("Наружу (Shift+F11)", () -> resume(DebugSession.Mode.OUT)));
        for (VisTextButton b : pausedOnly) {
            bar.addActor(b);
        }
        bar.addActor(button("Справка DGScript", () -> HelpDialog.showScript(getStage())));

        VisTable code = new VisTable();
        code.add(bar).growX().minWidth(0).pad(2).row();
        code.add(editor).grow().row();
        check.setWrap(true);
        code.add(check).growX().minWidth(0).pad(2).row();

        VisTable debug = new VisTable();
        debug.top().left();
        debug.defaults().left().pad(2);
        state.setWrap(true);
        debug.add(state).growX().minWidth(0).row();
        debug.add(new VisLabel("Стек вызовов")).row();
        VisScrollPane stackPane = Form.wheel(new VisScrollPane(stack));
        stackPane.setFadeScrollBars(false);
        debug.add(stackPane).growX().height(70).row();
        debug.add(new VisLabel("Переменные")).row();
        variables.top().left();
        VisScrollPane varPane = Form.wheel(new VisScrollPane(variables));
        varPane.setFadeScrollBars(false);
        varPane.setScrollingDisabled(true, false);
        debug.add(varPane).grow().row();

        VisTable sandboxBox = new VisTable();
        sandboxBox.defaults().left().pad(2);
        VisTable sandboxBar = new VisTable();
        sandboxBar.add(new VisLabel("Песочница")).padRight(10);
        sandboxBar.add(time);
        sandboxBox.add(sandboxBar).row();
        sandboxBox.add(sandbox).grow();

        VisTable consoleBox = new VisTable();
        console.top().left();
        consolePane = Form.wheel(new VisScrollPane(console));
        consolePane.setFadeScrollBars(false);
        consolePane.setScrollingDisabled(true, false);
        consoleBox.add(consolePane).grow().row();
        input.setMessageText("код для песочницы: entity(\"but1\").enabled — Enter");
        consoleBox.add(input).growX().pad(2);

        VisSplitPane right = new VisSplitPane(debug, sandboxBox, true, "wide-vertical");
        right.setSplitAmount(0.55f);
        VisSplitPane center = new VisSplitPane(code, right, false, "wide-horizontal");
        center.setSplitAmount(0.66f);
        VisSplitPane top = new VisSplitPane(files, center, false, "wide-horizontal");
        top.setSplitAmount(0.18f);
        VisSplitPane all = new VisSplitPane(top, consoleBox, true, "wide-vertical");
        all.setSplitAmount(0.74f);
        add(all).grow();

        float w = getStageWidth(app) * 0.92f;
        float h = getStageHeight(app) * 0.9f;
        setSize(w, h);
        centerWindow();

        fileList.addListener(
                EditorApp.changed(
                        () -> {
                            int i = fileList.getSelectedIndex();
                            if (!loading && i >= 0 && i < entries.size()) {
                                open(entries.get(i));
                            }
                        }));
        editor.area()
                .addListener(
                        EditorApp.changed(
                                () -> {
                                    if (!loading) {
                                        apply();
                                    }
                                }));
        stack.addListener(EditorApp.changed(this::showFrame));
        input.addListener(new ConsoleKeys());
        addListener(new DebugKeys());
        refreshEntries();
        updateState();
    }

    private static float getStageWidth(EditorApp app) {
        return app.stage().getWidth();
    }

    private static float getStageHeight(EditorApp app) {
        return app.stage().getHeight();
    }

    // ------------------------------------------------------------------ файлы

    /** Скрипт уровня и обработчики всех объектов, у префаба которых есть события. */
    private void refreshEntries() {
        entries.clear();
        Object script = doc.root().get("script");
        if (script instanceof String name && !name.isBlank()) {
            String path = PackProject.scriptPath(doc.id(), name);
            entries.add(new Entry("Скрипт уровня: " + name, path, path, LEVEL_SCRIPT, null, null));
        } else {
            String name = doc.id().substring(doc.id().lastIndexOf('/') + 1) + ".dgs";
            String path = PackProject.scriptPath(doc.id(), name);
            entries.add(
                    new Entry(
                            "Скрипт уровня (создать " + name + ")",
                            path,
                            path,
                            LEVEL_SCRIPT,
                            null,
                            null));
        }
        // Общие файлы пака: все существующие и те, что уровень подключает через use.
        Set<String> used = usedShared(entries.get(0).path());
        shownUses = used;
        Set<String> shared = new TreeSet<>(project.sharedScripts());
        shared.addAll(used);
        shared.remove(entries.get(0).path());
        for (String path : shared) {
            boolean exists = project.script(path) != null;
            String note = !exists ? "  (создать)" : used.contains(path) ? "  — подключён" : "";
            entries.add(new Entry("Общий: " + path + note, path, path, SHARED, null, null));
        }
        List<Map<String, Object>> list = doc.entities();
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> e = list.get(i);
            Prefab p = app.data().prefabs().get(String.valueOf(e.get("prefab")));
            if (p == null) {
                continue;
            }
            for (String event : p.events()) {
                String key = Prefab.handlerKey(event);
                Object id = e.get("id");
                int[] at = LevelDoc.at(e);
                String who =
                        id == null
                                ? p.id() + " [" + at[0] + ", " + at[1] + "]"
                                : String.valueOf(id);
                String file =
                        ScriptHost.handlerFile(
                                doc.id(), id == null ? null : String.valueOf(id), event);
                entries.add(
                        new Entry(
                                who + " · " + key + (e.containsKey(key) ? "" : "  (пусто)"),
                                file,
                                null,
                                i,
                                key,
                                e));
            }
        }
        Array<String> items = new Array<>();
        for (Entry e : entries) {
            items.add(e.label());
        }
        loading = true;
        fileList.setItems(items);
        loading = false;
    }

    /** Открыть скрипт: уровня ({@code entity < 0}) или обработчик объекта. */
    void select(int entity, String key) {
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (e.entity() == entity && (key == null || key.equals(e.key()))) {
                fileList.setSelectedIndex(i);
                open(e);
                return;
            }
        }
        if (!entries.isEmpty()) {
            fileList.setSelectedIndex(0);
            open(entries.get(0));
        }
    }

    private Entry entryForFile(String file) {
        for (Entry e : entries) {
            if (e.file().equals(file)) {
                return e;
            }
        }
        return null;
    }

    private void open(Entry entry) {
        current = entry;
        checkpointed = false;
        loading = true;
        editor.setText(read(entry));
        loading = false;
        Set<Integer> lines = breakpoints.computeIfAbsent(entry.file(), f -> new TreeSet<>());
        editor.setBreakpoints(
                lines,
                line -> {
                    if (!lines.remove(line)) {
                        lines.add(line);
                    }
                });
        editor.setReadOnly(false);
        validate(editor.getText());
        showPauseLine();
        getStage().setKeyboardFocus(editor.area());
    }

    private String read(Entry entry) {
        if (entry.isFile()) {
            String text = project.script(entry.path());
            return text != null
                    ? text
                    : entry.isShared() ? sharedTemplate(entry.path()) : template();
        }
        Object value = entry.ref().get(entry.key());
        if (value instanceof List<?> lines) {
            StringBuilder sb = new StringBuilder();
            for (Object line : lines) {
                sb.append(sb.length() == 0 ? "" : "\n").append(line);
            }
            return sb.toString();
        }
        return value == null ? "" : String.valueOf(value);
    }

    /** Заготовка нового скрипта уровня: необязательные хуки с пояснениями. */
    private String template() {
        return "// Скрипт уровня "
                + doc.id()
                + "\n"
                + "// Хуки необязательны: раскомментируйте нужные, остальные игра пропустит.\n\n"
                + "// Один раз, когда уровень загружен.\n"
                + "// def init() {\n"
                + "// }\n\n"
                + "// Каждый тик игры (60 раз в секунду), dt — секунды с прошлого тика.\n"
                + "// def loop(dt) {\n"
                + "// }\n\n"
                + "// Герой шагнул в клетку x, y.\n"
                + "// def step(x, y) {\n"
                + "// }\n";
    }

    /** Заготовка общего скрипта: как его подключить. */
    private static String sharedTemplate(String path) {
        String name = path.substring(0, path.length() - ".dgs".length());
        return "// Общий скрипт пака "
                + path
                + "\n"
                + "// Подключается в скрипте уровня (или в обработчике) строкой\n"
                + "//     use \""
                + name
                + "\"\n"
                + "// после этого его функции и переменные видны всем скриптам уровня.\n\n";
    }

    /**
     * Общие файлы (пути .dgs), которые подключает уровень: скрипт уровня и обработчики — и то, что
     * подключают сами эти файлы.
     */
    private Set<String> usedShared(String levelScript) {
        Set<String> used = new TreeSet<>();
        String script = project.script(levelScript);
        List<String> queue = new ArrayList<>();
        queue.add(script == null ? "" : script);
        for (Map<String, Object> e : doc.entities()) {
            for (Object value : e.values()) {
                if (value instanceof String text) {
                    queue.add(text);
                } else if (value instanceof List<?> lines) {
                    StringBuilder sb = new StringBuilder();
                    for (Object line : lines) {
                        sb.append(line).append('\n');
                    }
                    queue.add(sb.toString());
                }
            }
        }
        while (!queue.isEmpty()) {
            for (String module : ScriptEngine.uses(queue.remove(queue.size() - 1), "")) {
                String path = module + ".dgs";
                if (!ScriptEngine.isBuiltinModule(module) && !module.isBlank() && used.add(path)) {
                    String text = project.script(path);
                    if (text != null) {
                        queue.add(text);
                    }
                }
            }
        }
        return used;
    }

    /** Создать общий скрипт пака и открыть его. */
    private void newShared() {
        com.kotcrab.vis.ui.util.dialog.Dialogs.showInputDialog(
                getStage(),
                "Новый общий скрипт",
                "имя (например utils или lib/math2): ",
                input ->
                        input.matches("[A-Za-z0-9_]+(/[A-Za-z0-9_]+)*")
                                && !input.equals("math")
                                && project.script(input + ".dgs") == null,
                new com.kotcrab.vis.ui.util.dialog.InputDialogAdapter() {
                    @Override
                    public void finished(String name) {
                        String path = name + ".dgs";
                        project.script(path, sharedTemplate(path));
                        app.levelEdited();
                        refreshEntries();
                        for (Entry e : entries) {
                            if (e.isShared() && e.path().equals(path)) {
                                fileList.setSelectedIndex(entries.indexOf(e));
                                open(e);
                            }
                        }
                        // Enter, закрывший диалог, ещё не дошёл до поля кода: фокус — кадром позже.
                        getStage().setKeyboardFocus(null);
                        com.badlogic.gdx.Gdx.app.postRunnable(
                                () -> {
                                    getStage().setKeyboardFocus(editor.area());
                                    editor.area().setCursorPosition(editor.getText().length());
                                });
                        line(
                                "Создан "
                                        + path
                                        + ". Подключите его в скрипте уровня: use \""
                                        + name
                                        + "\"",
                                DebugSession.Kind.SYSTEM);
                    }
                });
    }

    /** Текст из поля — в проект: файл скрипта или поле обработчика (по строке на строку). */
    private void apply() {
        if (current == null) {
            return;
        }
        String text = editor.getText();
        editedSinceStart = session != null;
        if (current.isShared()) {
            boolean created = project.script(current.path()) == null;
            project.script(current.path(), text);
            if (created) {
                levelChanged(); // «(создать)» в списке больше не нужно
            }
        } else if (current.isLevelScript()) {
            project.script(current.path(), text);
            if (!(doc.root().get("script") instanceof String)) {
                doc.checkpoint();
                String path = current.path();
                doc.root().put("script", path.substring(path.lastIndexOf('/') + 1));
                doc.changed();
                int at = fileList.getSelectedIndex();
                refreshEntries();
                loading = true;
                fileList.setSelectedIndex(at);
                loading = false;
                current = entries.get(0);
            }
        } else {
            if (!checkpointed) {
                doc.checkpoint();
                checkpointed = true;
            }
            Map<String, Object> e = current.ref();
            if (!isInLevel(e)) {
                return; // объект удалён — запись обновится в act()
            }
            String[] lines = text.split("\n", -1);
            if (text.isBlank()) {
                e.remove(current.key());
            } else if (lines.length == 1) {
                e.put(current.key(), text.strip());
            } else {
                List<Object> list = new ArrayList<>();
                for (String line : lines) {
                    list.add(line.stripTrailing());
                }
                while (!list.isEmpty() && ((String) list.get(list.size() - 1)).isEmpty()) {
                    list.remove(list.size() - 1);
                }
                e.put(current.key(), list);
            }
            doc.changed();
        }
        if (current.isLevelScript() && !usedShared(current.path()).equals(shownUses)) {
            levelChanged(); // в скрипте уровня появился или пропал use — список файлов заново
        }
        app.levelEdited();
        validate(text);
    }

    /** Проверка синтаксиса — как в игре (обработчик — внутри функции с self). */
    private void validate(String text) {
        try {
            if (current.isFile()) {
                ScriptEngine.create().compile(text, current.file());
            } else {
                ScriptEngine.create().compile("__h = def(self) { " + text + "\n}", current.file());
            }
            editor.setErrorLine(-1);
            check.setColor(new Color(0.55f, 0.85f, 0.45f, 1));
            check.setText(
                    editedSinceStart
                            ? "Синтаксис в порядке. Песочница запущена со старым текстом —"
                                    + " перезапустите (F9), чтобы проверить правку."
                            : "Синтаксис в порядке.");
        } catch (ScriptException e) {
            int line = e.position() == null ? -1 : e.position().line();
            String reason = e.reason();
            int count = text.split("\n", -1).length;
            if (!current.isFile() && line > count) {
                // Ошибка в служебной закрывающей скобке обработчика: код оборвался.
                line = count;
                reason = reason.replace("найдено '}'", "а код обработчика закончился");
            }
            editor.setErrorLine(line);
            check.setColor(new Color(1, 0.45f, 0.45f, 1));
            check.setText((line > 0 ? "Строка " + line + ": " : "") + reason);
        }
    }

    // ------------------------------------------------------------------ отладка

    private void start() {
        stop();
        Problems problems = new Problems(doc.id());
        LevelDefinition def = app.data().parser().parse(doc.toJson(), doc.id(), problems);
        clearConsole();
        if (def == null) {
            for (Problem p : problems.list()) {
                if (p.isError()) {
                    line(p.path() + ": " + p.message(), DebugSession.Kind.ERROR);
                }
            }
            line("Уровень с ошибками — песочница не запускается.", DebugSession.Kind.SYSTEM);
            return;
        }
        String script = def.scriptPath() == null ? null : project.script(def.scriptPath());
        editedSinceStart = false;
        session = new DebugSession(this::hasBreakpoint);
        session.start(def, script, project::script, app.data().items());
        afterRun();
        if (current != null) {
            validate(editor.getText());
        }
    }

    private boolean hasBreakpoint(String file, int line) {
        Set<Integer> lines = breakpoints.get(file);
        return lines != null && lines.contains(line);
    }

    private void stop() {
        if (session != null) {
            session.close();
            session = null;
            line("Остановлено.", DebugSession.Kind.SYSTEM);
        }
        time.setChecked(false);
        shownPause = null;
        editor.setExecLine(-1);
        stack.setItems(new Array<>());
        variables.clearChildren();
        updateState();
    }

    private void resume(DebugSession.Mode mode) {
        if (session != null && session.isPaused()) {
            session.resume(mode);
            afterRun();
        }
    }

    /** После задачи песочницы: консоль, остановка, кнопки. */
    private void afterRun() {
        drainConsole();
        DebugSession.Pause pause = session == null ? null : session.pause();
        if (pause != shownPause) {
            shownPause = pause;
            if (pause != null) {
                showPause(pause);
            } else {
                editor.setExecLine(-1);
                stack.setItems(new Array<>());
                variables.clearChildren();
            }
        }
        updateState();
    }

    private void showPause(DebugSession.Pause pause) {
        Entry entry = entryForFile(pause.position().file());
        if (entry != null && entry != current) {
            fileList.setSelectedIndex(entries.indexOf(entry));
            open(entry);
        }
        showPauseLine();
        Array<String> frames = new Array<>();
        for (DebugSession.Frame f : pause.frames()) {
            frames.add(frameName(f) + "  — " + f.position().file() + ":" + f.position().line());
        }
        loading = true;
        stack.setItems(frames);
        stack.setSelectedIndex(0);
        loading = false;
        showVariables(pause, 0);
    }

    private static String frameName(DebugSession.Frame f) {
        return f.function().equals("<lambda>") ? "обработчик / функция без имени" : f.function();
    }

    /** Подсветить строку остановки, если открыт её файл. */
    private void showPauseLine() {
        DebugSession.Pause pause = session == null ? null : session.pause();
        if (pause != null && current != null && pause.position().file().equals(current.file())) {
            editor.setExecLine(pause.position().line());
        } else {
            editor.setExecLine(-1);
        }
    }

    private void showFrame() {
        DebugSession.Pause pause = session == null ? null : session.pause();
        int i = stack.getSelectedIndex();
        if (loading || pause == null || i < 0 || i >= pause.frames().size()) {
            return;
        }
        DebugSession.Frame f = pause.frames().get(i);
        Entry entry = entryForFile(f.position().file());
        if (entry != null && entry != current) {
            fileList.setSelectedIndex(entries.indexOf(entry));
            open(entry);
        }
        editor.goTo(f.position().line());
        showVariables(pause, i);
    }

    private void showVariables(DebugSession.Pause pause, int frame) {
        variables.clearChildren();
        variables.defaults().left().top().pad(1, 2, 1, 2);
        DebugSession.Frame f = pause.frames().get(frame);
        if (frame < pause.frames().size() - 1 || !f.locals().isEmpty()) {
            section("Локальные: " + frameName(f));
            values(f.locals());
        }
        section("Глобальные");
        values(pause.globals());
    }

    private void section(String title) {
        VisLabel l = new VisLabel(title, "small");
        l.setColor(1, 0.85f, 0.4f, 1);
        variables.add(l).colspan(2).padTop(4).row();
    }

    private void values(Map<String, String> values) {
        if (values.isEmpty()) {
            variables.add(new VisLabel("(нет)", "small")).colspan(2).row();
        }
        for (Map.Entry<String, String> v : values.entrySet()) {
            variables.add(new VisLabel(v.getKey(), "small"));
            VisLabel value = new VisLabel(v.getValue(), "small");
            value.setWrap(true);
            variables.add(value).growX().minWidth(0).row();
        }
    }

    private void updateState() {
        boolean paused = session != null && session.isPaused();
        for (VisTextButton b : pausedOnly) {
            b.setDisabled(!paused);
        }
        String text;
        if (session == null) {
            text = "Не запущено. F9 — запустить уровень в песочнице.";
        } else if (paused) {
            DebugSession.Pause p = session.pause();
            text =
                    "Остановлено: "
                            + p.position().file()
                            + ", строка "
                            + p.position().line()
                            + ". F8 — дальше, F10 — шаг.";
        } else if (session.isFinished()) {
            text = "Уровень закончился. F9 — запустить заново.";
        } else {
            text = "Запущено. Щёлкните по объекту с меткой в песочнице или включите время.";
        }
        state.setText(text);
    }

    private boolean isInLevel(Map<String, Object> entity) {
        for (Map<String, Object> e : doc.entities()) {
            if (e == entity) {
                return true;
            }
        }
        return false;
    }

    private int seenRevision = -1;

    /**
     * Уровень изменился (в том числе в главном окне): список скриптов заново. Если открытый
     * обработчик принадлежал удалённому или заменённому (отменой) объекту — открыть его заново по
     * месту, иначе скрипт уровня.
     */
    private void levelChanged() {
        Entry old = current;
        int at = fileList.getSelectedIndex();
        refreshEntries();
        Entry same = null;
        if (old != null && !old.isFile()) {
            for (Entry e : entries) {
                if (e.ref() == old.ref() && old.key().equals(e.key())) {
                    same = e;
                }
            }
            if (same == null) {
                Entry byPlace = null;
                for (Entry e : entries) {
                    if (e.entity() == old.entity() && old.key().equals(e.key())) {
                        byPlace = e;
                    }
                }
                Entry next = byPlace != null ? byPlace : entries.get(0);
                if (byPlace == null) {
                    line(
                            "Объект «" + old.label() + "» удалён — открыт скрипт уровня.",
                            DebugSession.Kind.SYSTEM);
                }
                fileList.setSelectedIndex(entries.indexOf(next));
                open(next);
                return;
            }
        } else if (old != null) {
            same = entries.get(0);
            for (Entry e : entries) {
                if (e.isShared() && e.path().equals(old.path())) {
                    same = e;
                }
            }
        }
        if (same != null) {
            current = same;
            loading = true;
            fileList.setSelectedIndex(entries.indexOf(same));
            loading = false;
        } else if (at >= 0 && at < entries.size()) {
            loading = true;
            fileList.setSelectedIndex(at);
            loading = false;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (doc.revision() != seenRevision) {
            seenRevision = doc.revision();
            levelChanged();
        }
        if (session == null) {
            return;
        }
        if (time.isChecked() && session.isIdle() && !session.isFinished()) {
            ticks += delta;
            int n = Math.min(6, (int) (ticks / LevelRuntime.TICK));
            if (n > 0) {
                ticks -= n * LevelRuntime.TICK;
                session.tick(n);
            }
        } else {
            ticks = 0;
        }
        afterRun();
    }

    // ------------------------------------------------------------------ консоль

    private void drainConsole() {
        if (session == null) {
            return;
        }
        List<DebugSession.Line> lines = session.drain();
        for (DebugSession.Line l : lines) {
            line(l.text(), l.kind());
            if (l.kind() == DebugSession.Kind.ERROR) {
                markError(l.text());
            }
        }
    }

    /** Ошибка выполнения в открытом файле — отметить строку. */
    private void markError(String text) {
        Matcher m = LOCATION.matcher(text);
        if (m.find() && current != null && m.group(1).equals(current.file())) {
            editor.setErrorLine(Integer.parseInt(m.group(2)));
        }
    }

    private void line(String text, DebugSession.Kind kind) {
        VisLabel label = new VisLabel(text, "small");
        label.setWrap(true);
        label.setAlignment(Align.topLeft);
        label.setColor(
                switch (kind) {
                    case ERROR -> new Color(1, 0.45f, 0.45f, 1);
                    case SYSTEM -> new Color(0.6f, 0.75f, 1f, 1);
                    case INPUT -> Color.LIGHT_GRAY;
                    case INFO -> Color.WHITE;
                });
        Matcher m = LOCATION.matcher(text);
        if (m.find()) {
            String file = m.group(1);
            int ln = Integer.parseInt(m.group(2));
            label.addListener(
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            Entry entry = entryForFile(file);
                            if (entry != null) {
                                fileList.setSelectedIndex(entries.indexOf(entry));
                                open(entry);
                                editor.goTo(ln);
                            }
                        }
                    });
        }
        console.add(label).growX().minWidth(0).left().row();
        while (console.getChildren().size > CONSOLE_LIMIT) {
            console.removeActorAt(0, false);
        }
        console.layout();
        consolePane.layout();
        consolePane.setScrollPercentY(1);
    }

    private void clearConsole() {
        console.clearChildren();
    }

    private void evaluate() {
        String code = input.getText().strip();
        if (code.isEmpty()) {
            return;
        }
        history.add(code);
        historyIndex = history.size();
        input.setText("");
        if (session == null) {
            start();
        }
        if (session != null && session.isIdle()) {
            session.evaluate(code);
            afterRun();
        } else if (session != null && session.isPaused()) {
            line(
                    "Скрипт стоит на остановке — продолжите (F8), потом выполняйте код.",
                    DebugSession.Kind.SYSTEM);
        }
    }

    private final class ConsoleKeys extends InputListener {
        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            switch (keycode) {
                case Keys.ENTER, Keys.NUMPAD_ENTER -> evaluate();
                case Keys.UP -> {
                    if (historyIndex > 0) {
                        input.setText(history.get(--historyIndex));
                        input.setCursorPosition(input.getText().length());
                    }
                }
                case Keys.DOWN -> {
                    if (historyIndex < history.size() - 1) {
                        input.setText(history.get(++historyIndex));
                    } else {
                        historyIndex = history.size();
                        input.setText("");
                    }
                    input.setCursorPosition(input.getText().length());
                }
                default -> {
                    return false;
                }
            }
            return true;
        }
    }

    private final class DebugKeys extends InputListener {
        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            switch (keycode) {
                case Keys.F9 -> start();
                case Keys.F8 -> resume(DebugSession.Mode.RUN);
                case Keys.F10 -> resume(DebugSession.Mode.OVER);
                case Keys.F11 ->
                        resume(UIUtils.shift() ? DebugSession.Mode.OUT : DebugSession.Mode.INTO);
                default -> {
                    return false;
                }
            }
            event.stop();
            return true;
        }
    }

    // ------------------------------------------------------------------ окно

    @Override
    protected void close() {
        stop();
        app.selectionChanged();
        app.scriptWindowClosed(this);
        super.close();
    }

    /** Закрыть без анимации и обновлений (смена масштаба, выход). */
    void shutdown() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    /** id уровня, с которым открыто окно (переименование уровня окно закрывает). */
    String levelId() {
        return levelId;
    }

    /** Окно показывает этот уровень. */
    LevelDoc level() {
        return doc;
    }

    private static VisTextButton button(String text, Runnable action) {
        VisTextButton b = new VisTextButton(text);
        b.addListener(EditorApp.changed(action));
        return b;
    }
}
