package com.craftinginterpreters.lox;

import java.security.spec.EncodedKeySpec;
import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;
    final boolean isStatic;

    LoxFunction(Stmt.Function declaration, Environment closure,
                boolean isInitializer, boolean isStatic) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
        this.isStatic = isStatic;
    }

    LoxFunction bind(LoxInstance instance) {    // Implement for `this` in a class
        Environment environment = new Environment(closure);
        environment.define("this", instance);  // Add `this` to environment of each method in a class
        return new LoxFunction(declaration, environment, isInitializer, false);  // Static methods do not allow `this`
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.params.size(); ++i)
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if (isInitializer)  return closure.getAt(0, "this");
            return returnValue.value;
        }

        // `this` and `init()` are in the same environment
        if (isInitializer)  return closure.getAt(0, "this");

        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}

class LoxLambda implements LoxCallable {
    private final Expr.Lambda lambda;
    private final Environment closure;

    LoxLambda(Expr.Lambda lambda, Environment closure) {
        this.lambda = lambda;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return lambda.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);

        for (int i = 0; i < lambda.params.size(); ++i)
            environment.define(lambda.params.get(i).lexeme, arguments.get(i));

        try {
            interpreter.executeBlock(lambda.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn lambda>";
    }

}