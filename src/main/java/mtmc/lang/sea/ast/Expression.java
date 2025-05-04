package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public sealed abstract class Expression extends Ast permits ExpressionParens, ExpressionIdent, ExpressionInteger {
    public Expression(Token start, Token end) {
        super(start, end);
    }
}
