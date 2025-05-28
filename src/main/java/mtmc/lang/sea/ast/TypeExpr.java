package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public abstract sealed class TypeExpr extends Ast permits TypeExprArray, TypeExprChar, TypeExprInt, TypeExprRef, TypePointer {
    public TypeExpr(Token start, Token end) {
        super(start, end);
    }
}
