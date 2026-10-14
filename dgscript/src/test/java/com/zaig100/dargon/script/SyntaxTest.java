package com.zaig100.dargon.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SyntaxTest extends ScriptTestSupport {

    @Test
    void unterminatedStringIsAnErrorNotAnEndlessLoop() {
        assertEquals("test.dgs:1:7: строка не закрыта", error("print(\"abc"));
    }

    @Test
    void unknownEscapeIsReported() {
        assertTrue(error("x = \"a\\q\"").contains("неизвестная escape-последовательность \\q"));
    }

    @Test
    void singleAmpersandSuggestsDoubleAmpersand() {
        assertTrue(error("x = a & b").contains("может быть '&&'?"));
    }

    @Test
    void unclosedCommentAndBlockPointToTheirStart() {
        assertEquals("test.dgs:2:1: комментарий /* не закрыт", error("x = 1\n/* ..."));
        assertTrue(error("if true {\n  x = 1\n").contains("нет '}' для '{' в строке 1:9"));
    }

    @Test
    void missingClosingParenthesisIsAnError() {
        assertTrue(error("x = (1 + 2").contains("ожидалось ')'"));
    }

    @Test
    void chainedEqualityParses() {
        assertEquals("true", eval("x = 1 == 1 == true", "x"));
    }

    @Test
    void nestedUnaryOperatorsParse() {
        assertEquals("5", eval("x = - -5", "x"));
        assertEquals("true", eval("y = !!1", "y"));
    }

    @Test
    void breakOutsideLoopIsRejected() {
        assertTrue(error("break").contains("'break' вне цикла"));
        assertTrue(error("while true { f = def() { break } }").contains("'break' вне цикла"));
    }

    @Test
    void expressionWithoutEffectIsRejected() {
        assertEquals("test.dgs:1:1: ожидалось присваивание или вызов функции", error("1 + 2"));
    }

    @Test
    void assignmentNeedsAssignableTarget() {
        assertTrue(error("f() = 1").contains("слева от '='"));
    }

    @Test
    void duplicateParametersAndKeysAreRejected() {
        assertTrue(error("def f(a, a) return a").contains("параметр 'a' повторяется"));
        assertTrue(error("m = {a: 1, a: 2}").contains("ключ 'a' повторяется"));
    }

    @Test
    void positionsCountLinesAndColumns() {
        String message = error("x = 1\ny = 2\n  z = x / (y - 2)");
        assertEquals("test.dgs:3:9: деление на ноль", message);
    }

    @Test
    void commentsSemicolonsAndNewlinesAreFlexible() {
        assertEquals(
                List.of("3", "4"),
                run("// комментарий\nx = 1; y = 2 /* inline */ ; print(x + y)\nprint(x +\n 3)"));
    }

    @Test
    void legacyTestScriptStillWorks() {
        // testscript.dgs из DarGon3 — без унарных '*' и '/', которых больше нет.
        String legacy =
                """
                use "math"
                i = 8 + 13 - 4 * -6 - 10 * 46 / 16
                j = 8 / 8 + 8 * 8 - 8+(i*0)+(i-i)
                c = i==j
                add = def(a,b) return a+b
                sub = def(a,b) return a-b
                if((45/5==9||60/5*7==8*6)&&!c){
                println(i+j)
                }
                println(add(10,6))
                println(sub(10,6))
                """;
        assertEquals(List.of("73.25", "16", "4"), run(legacy));
    }
}
