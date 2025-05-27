package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public sealed abstract class Expression extends Ast permits ExpressionAccess, ExpressionBin, ExpressionCall, ExpressionCast, ExpressionChar, ExpressionIdent, ExpressionIndex, ExpressionInteger, ExpressionParens, ExpressionPostfix, ExpressionPrefix, ExpressionString, ExpressionSyntaxError, ExpressionTernary, ExpressionTypeError {
    public Expression(Token start, Token end) {
        super(start, end);
    }

    private SeaType type;
    public SeaType getType() {
        if (type != null) return type;
        type = switch (this) {
            case ExpressionAccess expressionAccess -> null;
            case ExpressionBin bin -> switch (bin.op()) {
                case "=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", "&=", "|=" -> SeaType.VOID;
                case "||", "&&", "==", "!=", "&", "|", "^", ">>", "<<", "*", "/", "%" -> SeaType.INT;
                case "+", "-" -> {
                    if (bin.lhs.getType().isAPointer() && bin.rhs.getType().isAPointer()) {

                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + bin.op());
            };
            case ExpressionCall expressionCall -> null;
            case ExpressionCast expressionCast -> null;
            case ExpressionChar expressionChar -> null;
            case ExpressionIdent expressionIdent -> null;
            case ExpressionIndex expressionIndex -> null;
            case ExpressionInteger expressionInteger -> null;
            case ExpressionParens expressionParens -> null;
            case ExpressionPostfix expressionPostfix -> null;
            case ExpressionPrefix expressionPrefix -> null;
            case ExpressionString expressionString -> null;
            case ExpressionSyntaxError expressionSyntaxError -> null;
            case ExpressionTernary expressionTernary -> null;
            case ExpressionTypeError expressionTypeError -> null;
        };
        return type;
    }
}
