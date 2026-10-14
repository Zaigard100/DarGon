package com.zaig100.dargon.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.highlight.Highlighter;
import com.kotcrab.vis.ui.widget.HighlightTextArea;
import com.kotcrab.vis.ui.widget.VisTable;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntConsumer;

/**
 * Поле кода DGScript: подсветка синтаксиса, номера строк, точки остановки (щелчок по номеру),
 * текущая строка отладчика и строка с ошибкой.
 */
final class CodeEditor extends VisTable {

    private static final Color KEYWORD = new Color(0.95f, 0.6f, 0.3f, 1);
    private static final Color STRING = new Color(0.55f, 0.85f, 0.45f, 1);
    private static final Color NUMBER = new Color(0.45f, 0.75f, 1f, 1);
    private static final Color COMMENT = new Color(0.5f, 0.5f, 0.5f, 1);
    private static final Color GAME = new Color(0.85f, 0.75f, 1f, 1);
    private static final Color BREAKPOINT = new Color(0.9f, 0.2f, 0.2f, 1);
    private static final Color EXEC = new Color(1, 0.85f, 0.3f, 0.22f);
    private static final Color EXEC_MARK = new Color(1, 0.85f, 0.3f, 1);
    private static final Color ERROR = new Color(1, 0.25f, 0.25f, 0.25f);
    private static final Color GUTTER = new Color(0.6f, 0.6f, 0.6f, 1);

    private final HighlightTextArea area;
    private final ScrollPane scroll;
    private final Gutter gutter = new Gutter();
    private Set<Integer> breakpoints = new TreeSet<>();
    private IntConsumer toggle = line -> {};
    private int execLine = -1;
    private int errorLine = -1;

    CodeEditor() {
        area = new HighlightTextArea("", "mono");
        Highlighter h = new Highlighter();
        h.regex(NUMBER, "\\b\\d+(\\.\\d+)?\\b");
        h.regex(
                KEYWORD,
                "\\b(if|else|while|do|for|in|break|continue|return|def|use|true|false|null|self)\\b");
        h.regex(
                GAME,
                "\\b(print|entity|setTile|achieve|after|every|cancel|len|push|pop|remove|contains"
                        + "|keys|join|range|array|rand|type|str|num|int"
                        + "|init|loop|step)\\b(?=\\s*\\()");
        h.regex(STRING, "\"([^\"\\\\\\n]|\\\\.)*\"|'([^'\\\\\\n]|\\\\.)*'");
        h.regex(COMMENT, "//[^\\n]*|/\\*(.|\\n)*?\\*/");
        area.setHighlighter(h);
        // Поле с подсветкой рисует первую строку вплотную к верху — немного воздуха сверху.
        com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle style =
                new com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle(area.getStyle());
        if (style.background != null) {
            style.background = VisUI.getSkin().newDrawable(style.background);
            style.background.setTopHeight(style.background.getTopHeight() + 4);
        }
        area.setStyle(style);
        scroll = area.createCompatibleScrollPane();
        scroll.setFadeScrollBars(false);
        Form.wheel(scroll);
        add(gutter).width(56).growY();
        add(scroll).grow();
        gutter.addListener(
                new InputListener() {
                    @Override
                    public boolean touchDown(
                            InputEvent event, float x, float y, int pointer, int button) {
                        int line = lineAt(y + gutter.getY());
                        if (line >= 1 && line <= lineCount()) {
                            toggle.accept(line);
                        }
                        return true;
                    }
                });
    }

    HighlightTextArea area() {
        return area;
    }

    String getText() {
        return area.getText();
    }

    void setText(String text) {
        area.setText(text);
        area.setCursorPosition(0);
        scroll.setScrollY(0);
    }

    void setReadOnly(boolean readOnly) {
        area.setDisabled(readOnly);
    }

    /** Точки остановки этого файла (живой набор) и что делать при щелчке по номеру строки. */
    void setBreakpoints(Set<Integer> lines, IntConsumer toggle) {
        this.breakpoints = lines;
        this.toggle = toggle;
    }

    /** Строка, на которой стоит отладчик (с единицы), или -1. */
    void setExecLine(int line) {
        execLine = line;
        if (line > 0) {
            showLine(line);
        }
    }

    /** Строка с ошибкой (с единицы) или -1. */
    void setErrorLine(int line) {
        errorLine = line;
    }

    /** Прокрутить к строке и поставить на неё курсор. */
    void goTo(int line) {
        String text = area.getText();
        int pos = 0;
        for (int i = 1; i < line && pos >= 0; i++) {
            pos = text.indexOf('\n', pos);
            pos = pos < 0 ? -1 : pos + 1;
        }
        if (pos >= 0) {
            area.setCursorPosition(pos);
        }
        showLine(line);
    }

    private void showLine(int line) {
        layout();
        scroll.layout();
        float lh = lineHeight();
        float top = textTop() - (line - 1) * lh;
        scroll.scrollTo(0, top - lh * 3, 1, lh * 5);
    }

    private int lineCount() {
        return area.getText().split("\n", -1).length;
    }

    private float lineHeight() {
        return area.getStyle().font.getLineHeight();
    }

    /** Верх первой строки в координатах поля. */
    private float textTop() {
        float top = area.getHeight();
        if (area.getStyle().background != null) {
            top -= area.getStyle().background.getTopHeight();
        }
        return top;
    }

    /**
     * Верх первой строки в координатах редактора. Считается в {@link #act}: во время рисования
     * scene2d временно сдвигает рисуемых детей, и пересчёт координат между ними врёт.
     */
    private float codeTop;

    @Override
    public void act(float delta) {
        super.act(delta);
        codeTop = area.localToActorCoordinates(this, new Vector2(0, textTop())).y;
    }

    /** Строка (с единицы) под точкой {@code y} редактора. */
    private int lineAt(float y) {
        return (int) Math.floor((codeTop - y) / lineHeight()) + 1;
    }

    /** Низ строки в координатах редактора. */
    private float lineBottom(int line) {
        return codeTop - line * lineHeight();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        // Номера строк, точки остановки и подсветка строк: всё в координатах редактора.
        float x = getX();
        float y = getY();
        float lh = lineHeight();
        if (clipBegin(
                x + gutter.getX(), y + gutter.getY(), gutter.getWidth(), gutter.getHeight())) {
            BitmapFont font = area.getStyle().font;
            float gx = x + gutter.getX();
            float gw = gutter.getWidth();
            int count = lineCount();
            for (int line = 1; line <= count; line++) {
                float bottom = lineBottom(line);
                if (bottom > gutter.getY() + gutter.getHeight()) {
                    continue;
                }
                if (bottom + lh < gutter.getY()) {
                    break;
                }
                float by = y + bottom;
                if (breakpoints.contains(line)) {
                    float s = lh * 0.6f;
                    batch.setColor(BREAKPOINT);
                    batch.draw(white(), gx + 4, by + (lh - s) / 2, s, s);
                }
                if (line == execLine) {
                    batch.setColor(EXEC_MARK);
                    batch.draw(white(), gx + gw - 6, by + 2, 4, lh - 4);
                }
                batch.setColor(Color.WHITE);
                font.setColor(line == errorLine ? BREAKPOINT : GUTTER);
                String n = String.valueOf(line);
                font.draw(batch, n, gx + gw - 10 - n.length() * lh * 0.5f, by + lh - 1);
            }
            font.setColor(Color.WHITE);
            batch.flush();
            clipEnd();
        }
        if ((execLine > 0 || errorLine > 0)
                && clipBegin(
                        x + scroll.getX(),
                        y + scroll.getY(),
                        scroll.getWidth(),
                        scroll.getHeight())) {
            if (errorLine > 0) {
                bar(batch, errorLine, ERROR);
            }
            if (execLine > 0) {
                bar(batch, execLine, EXEC);
            }
            batch.flush();
            clipEnd();
        }
    }

    private void bar(Batch batch, int line, Color color) {
        batch.setColor(color);
        batch.draw(
                white(),
                getX() + scroll.getX(),
                getY() + lineBottom(line),
                scroll.getWidth(),
                lineHeight());
        batch.setColor(Color.WHITE);
    }

    private static TextureRegion white;

    static TextureRegion white() {
        if (white == null) {
            white = VisUI.getSkin().getRegion("white");
        }
        return white;
    }

    /** Полоса под номера строк: фон; номера рисует редактор. */
    private static final class Gutter extends Widget {
        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.setColor(0.1f, 0.1f, 0.11f, 1);
            batch.draw(white(), getX(), getY(), getWidth(), getHeight());
            batch.setColor(Color.WHITE);
        }
    }

    /** Строки, которые сейчас отмечены как точки остановки (копия). */
    Set<Integer> breakpoints() {
        return new TreeSet<>(breakpoints);
    }
}
