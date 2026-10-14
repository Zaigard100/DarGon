package com.zaig100.dg.utils.dgscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Lexer {

    private static final String OPERATOR_CHARS = "+-*/()[]{}=<>!&|,";
    private static final Map<String, TokenType> OPERATORS;

    static {
        OPERATORS = new HashMap<>();
        OPERATORS.put("+", TokenType.PLUS);
        OPERATORS.put("-", TokenType.MINUS);
        OPERATORS.put("*", TokenType.STAR);
        OPERATORS.put("/", TokenType.SLASH);
        OPERATORS.put("(", TokenType.LPAR);
        OPERATORS.put(")", TokenType.RPAR);
        OPERATORS.put("[", TokenType.LBRAKET);
        OPERATORS.put("]", TokenType.RBRAKET);
        OPERATORS.put("{", TokenType.LBRACE);
        OPERATORS.put("}", TokenType.RBRACE);
        OPERATORS.put("=", TokenType.EQ);
        OPERATORS.put("<", TokenType.LT);
        OPERATORS.put(">", TokenType.GT);
        OPERATORS.put(",", TokenType.COMMA);

        OPERATORS.put("!", TokenType.EXCL);
        OPERATORS.put("&", TokenType.BAR);
        OPERATORS.put("|", TokenType.AMP);

        OPERATORS.put("==", TokenType.EQEQ);
        OPERATORS.put("!=", TokenType.EXCLEQ);
        OPERATORS.put("<=", TokenType.LTEQ);
        OPERATORS.put(">=", TokenType.GTEQ);

        OPERATORS.put("&&", TokenType.AMPAMP);
        OPERATORS.put("||", TokenType.BARBAR);
    }

    private final String input;
    private final int lenght;

    private final List<Token> tokens;

    private int pos;

    public Lexer(String input) {
        this.input = input;
        lenght = input.length();

        tokens = new ArrayList<>();
    }

    public List<Token> tokenize() {
        while (pos < lenght) {
            final char curent = peek(0);
            if (Character.isDigit(curent)) tokinizeNum();
            else if (Character.isLetter(curent)) tokinizeWord();
            else if (curent == '\"') {
                tokinizeText();
            } else if (OPERATOR_CHARS.indexOf(curent) != -1) {
                tokinizeOpe();
            } else {
                next();
            }
        }
        return tokens;
    }

    private void tokinizeOpe() {
        char current = peek(0);
        if (current == '/') {
            if (peek(1) == '/') {
                next();
                next();
                tokinizeComments();
                return;
            } else if (peek(1) == '*') {
                next();
                next();
                tokinizeMultilineComments();
                return;
            }
        }
        final StringBuilder bufffer = new StringBuilder();
        while (true) {
            final String text = bufffer.toString();
            if (!OPERATORS.containsKey(text + current) && !text.isEmpty()) {
                addtoken(OPERATORS.get(text));
                return;
            }
            bufffer.append(current);
            current = next();
        }
    }

    private void tokinizeComments() {
        char current = peek(0);
        while ("\r\n\0".indexOf(current) == -1) {
            current = next();
        }
    }

    private void tokinizeMultilineComments() {
        char current = peek(0);
        while (true) {
            if (current == '\0') throw new RuntimeException("Missing close tag");
            if (current == '*' && peek(1) == '/') break;
            current = next();
        }
        next();
        next();
    }

    private void tokinizeText() {
        next(); //skip "
        final StringBuilder buffer = new StringBuilder();
        char current = peek(0);
        while (true) {
            if (current == '\\') {
                current = next();
                switch (current) {
                    case '"':
                        current = next();
                        buffer.append('"');
                        continue;
                    case 'n':
                        current = next();
                        buffer.append('\n');
                        continue;
                    case 't':
                        current = next();
                        buffer.append('\t');
                        continue;
                }
                buffer.append('\\');
                continue;
            }
            if (current == '\"') break;
            buffer.append(current);
            current = next();
        }
        next();
        addtoken(TokenType.TEXT, buffer.toString());
    }


    private void tokinizeWord() {
        final StringBuilder buffer = new StringBuilder();
        char current = peek(0);
        while (Character.isLetterOrDigit(current) || (current == '_')) {
            buffer.append(current);
            current = next();
        }
        final String word = buffer.toString();
        switch (word) {
            case "if":
                addtoken(TokenType.IF);
                break;
            case "else":
                addtoken(TokenType.ELSE);
                break;
            case "while":
                addtoken(TokenType.WHILE);
                break;
            case "for":
                addtoken(TokenType.FOR);
                break;
            case "do":
                addtoken(TokenType.DO);
                break;
            case "break":
                addtoken(TokenType.BRAKE);
                break;
            case "continue":
                addtoken(TokenType.CONTINUE);
                break;
            case "def":
                addtoken(TokenType.DEF);
                break;
            case "return":
                addtoken(TokenType.RETURN);
                break;
            case "use":
                addtoken(TokenType.USE);
                break;
            default:
                addtoken(TokenType.WORD, word);
        }
    }

    private void tokinizeNum() {
        final StringBuilder buffer = new StringBuilder();
        char current = peek(0);
        while (true) {
            if (current == '.') {
                if (buffer.indexOf(".") != -1)
                    throw new RuntimeException("Invalid number in " + pos + " : " + current);
            } else if (!Character.isDigit(current)) {
                break;
            }
            buffer.append(current);
            current = next();
        }
        addtoken(TokenType.NUMBER, buffer.toString());
    }

    private char next() {
        pos++;
        return peek(0);

    }

    private char peek(int revPos) {
        final int position = pos + revPos;
        if (position >= lenght) return '\0';
        return input.charAt(position);
    }

    private void addtoken(TokenType type) {
        addtoken(type, "");
    }

    private void addtoken(TokenType type, String text) {
        tokens.add(new Token(type, text));
    }

}
