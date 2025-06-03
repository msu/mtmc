package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public sealed abstract class Expression extends Ast permits ExpressionAccess, ExpressionBin, ExpressionCall, ExpressionCast, ExpressionChar, ExpressionIdent, ExpressionIndex, ExpressionInteger, ExpressionParens, ExpressionPostfix, ExpressionPrefix, ExpressionString, ExpressionSyntaxError, ExpressionTernary, ExpressionTypeError {
    private final SeaType type;
    public Expression(Token start, Token end, SeaType type) {
        super(start, end);
        this.type = Objects.requireNonNull(type, "'type' cannot be null");
    }

    public SeaType type() {
        return type;
    }

    public enum ValueKind {
        LValue,
        RValue;
    }

    public ValueKind valueKind() {
        return switch (this) {
            case ExpressionAccess ignored -> ValueKind.LValue;
            case ExpressionBin ignored -> ValueKind.RValue;
            case ExpressionCall ignored -> ValueKind.RValue;
            case ExpressionCast ignored -> ValueKind.RValue;
            case ExpressionChar ignored -> ValueKind.RValue;
            case ExpressionIdent ignored -> ValueKind.LValue;
            case ExpressionIndex ignored -> ValueKind.LValue;
            case ExpressionInteger ignored -> ValueKind.LValue;
            case ExpressionParens expressionParens -> expressionParens.valueKind();
            case ExpressionPostfix ignored -> ValueKind.RValue;
            case ExpressionPrefix prefix -> {
                if (prefix.op().equals("*")) {
                    yield ValueKind.LValue;
                } else {
                    yield ValueKind.RValue;
                }
            }
            case ExpressionString ignored -> ValueKind.LValue;
            case ExpressionSyntaxError ignored -> ValueKind.RValue;
            case ExpressionTernary ignored -> ValueKind.RValue;
            case ExpressionTypeError ignored -> ignored.valueKind();
        };
    }
}
