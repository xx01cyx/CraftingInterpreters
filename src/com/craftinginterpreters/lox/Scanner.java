package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;  // List is an Interface
import java.util.Map;   // Map is an Interface

// Attributes or methods could be used directly via static import
// Statically importing a class or an enum is not allowed
import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }
    
    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '/':
                if (match('/'))
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                else
                    addToken(SLASH);
                break;
            case '"': scanString(); break;

            // Ignore whitespace
            case ' ': case '\r': case '\t': break;
            case '\n': line++; break;

            default:
                if (isDigit(c))
                    scanNumber();
                else if (isAlpha(c))
                    scanIdentifier();
                else
                    Lox.error(line, "Unexpected character.");
                break;
        }
    }

    // One subcase of `ScanToken`
    private void scanString() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;    // Lox supports multi-line strings
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string");
            return;
        }

        // The closing `"`
        advance();

        // Trim the surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // The other subcase of `ScanToken`
    private void scanNumber() {
        while (isDigit(peek()))    // Integral part
            advance();

        if (peek() == '.' && isDigit(peekNext())) {    // Fractional part
            advance();    // Skip `.`
            while (isDigit(peek()))
                advance();
        }

        String value = source.substring(start, current);
        addToken(NUMBER, Double.parseDouble(value));
    }

    // Another subcase of `ScanToken`
    private void scanIdentifier() {
        while (isAlphaNumeric(peek()))  advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null)  type = IDENTIFIER;
        addToken(type);
    }

    // Add a non-literal token to `tokens`
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    // Add a literal token to `tokens`
    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, literal, line));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // Advance and scan the next character
    private char advance() {
        current++;    // The character that `current` is pointing to is always one character ahead of the scanned one
        return source.charAt(current - 1);
    }

    // Check if the next character matches the expected one
    private boolean match(char expected) {
        if (isAtEnd())  return false;

        if (source.charAt(current) == expected) {
            current++;
            return true;
        }
        return false;
    }

    // Peek the character that `current` is pointing to
    private char peek() {
        if (isAtEnd())  return '\0';
        return source.charAt(current);
    }

    // Peek the character after the one that `current` is pointing to
    private char peekNext() {
        if (current + 1 >= source.length())  return '\0';
        return source.charAt(current + 1);
    }
}
