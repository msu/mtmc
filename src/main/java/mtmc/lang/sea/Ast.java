package mtmc.lang.sea;

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

    sealed interface Expr extends Ast permits AccessExpr, BinaryExpr, Char, Error, Group, Ident, IndexExpr, InitializerList, Int, InvokeExpr, PostfixExpr, PrefixExpr, PtrAccessExpr, Str, TernaryExpr {
        @Override
        default Stream<Ast> getChildren() {
            return switch (this) {
                case AccessExpr accessExpr -> Stream.of(accessExpr.expr);
                case BinaryExpr binaryExpr -> Stream.of(binaryExpr.lhs, binaryExpr.rhs);
                case Char _char -> Stream.empty();
                case Str _str -> Stream.empty();
                case Int ignored -> Stream.empty();
                case Ident _ident -> Stream.empty();
                case Error _error -> Stream.empty();
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
                case Error _error -> Stream.of();
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
