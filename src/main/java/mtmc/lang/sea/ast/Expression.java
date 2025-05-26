package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public sealed abstract class Expression extends Ast permits ExpressionAccess, ExpressionBin, ExpressionCall, ExpressionCast, ExpressionChar, ExpressionIdent, ExpressionIndex, ExpressionInteger, ExpressionParens, ExpressionPostfix, ExpressionPrefix, ExpressionString, ExpressionSyntaxError, ExpressionTernary, ExpressionTypeError {
    public Expression(Token start, Token end) {
        super(start, end);
    }

    public SeaType getType() {
        return switch (this) {
            case ExpressionAccess expressionAccess -> null;
            case ExpressionBin expressionBin -> switch (expressionBin.op()) {
                case "=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", "&=", "|=" -> SeaType.Void;
                case "||", "&&", "==", "!=", "&", "|", "^", ">>", "<<", "+", "-", "*", "/", "%" -> SeaType.Int;
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
        };
    }
}
