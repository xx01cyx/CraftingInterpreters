package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS  // Check the validation of `super`
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements)
            resolve(statement);
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }


    // Statements

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        boolean hasSuperclass = (stmt.superclass != null);

        if (hasSuperclass && stmt.name.lexeme.equals(stmt.superclass.name.lexeme))
            Lox.error(stmt.superclass.name, "A class cannot inherit from itself.");

        if (hasSuperclass) {
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);
        }

        if (hasSuperclass) {
            beginScope();
            scopes.peek().put("super", true);
        }

        // Whenever a `this` expression is encountered, it will resolve to
        // a “local variable” defined in an implicit scope just outside of
        // the block for the method body.
        beginScope();
        scopes.peek().put("this", true);

        for (Stmt.Function nonstaticMethod : stmt.nonstaticMethods) {
            FunctionType declaration = FunctionType.METHOD;
            if (nonstaticMethod.name.lexeme.equals("init"))
                declaration = FunctionType.INITIALIZER;
            resolveFunction(nonstaticMethod, declaration);
        }

        for (Stmt.Function staticMethod : stmt.staticMethods) {
            if (staticMethod.name.lexeme.equals("init"))
                Lox.error(staticMethod.name, "Init method of a class cannot be static.");
            resolveFunction(staticMethod, FunctionType.METHOD);
        }

        endScope();

        if (hasSuperclass)
            endScope();

        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    /**
     * At runtime declaring a function doesn't do anything with the function’s body.
     * The body doesn't get touched until later when the function is called.
     * In a static analysis, we immediately traverse into the body right then and there.
     */
    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {  // No control flow
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null)
            resolve(stmt.elseBranch);

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE)
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        else if (currentFunction == FunctionType.INITIALIZER)
            Lox.error(stmt.keyword, "Can't return a value from an initializer.");

        if (stmt.value != null)
            resolve(stmt.value);

        return null;
    }

    /**
     * `declare` and `define` are separated in this case, since errors
     * might occur when resolving `stmt.initializer`. For example,
     * `var a;  a = a;` Namely, referencing a variable in its initializer,
     * which will be handled in `visitVariableExpr`.
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null)
            resolve(stmt.initializer);
        define(stmt.name);

        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }


    // Expressions

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments)
            resolve(argument);
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);  // Properties are looked up DYNAMICALLY, so they don't get resolved
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLambdaExpr(Expr.Lambda expr) {
        resolveLambda(expr, FunctionType.FUNCTION);
        return null;
    }

    private void resolveLambda(Expr.Lambda lambda, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : lambda.params) {
            declare(param);
            define(param);
        }
        resolve(lambda.body);
        endScope();

        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);  // No short-circuit
        return null;
    }

    /**
     * The property is DYNAMICALLY evaluated, so all we need to do
     * is to resolve the object whose property is being set and
     * the value it's being set to.
     */
    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);  // Resolve `expr.value` first!
        resolve(expr.object);
        return null;
    }

    /**
     * The resolution for `super` store the hops along the environment
     * chain that the interpreter has to walk to find the environment
     * where the superclass is stored.
     */
    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE)
            Lox.error(expr.keyword, "Cannot use 'super' outside of a class.");
        else if (currentClass != ClassType.SUBCLASS)
            Lox.error(expr.keyword, "Cannot use 'super' in a class with no superclass.");

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.empty() &&
            scopes.peek().get(expr.name.lexeme) == Boolean.FALSE)  // Haven't been initialized yet
            Lox.error(expr.name, "Can't read local variable in its own initializer.");

        resolveLocal(expr, expr.name);

        return null;
    }


    // Utils

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; --i) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void declare(Token name) {
        if (scopes.empty())  return;  // Global variables won't be pushed into the stack

        Map<String, Boolean> scope = scopes.peek();

        // Re-declaration in the same scope is not allowed
        if (scope.containsKey(name.lexeme))
            Lox.error(name, "Already variable with this name in this scope.");

        scope.put(name.lexeme, false);  // Mark the variable as existing but not-ready-yet
    }

    private void define(Token name) {
        if (scopes.empty())  return;

        // Mark the variable as fully initialized and available for use
        scopes.peek().put(name.lexeme, true);
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

}
