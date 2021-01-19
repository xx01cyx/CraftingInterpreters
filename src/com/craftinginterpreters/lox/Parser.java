package com.craftinginterpreters.lox;

import java.text.ParseException;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;   // Not promise to return a usable syntax tree if an error is found
        }
    }

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        Expr expression = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expression = new Expr.Binary(expression, operator, right);
        }

        return expression;
    }

    private Expr comparison() {
        Expr expression = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expression = new Expr.Binary(expression, operator, right);
        }

        return expression;
    }

    private Expr term() {
        Expr expression = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expression = new Expr.Binary(expression, operator, right);
        }

        return expression;
    }

    private Expr factor() {
        Expr expression = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expression = new Expr.Binary(expression, operator, right);
        }

        return expression;
    }

    private Expr unary() {
        if (match(MINUS, BANG)) {
            Token operation = previous();
            Expr right = unary();
            return new Expr.Unary(operation, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(STRING, NUMBER))
            return new Expr.Literal(previous().literal);
        if (match(LEFT_PAREN)) {
            Expr expression = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expression);
        }

        throw error(peek(), "Expect expression");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd())  return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd())  current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type))  return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;   // End of a line

            switch (peek().type) {                      // New statement
                case CLASS: case FUN: case FOR: case PRINT:
                case IF: case RETURN: case VAR: case WHILE:
                    return;
            }

            advance();  // Discard tokens that might cause cascaded errors
        }

    }
}