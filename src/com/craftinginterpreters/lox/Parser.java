package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<Stmt>();

        while(!isAtEnd())
            statements.add(declaration());

        return statements;
    }


    // Statement

    private Stmt declaration() {
        try {
            if (match(VAR))
                return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(EQUAL))
            initializer = expression();
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(PRINT))
            return printStatement();
        if (match(LEFT_BRACE))
            return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd())
            statements.add(declaration());

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    // Expression

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expression = equality();   // L-value might be an expression (e.g. `newPoint(x+2, 0).y`)

        if (match(EQUAL)) {
            Token equals = previous();  // The EQUAL token
            Expr value = assignment();  // Recursively call `assignment()` since assignment(=) is right-associative

            if (expression instanceof Expr.Variable) {  // Assignable
                Token name = ((Expr.Variable)expression).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }

        return expression;
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
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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