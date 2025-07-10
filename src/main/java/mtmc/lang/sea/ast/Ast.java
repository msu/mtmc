package mtmc.lang.sea.ast;

import mtmc.lang.Span;
import mtmc.lang.sea.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public sealed abstract class Ast permits Declaration, DeclarationFunc.Param, DeclarationStruct.Field, Expression, Statement, TypeExpr, Unit {
    public final Token start, end;

    public Ast(Token start, Token end) {
        this.start = start;
        this.end = end;
    }

    public Span span() {
        return Span.of(start, end);
    }

    public Stream<Ast> getChildren() {
        return switch (this) {
            case DeclarationFunc declarationFunc -> {
                Stream<Ast> out = Stream.of(declarationFunc.returnType);
                out = Stream.concat(out, declarationFunc.params.params().stream());
                if (declarationFunc.body != null) {
                    out = Stream.concat(out, Stream.of(declarationFunc.body));
                }
                yield out;
            }
            case DeclarationSyntaxError ignored -> Stream.empty();
            case DeclarationTypedef declarationTypedef -> Stream.of(declarationTypedef.type);
            case DeclarationVar declarationVar -> {
                Stream<Ast> out = Stream.of(declarationVar.type);
                if (declarationVar.initializer != null) {
                    out = Stream.concat(out, Stream.of(declarationVar.initializer));
                }
                yield out;
            }
            case DeclarationStruct struct -> struct.fields.stream().map(x -> x);
            case DeclarationStruct.Field field -> Stream.of(field.type);
            case DeclarationFunc.Param param -> Stream.of(param.type);
            case ExpressionAccess expressionAccess -> Stream.of(expressionAccess.value);
            case ExpressionBin expressionBin -> Stream.of(expressionBin.lhs, expressionBin.rhs);
            case ExpressionCall expressionCall -> {
                Stream<Ast> out = Stream.of(expressionCall.functor);
                out = Stream.concat(out, expressionCall.args.stream());
                yield out;
            }
            case ExpressionInitializer init -> init.values.stream().map(x -> x);
            case ExpressionCast expressionCast -> Stream.of(expressionCast.type, expressionCast.value);
            case ExpressionChar ignored -> Stream.empty();
            case ExpressionIdent ignored -> Stream.empty();
            case ExpressionIndex expressionIndex -> Stream.of(expressionIndex.array, expressionIndex.index);
            case ExpressionInteger ignored -> Stream.empty();
            case ExpressionParens expressionParens -> Stream.of(expressionParens.inner);
            case ExpressionPostfix expressionPostfix -> Stream.of(expressionPostfix.inner);
            case ExpressionPrefix expressionPrefix -> Stream.of(expressionPrefix.inner);
            case ExpressionString ignored -> Stream.empty();
            case ExpressionTypeError typeError -> Stream.of(typeError.inner);
            case ExpressionSyntaxError expressionSyntaxError -> {
                if (expressionSyntaxError.child != null) {
                    yield Stream.of(expressionSyntaxError.child);
                } else {
                    yield Stream.empty();
                }
            }
            case ExpressionTernary expressionTernary -> Stream.of(
                    expressionTernary.cond,
                    expressionTernary.then,
                    expressionTernary.otherwise
            );
            case StatementBlock statementBlock -> statementBlock.statements.stream().map(x -> x);
            case StatementBreak ignored -> Stream.empty();
            case StatementContinue ignored -> Stream.empty();
            case StatementDoWhile statementDoWhile -> Stream.of(statementDoWhile.body, statementDoWhile.condition);
            case StatementExpression statementExpression -> Stream.of(statementExpression.expression);
            case StatementFor statementFor -> {
                Stream<Ast> out = Stream.empty();
                if (statementFor.initExpression != null) {
                    out = Stream.concat(out, Stream.of(statementFor.initExpression));
                } else if (statementFor.initStatement != null) {
                    out = Stream.concat(out, Stream.of(statementFor.initStatement));
                }
                if (statementFor.condition != null) {
                    out = Stream.concat(out, Stream.of(statementFor.condition));
                }
                if (statementFor.inc != null) {
                    out = Stream.concat(out, Stream.of(statementFor.inc));
                }
                out = Stream.concat(out, Stream.of(statementFor.body));
                yield out;
            }
            case StatementGoto ignored -> Stream.empty();
            case StatementIf statementIf -> {
                Stream<Ast> out = Stream.of(statementIf.condition, statementIf.body);
                if (statementIf.elseBody != null) out = Stream.concat(out, Stream.of(statementIf.elseBody));
                yield out;
            }
            case StatementReturn statementReturn -> {
                if (statementReturn.value == null) {
                    yield Stream.empty();
                } else {
                    yield Stream.of(statementReturn.value);
                }
            }
            case StatementSyntaxError ignored -> Stream.empty();
            case StatementVar statementVar -> {
                Stream<Ast> out = Stream.of(statementVar.type);
                if (statementVar.initValue != null) {
                    out = Stream.concat(out, Stream.of(statementVar.initValue));
                }
                yield out;
            }
            case StatementWhile statementWhile -> Stream.of(statementWhile.condition, statementWhile.body);
            case TypeExprArray typeExprArray -> Stream.of(typeExprArray.inner);
            case TypeExprChar ignored -> Stream.empty();
            case TypeExprInt ignored -> Stream.empty();
            case TypeExprRef ignored -> Stream.empty();
            case TypeExprVoid ignored -> Stream.empty();
            case TypePointer ignored -> Stream.empty();
            case Unit unit -> unit.declarations.stream().map(x -> x);
        };
    }

    public List<Error> collectErrors() {
        var errors = new ArrayList<Error>();
        collectErrors(errors);
        return errors;
    }

    public void collectErrors(List<Error> errors) {
        if (this instanceof Error e) {
            errors.add(e);
        }
        getChildren().forEach(child -> child.collectErrors(errors));
    }
}
