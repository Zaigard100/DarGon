package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Разбивает исходный текст на токены. Последний токен — всегда {@link TokenType#EOF}. */
final class Lexer {

    private static final Map<String, TokenType> KEYWORDS =
            Map.ofEntries(
                    Map.entry("if", TokenType.IF),
                    Map.entry("else", TokenType.ELSE),
                    Map.entry("while", TokenType.WHILE),
                    Map.entry("do", TokenType.DO),
                    Map.entry("for", TokenType.FOR),
                    Map.entry("in", TokenType.IN),
                    Map.entry("break", TokenType.BREAK),
                    Map.entry("continue", TokenType.CONTINUE),
                    Map.entry("return", TokenType.RETURN),
                    Map.entry("def", TokenType.DEF),
                    Map.entry("use", TokenType.USE),
                    Map.entry("true", TokenType.TRUE),
                    Map.entry("false", TokenType.FALSE),
                    Map.entry("null", TokenType.NULL));

    private final String source;
    private final String file;
    private final List<Token> tokens = new ArrayList<>();

    private int pos;
    private int line = 1;
    private int column = 1;

    private int startLine;
    private int startColumn;

    Lexer(String source, String file) {
        this.source = source;
        this.file = file;
    }

    List<Token> tokenize() {
        while (true) {
            skipWhitespaceAndComments();
            startLine = line;
            startColumn = column;
            if (pos >= source.length()) {
                tokens.add(new Token(TokenType.EOF, "", line, column));
                return tokens;
            }
            char c = peek(0);
            if (isDigit(c)) {
                number();
            } else if (isIdentStart(c)) {
                identifier();
            } else if (c == '"' || c == '\'') {
                string();
            } else {
                operator();
            }
        }
    }

    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = peek(0);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && peek(1) == '/') {
                while (pos < source.length() && peek(0) != '\n') {
                    advance();
                }
            } else if (c == '/' && peek(1) == '*') {
                int openLine = line;
                int openColumn = column;
                advance();
                advance();
                while (!(peek(0) == '*' && peek(1) == '/')) {
                    if (pos >= source.length()) {
                        throw error("комментарий /* не закрыт", openLine, openColumn);
                    }
                    advance();
                }
                advance();
                advance();
            } else {
                return;
            }
        }
    }

    private void number() {
        int start = pos;
        while (isDigit(peek(0))) {
            advance();
        }
        if (peek(0) == '.' && isDigit(peek(1))) {
            advance();
            while (isDigit(peek(0))) {
                advance();
            }
        }
        if (isIdentStart(peek(0))) {
            throw error("неверное число: '" + source.substring(start, pos + 1) + "'");
        }
        add(TokenType.NUMBER, source.substring(start, pos));
    }

    private void identifier() {
        int start = pos;
        while (isIdentPart(peek(0))) {
            advance();
        }
        String word = source.substring(start, pos);
        add(KEYWORDS.getOrDefault(word, TokenType.IDENT), word);
    }

    /** Строка в двойных или одинарных кавычках (одинарные удобны внутри JSON уровней). */
    private void string() {
        char quote = advance(); // открывающая кавычка
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= source.length() || peek(0) == '\n') {
                throw error("строка не закрыта", startLine, startColumn);
            }
            char c = advance();
            if (c == quote) {
                break;
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= source.length()) {
                throw error("строка не закрыта", startLine, startColumn);
            }
            char escaped = advance();
            switch (escaped) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case '"' -> sb.append('"');
                case '\'' -> sb.append('\'');
                case '\\' -> sb.append('\\');
                default -> throw error("неизвестная escape-последовательность \\" + escaped);
            }
        }
        add(TokenType.STRING, sb.toString());
    }

    private void operator() {
        char c = advance();
        char next = peek(0);
        TokenType type =
                switch (c) {
                    case '(' -> TokenType.LPAREN;
                    case ')' -> TokenType.RPAREN;
                    case '[' -> TokenType.LBRACKET;
                    case ']' -> TokenType.RBRACKET;
                    case '{' -> TokenType.LBRACE;
                    case '}' -> TokenType.RBRACE;
                    case ',' -> TokenType.COMMA;
                    case '.' -> TokenType.DOT;
                    case ':' -> TokenType.COLON;
                    case ';' -> TokenType.SEMICOLON;
                    case '%' -> TokenType.PERCENT;
                    case '+' -> twoChar(next, '=', TokenType.PLUS_ASSIGN, TokenType.PLUS);
                    case '-' -> twoChar(next, '=', TokenType.MINUS_ASSIGN, TokenType.MINUS);
                    case '*' -> twoChar(next, '=', TokenType.STAR_ASSIGN, TokenType.STAR);
                    case '/' -> twoChar(next, '=', TokenType.SLASH_ASSIGN, TokenType.SLASH);
                    case '=' -> twoChar(next, '=', TokenType.EQ, TokenType.ASSIGN);
                    case '!' -> twoChar(next, '=', TokenType.NOT_EQ, TokenType.BANG);
                    case '<' -> twoChar(next, '=', TokenType.LT_EQ, TokenType.LT);
                    case '>' -> twoChar(next, '=', TokenType.GT_EQ, TokenType.GT);
                    case '&' -> {
                        if (next != '&') {
                            throw error("неизвестный оператор '&', может быть '&&'?");
                        }
                        yield twoChar(next, '&', TokenType.AND, TokenType.AND);
                    }
                    case '|' -> {
                        if (next != '|') {
                            throw error("неизвестный оператор '|', может быть '||'?");
                        }
                        yield twoChar(next, '|', TokenType.OR, TokenType.OR);
                    }
                    default -> throw error("неожиданный символ '" + c + "'");
                };
        add(type, source.substring(pos - tokenLength(type), pos));
    }

    private TokenType twoChar(char next, char expected, TokenType ifMatch, TokenType otherwise) {
        if (next == expected) {
            advance();
            return ifMatch;
        }
        return otherwise;
    }

    private int tokenLength(TokenType type) {
        return switch (type) {
            case PLUS_ASSIGN,
                    MINUS_ASSIGN,
                    STAR_ASSIGN,
                    SLASH_ASSIGN,
                    EQ,
                    NOT_EQ,
                    LT_EQ,
                    GT_EQ,
                    AND,
                    OR ->
                    2;
            default -> 1;
        };
    }

    private void add(TokenType type, String text) {
        tokens.add(new Token(type, text, startLine, startColumn));
    }

    private char peek(int offset) {
        int i = pos + offset;
        return i < source.length() ? source.charAt(i) : '\0';
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private ScriptException error(String message) {
        return error(message, startLine, startColumn);
    }

    private ScriptException error(String message, int atLine, int atColumn) {
        return new ScriptException(message, new SourcePosition(file, atLine, atColumn));
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentStart(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private static boolean isIdentPart(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }
}
