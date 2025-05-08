package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public sealed abstract class Expression extends Ast permits ExpressionAccess, ExpressionBin, ExpressionCall, ExpressionCast, ExpressionChar, ExpressionIdent, ExpressionIndex, ExpressionInteger, ExpressionParens, ExpressionPostfix, ExpressionPrefix, ExpressionString, ExpressionSyntaxError, ExpressionTernary {
    public Expression(Token start, Token end) {
        super(start, end);
    }
}
