package com.zaig100.dargon.script;

import java.util.ArrayList;
import java.util.List;

/**
 * Парсер DGScript (рекурсивный спуск).
 *
 * <pre>
 * program     := statement* EOF
 * statement   := "use" STRING
 *              | "if" expr body ("else" body)?
 *              | "while" expr body
 *              | "do" body "while" expr
 *              | "for" IDENT "in" expr body
 *              | "for" "("? simple ("," | ";") expr ("," | ";") simple ")"? body
 *              | "break" | "continue" | "return" expr?
 *              | "def" IDENT "(" params ")" body
 *              | "{" statement* "}"
 *              | simple
 *              | ";"
 * simple      := postfix ("=" | "+=" | "-=" | "*=" | "/=") expr | call
 * body        := "{" statement* "}" | statement
 *
 * expr        := or
 * or          := and ("||" and)*
 * and         := equality ("&&" equality)*
 * equality    := comparison (("==" | "!=") comparison)*
 * comparison  := additive (("<" | "<=" | ">" | ">=") additive)*
 * additive    := term (("+" | "-") term)*
 * term        := unary (("*" | "/" | "%") unary)*
 * unary       := ("-" | "!") unary | postfix
 * postfix     := primary ("(" args ")" | "." IDENT | "[" expr "]")*
 * primary     := NUMBER | STRING | "true" | "false" | "null" | IDENT | "(" expr ")"
 *              | "[" (expr ("," expr)*)? ","? "]"
 *              | "{" ((IDENT | STRING) ":" expr ("," ...)*)? ","? "}"
 *              | "def" "(" params ")" body
 * </pre>
 *
 * <p>Переводы строк незначимы, кроме одного места: {@code return} без значения — если следующий
 * токен на другой строке.
 */
final class Parser {

    private final List<Token> tokens;
    private final String file;
    private int pos;
    private int loopDepth;

    Parser(List<Token> tokens, String file) {
        this.tokens = tokens;
        this.file = file;
    }

    Stmt.Block parseProgram() {
        SourcePosition start = position(peek());
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            Stmt statement = statement();
            if (statement != null) {
                statements.add(statement);
            }
        }
        return new Stmt.Block(start, statements);
    }

    // ------------------------------------------------------------------ инструкции

    /** Инструкция или {@code null} для пустой ({@code ;}). */
    private Stmt statement() {
        Token token = peek();
        SourcePosition at = position(token);
        switch (token.type()) {
            case SEMICOLON:
                advance();
                return null;
            case USE:
                advance();
                return new Stmt.Use(at, expect(TokenType.STRING, "имя модуля в кавычках").text());
            case IF:
                return ifStatement();
            case WHILE:
                {
                    advance();
                    Expr condition = expression();
                    return new Stmt.While(at, condition, loopBody());
                }
            case DO:
                {
                    advance();
                    Stmt body = loopBody();
                    expect(TokenType.WHILE, "'while' после тела do");
                    return new Stmt.DoWhile(at, body, expression());
                }
            case FOR:
                return forStatement();
            case BREAK:
            case CONTINUE:
                advance();
                if (loopDepth == 0) {
                    throw error(token, "'" + token.text() + "' вне цикла");
                }
                return token.type() == TokenType.BREAK ? new Stmt.Break(at) : new Stmt.Continue(at);
            case RETURN:
                {
                    advance();
                    Token next = peek();
                    boolean hasValue =
                            next.line() == token.line()
                                    && next.type() != TokenType.RBRACE
                                    && next.type() != TokenType.SEMICOLON
                                    && next.type() != TokenType.EOF;
                    return new Stmt.Return(at, hasValue ? expression() : null);
                }
            case DEF:
                if (peek(1).type() == TokenType.IDENT) {
                    advance();
                    String name = advance().text();
                    return new Stmt.FunctionDeclaration(at, function(at, name));
                }
                return simpleStatement();
            case LBRACE:
                return block();
            default:
                return simpleStatement();
        }
    }

    private Stmt ifStatement() {
        SourcePosition at = position(advance());
        Expr condition = expression();
        Stmt then = body();
        Stmt otherwise = null;
        if (match(TokenType.ELSE)) {
            otherwise = check(TokenType.IF) ? ifStatement() : body();
        }
        return new Stmt.If(at, condition, then, otherwise);
    }

    private Stmt forStatement() {
        SourcePosition at = position(advance());
        if (check(TokenType.IDENT) && peek(1).type() == TokenType.IN) {
            String variable = advance().text();
            advance(); // in
            Expr collection = expression();
            return new Stmt.ForIn(at, variable, collection, loopBody());
        }
        // for (init, cond, inc) — скобки необязательны, разделитель ',' или ';'.
        // Скобка, открывающая выражение в init (например, "(a)[0] = 1"), не поддерживается.
        boolean parenthesized = match(TokenType.LPAREN);
        Stmt init = simpleStatement();
        expectSeparator();
        Expr condition = expression();
        expectSeparator();
        Stmt increment = simpleStatement();
        if (parenthesized) {
            expect(TokenType.RPAREN, "')' после заголовка for");
        }
        return new Stmt.For(at, init, condition, increment, loopBody());
    }

    private void expectSeparator() {
        if (!match(TokenType.COMMA) && !match(TokenType.SEMICOLON)) {
            throw error(peek(), "ожидалось ',' в заголовке for, найдено " + peek().describe());
        }
    }

    private Stmt simpleStatement() {
        Token first = peek();
        SourcePosition at = position(first);
        Expr expression = expression();
        TokenType op = peek().type();
        if (op == TokenType.ASSIGN
                || op == TokenType.PLUS_ASSIGN
                || op == TokenType.MINUS_ASSIGN
                || op == TokenType.STAR_ASSIGN
                || op == TokenType.SLASH_ASSIGN) {
            if (!(expression instanceof Expr.Variable
                    || expression instanceof Expr.Member
                    || expression instanceof Expr.Index)) {
                throw error(
                        peek(),
                        "слева от '"
                                + peek().text()
                                + "' должна быть переменная, поле или элемент");
            }
            advance();
            return new Stmt.Assign(at, expression, op, expression());
        }
        if (expression instanceof Expr.Call) {
            return new Stmt.Expression(at, expression);
        }
        throw error(first, "ожидалось присваивание или вызов функции");
    }

    private Stmt body() {
        if (check(TokenType.LBRACE)) {
            return block();
        }
        Stmt statement = statement();
        if (statement == null) {
            throw error(peek(), "пустое тело: используйте {}");
        }
        return statement;
    }

    private Stmt loopBody() {
        loopDepth++;
        try {
            return body();
        } finally {
            loopDepth--;
        }
    }

    private Stmt.Block block() {
        Token open = expect(TokenType.LBRACE, "'{'");
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE)) {
            if (check(TokenType.EOF)) {
                throw error(
                        peek(),
                        "блок не закрыт: нет '}' для '{' в строке "
                                + open.line()
                                + ":"
                                + open.column());
            }
            Stmt statement = statement();
            if (statement != null) {
                statements.add(statement);
            }
        }
        advance();
        return new Stmt.Block(position(open), statements);
    }

    private Expr.FunctionLiteral function(SourcePosition at, String name) {
        expect(TokenType.LPAREN, "'(' перед параметрами");
        List<String> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token parameter = expect(TokenType.IDENT, "имя параметра");
                if (parameters.contains(parameter.text())) {
                    throw error(parameter, "параметр '" + parameter.text() + "' повторяется");
                }
                parameters.add(parameter.text());
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RPAREN, "')' после параметров");
        int outerLoops = loopDepth;
        loopDepth = 0; // break/continue не выходят за пределы функции
        try {
            return new Expr.FunctionLiteral(at, name, List.copyOf(parameters), body());
        } finally {
            loopDepth = outerLoops;
        }
    }

    // ------------------------------------------------------------------ выражения

    private Expr expression() {
        return or();
    }

    private Expr or() {
        Expr left = and();
        while (check(TokenType.OR)) {
            SourcePosition at = position(advance());
            left = new Expr.Logical(at, false, left, and());
        }
        return left;
    }

    private Expr and() {
        Expr left = equality();
        while (check(TokenType.AND)) {
            SourcePosition at = position(advance());
            left = new Expr.Logical(at, true, left, equality());
        }
        return left;
    }

    private Expr equality() {
        Expr left = comparison();
        while (check(TokenType.EQ) || check(TokenType.NOT_EQ)) {
            Token op = advance();
            left = new Expr.Binary(position(op), op.type(), left, comparison());
        }
        return left;
    }

    private Expr comparison() {
        Expr left = additive();
        while (check(TokenType.LT)
                || check(TokenType.LT_EQ)
                || check(TokenType.GT)
                || check(TokenType.GT_EQ)) {
            Token op = advance();
            left = new Expr.Binary(position(op), op.type(), left, additive());
        }
        return left;
    }

    private Expr additive() {
        Expr left = term();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            left = new Expr.Binary(position(op), op.type(), left, term());
        }
        return left;
    }

    private Expr term() {
        Expr left = unary();
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            Token op = advance();
            left = new Expr.Binary(position(op), op.type(), left, unary());
        }
        return left;
    }

    private Expr unary() {
        if (check(TokenType.MINUS) || check(TokenType.BANG)) {
            Token op = advance();
            return new Expr.Unary(position(op), op.type(), unary());
        }
        return postfix();
    }

    private Expr postfix() {
        Expr expression = primary();
        while (true) {
            Token token = peek();
            if (match(TokenType.LPAREN)) {
                List<Expr> arguments = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        arguments.add(expression());
                    } while (match(TokenType.COMMA));
                }
                expect(TokenType.RPAREN, "')' после аргументов");
                expression = new Expr.Call(expression.position, expression, List.copyOf(arguments));
            } else if (match(TokenType.DOT)) {
                Token name = expect(TokenType.IDENT, "имя поля после '.'");
                expression = new Expr.Member(position(name), expression, name.text());
            } else if (match(TokenType.LBRACKET)) {
                Expr index = expression();
                expect(TokenType.RBRACKET, "']'");
                expression = new Expr.Index(position(token), expression, index);
            } else {
                return expression;
            }
        }
    }

    private Expr primary() {
        Token token = advance();
        SourcePosition at = position(token);
        switch (token.type()) {
            case NUMBER:
                return new Expr.Literal(at, NumberValue.of(Double.parseDouble(token.text())));
            case STRING:
                return new Expr.Literal(at, StringValue.of(token.text()));
            case TRUE:
                return new Expr.Literal(at, BoolValue.TRUE);
            case FALSE:
                return new Expr.Literal(at, BoolValue.FALSE);
            case NULL:
                return new Expr.Literal(at, NullValue.INSTANCE);
            case IDENT:
                return new Expr.Variable(at, token.text());
            case LPAREN:
                {
                    Expr inner = expression();
                    expect(TokenType.RPAREN, "')'");
                    return inner;
                }
            case LBRACKET:
                {
                    List<Expr> items = new ArrayList<>();
                    while (!check(TokenType.RBRACKET)) {
                        items.add(expression());
                        if (!match(TokenType.COMMA)) {
                            break;
                        }
                    }
                    expect(TokenType.RBRACKET, "']' в конце массива");
                    return new Expr.ArrayLiteral(at, List.copyOf(items));
                }
            case LBRACE:
                return mapLiteral(at);
            case DEF:
                return function(at, "<lambda>");
            default:
                throw error(token, "ожидалось выражение, найдено " + token.describe());
        }
    }

    private Expr mapLiteral(SourcePosition at) {
        List<String> keys = new ArrayList<>();
        List<Expr> values = new ArrayList<>();
        while (!check(TokenType.RBRACE)) {
            Token key = advance();
            if (key.type() != TokenType.IDENT && key.type() != TokenType.STRING) {
                throw error(
                        key,
                        "ключ словаря должен быть именем или строкой, найдено " + key.describe());
            }
            if (keys.contains(key.text())) {
                throw error(key, "ключ '" + key.text() + "' повторяется");
            }
            expect(TokenType.COLON, "':' после ключа словаря");
            keys.add(key.text());
            values.add(expression());
            if (!match(TokenType.COMMA)) {
                break;
            }
        }
        expect(TokenType.RBRACE, "'}' в конце словаря");
        return new Expr.MapLiteral(at, List.copyOf(keys), List.copyOf(values));
    }

    // ------------------------------------------------------------------ токены

    private Token peek() {
        return tokens.get(pos);
    }

    private Token peek(int offset) {
        return tokens.get(Math.min(pos + offset, tokens.size() - 1));
    }

    private Token advance() {
        Token token = tokens.get(pos);
        if (token.type() != TokenType.EOF) {
            pos++;
        }
        return token;
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token expect(TokenType type, String what) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), "ожидалось " + what + ", найдено " + peek().describe());
    }

    private SourcePosition position(Token token) {
        return new SourcePosition(file, token.line(), token.column());
    }

    private ScriptException error(Token token, String message) {
        return new ScriptException(message, position(token));
    }
}
