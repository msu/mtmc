package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

import java.util.Objects;

public sealed abstract class Expression extends Ast permits ExpressionAccess, ExpressionBin, ExpressionCall, ExpressionCast, ExpressionChar, ExpressionIdent, ExpressionIndex, ExpressionInitializer, ExpressionInteger, ExpressionParens, ExpressionPostfix, ExpressionPrefix, ExpressionString, ExpressionSyntaxError, ExpressionTernary, ExpressionTypeError {
    private final SeaType type;
    public Expression(Token start, Token end, SeaType type) {
        super(start, end);
        this.type = Objects.requireNonNull(type, "'type' cannot be null");
    }

    public SeaType type() {
        return type;
    }

    public enum ValueKind {
        Addressable,
        Immediate
    }
}
