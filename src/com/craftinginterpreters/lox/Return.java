package com.craftinginterpreters.lox;

class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        super(null, null, false, false);  // Disable some JVM machinery that we don’t need
        this.value = value;
    }
}
