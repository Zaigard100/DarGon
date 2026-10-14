package com.zaig100.dargon.script;

/** Типы токенов DGScript. */
enum TokenType {
    NUMBER,
    STRING,
    IDENT,

    // ключевые слова
    IF,
    ELSE,
    WHILE,
    DO,
    FOR,
    IN,
    BREAK,
    CONTINUE,
    RETURN,
    DEF,
    USE,
    TRUE,
    FALSE,
    NULL,

    // скобки и разделители
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    LBRACE,
    RBRACE,
    COMMA,
    DOT,
    COLON,
    SEMICOLON,

    // операторы
    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,
    BANG,
    ASSIGN,
    PLUS_ASSIGN,
    MINUS_ASSIGN,
    STAR_ASSIGN,
    SLASH_ASSIGN,
    EQ,
    NOT_EQ,
    LT,
    LT_EQ,
    GT,
    GT_EQ,
    AND,
    OR,

    EOF
}
