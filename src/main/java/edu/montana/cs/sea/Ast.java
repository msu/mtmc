package edu.montana.cs.sea;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public sealed interface Ast permits Ast.Expr, Ast.Initializer {
    @NotNull Token start();

    @NotNull Token end();

    default Stream<Ast> getChildren() {
        return switch (this) {
            case Expr expr -> expr.getChildren();
            case Initializer initializer -> initializer.getChildren();
        };
    }

    interface Visitor<T> {
        default T visit(final Ast ast) {
            return switch (ast) {
                case Expr e -> visitExpr(e);
                case Initializer e -> visitInitializer(e);
            };
        }

        default T visitExpr(final Expr expr) {
            return switch (expr) {
                case AccessExpr accessExpr -> visitExpr(accessExpr);
                case BinaryExpr binaryExpr -> visitExpr(binaryExpr);
                case Char aChar -> visitExpr(aChar);
                case Error error -> visitExpr(error);
                case Group group -> visitExpr(group);
                case Ident ident -> visitExpr(ident);
                case IndexExpr indexExpr -> visitExpr(indexExpr);
                case InitializerList initializerList -> visitExpr(initializerList);
                case Int anInt -> visitExpr(anInt);
                case InvokeExpr invokeExpr -> visitExpr(invokeExpr);
                case PostfixExpr postfixExpr -> visitExpr(postfixExpr);
                case PrefixExpr prefixExpr -> visitExpr(prefixExpr);
                case PtrAccessExpr ptrAccessExpr -> visitExpr(ptrAccessExpr);
                case Str str -> visitExpr(str);
                case TernaryExpr ternaryExpr -> visitExpr(ternaryExpr);
            };
        }

        default T visitExpr(Error error) {
            return visitDefaultExpr(error);
        }

        default T visitExpr(AccessExpr expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(BinaryExpr expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(Char expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(Group expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(Ident expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(IndexExpr expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(InitializerList expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(Int expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(InvokeExpr expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(PostfixExpr expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(PrefixExpr expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(PtrAccessExpr expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(Str expr) {
            return visitDefaultExpr(expr);
        }

        default T visitExpr(TernaryExpr expr) {
            return visitDefaultExpr(expr);
        }

        default T visitDefaultExpr(Expr expr) {
            return null;
        }

        default T visitInitializer(Initializer init) {
            return switch (init) {
                case Error error -> visitInitializer(error);
                case FieldInitializer fieldInitializer -> visitInitializer(fieldInitializer);
                case ValueInitializer valueInitializer -> visitInitializer(valueInitializer);
            };
        }

        default T visitInitializer(Error item) {
            return visitDefaultInitializer(item);
        }

        default T visitInitializer(FieldInitializer item) {
            return visitDefaultInitializer(item);
        }

        default T visitInitializer(ValueInitializer item) {
            return visitDefaultInitializer(item);
        }

        default T visitDefaultInitializer(Initializer item) {
            return null;
        }
    }

    record Span(Token start, Token end) {
        public static Span span(final Token start, final Token end) {
            return new Span(start, end);
        }
    }

    sealed interface Expr extends Ast permits AccessExpr, BinaryExpr, Char, Error, Group, Ident, IndexExpr, InitializerList, Int, InvokeExpr, PostfixExpr, PrefixExpr, PtrAccessExpr, Str, TernaryExpr {
        @Override
        default Stream<Ast> getChildren() {
            return switch (this) {
                case AccessExpr accessExpr -> Stream.of(accessExpr.expr);
                case BinaryExpr binaryExpr -> Stream.of(binaryExpr.lhs, binaryExpr.rhs);
                case Char _, Error _, Ident _, Int _, Str _ -> Stream.empty();
                case Group group -> Stream.of(group.expr);
                case IndexExpr indexExpr -> Stream.of(indexExpr.expr, indexExpr.index);
                case Initializer initializer -> initializer.getChildren();
                case InitializerList initializerList ->
                        initializerList.initializers.stream().flatMap(Initializer::getChildren);
                case InvokeExpr expr -> {
                    Stream<Ast> functor = Stream.of(expr.functor);
                    Stream<Ast> args = expr.args.stream().map(e -> e);
                    yield Stream.concat(functor, args);
                }
                case PostfixExpr postfixExpr -> Stream.of(postfixExpr.lhs);
                case PrefixExpr prefixExpr -> Stream.of(prefixExpr.rhs);
                case PtrAccessExpr ptrAccessExpr -> Stream.of(ptrAccessExpr.expr, ptrAccessExpr.field);
                case TernaryExpr ternaryExpr -> Stream.of(ternaryExpr.cond, ternaryExpr.then, ternaryExpr.other);
            };
        }
    }

//    sealed interface Stmt extends SeaAst {
//    }
//
//    sealed interface Decl extends SeaAst {
//    }

    record Error(Token start, Token end, Map<Token, String> messages) implements Expr, Initializer {
        @Override
        public Stream<Ast> getChildren() {
            return Stream.of();
        }
    }

    record Int(@NotNull Token token) implements Expr {
        public int value() {
            return Integer.parseInt(token.content());
        }

        @NotNull
        @Override
        public Token start() {
            return token;
        }

        @NotNull
        @Override
        public Token end() {
            return token;
        }
    }

    record Str(Token token) implements Expr {
        public String content() {
            return token.content();
        }

        @NotNull
        @Override
        public Token start() {
            return token;
        }

        @NotNull
        @Override
        public Token end() {
            return token;
        }
    }

    record Char(Token token) implements Expr {
        public char value() {
            return token.content().charAt(0);
        }

        @NotNull
        @Override
        public Token start() {
            return token;
        }

        @NotNull
        @Override
        public Token end() {
            return token;
        }
    }

    record Ident(Token token) implements Expr {
        public String name() {
            return token.content();
        }

        @NotNull
        @Override
        public Token start() {
            return token;
        }

        @NotNull
        @Override
        public Token end() {
            return token;
        }
    }

    record Group(Token start, Expr expr, Token end) implements Expr {

    }

    record BinaryExpr(Expr lhs, Token op, Expr rhs) implements Expr {
        @NotNull
        @Override
        public Token start() {
            return lhs.start();
        }

        @NotNull
        @Override
        public Token end() {
            return rhs.end();
        }
    }

    record TernaryExpr(Expr cond, Expr then, Expr other) implements Expr {
        @NotNull
        @Override
        public Token start() {
            return cond.start();
        }

        @NotNull
        @Override
        public Token end() {
            return other.end();
        }
    }

    record PrefixExpr(Token op, Expr rhs) implements Expr {
        @NotNull
        @Override
        public Token start() {
            return op;
        }

        @NotNull
        @Override
        public Token end() {
            return rhs.end();
        }
    }

    record PostfixExpr(Expr lhs, Token op) implements Expr {
        @NotNull
        @Override
        public Token start() {
            return lhs.start();
        }

        @NotNull
        @Override
        public Token end() {
            return op;
        }
    }

    record AccessExpr(Expr expr, Ident ident) implements Expr {
        @NotNull
        @Override
        public Token start() {
            return expr.start();
        }

        @NotNull
        @Override
        public Token end() {
            return ident.end();
        }
    }

    record PtrAccessExpr(Expr expr, Ident field) implements Expr {
        @NotNull
        @Override
        public Token start() {
            return expr.start();
        }

        @NotNull
        @Override
        public Token end() {
            return field.end();
        }
    }

    record InvokeExpr(Expr functor, List<Expr> args, Token end) implements Expr {
        @NotNull
        @Override
        public Token start() {
            return functor.start();
        }
    }

    record IndexExpr(Expr expr, Expr index, Token end) implements Expr {
        @NotNull
        @Override
        public Token start() {
            return expr.start();
        }
    }

    sealed interface Initializer extends Ast permits Error, FieldInitializer, ValueInitializer {
        @Override
        default Stream<Ast> getChildren() {
            return switch (this) {
                case Error _ -> Stream.of();
                case FieldInitializer fieldInitializer -> Stream.of(fieldInitializer.field, fieldInitializer.value);
                case ValueInitializer valueInitializer -> Stream.of(valueInitializer.value);
            };
        }
    }

    record ValueInitializer(Expr value) implements Initializer {
        @NotNull
        @Override
        public Token start() {
            return value.start();
        }

        @NotNull
        @Override
        public Token end() {
            return value.end();
        }
    }

    record FieldInitializer(Token start, Ident field, Expr value) implements Initializer {
        @NotNull
        @Override
        public Token end() {
            return value.end();
        }
    }

    record InitializerList(Token start, List<Initializer> initializers, Token end) implements Expr {

    }
}
