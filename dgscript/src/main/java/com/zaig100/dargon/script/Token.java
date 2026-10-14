package com.zaig100.dargon.script;

/** Токен с позицией начала в исходнике. */
record Token(TokenType type, String text, int line, int column) {

    String describe() {
        return switch (type) {
            case EOF -> "конец файла";
            case STRING -> "строка \"" + text + "\"";
            case NUMBER, IDENT -> "'" + text + "'";
            default -> "'" + text + "'";
        };
    }
}
