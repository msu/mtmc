package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public abstract sealed class TypeExpr extends Ast permits TypeExprArray, TypeExprChar, TypeExprInt, TypeExprRef, TypeExprVoid, TypePointer {
    private final SeaType type;

    public TypeExpr(Token start, Token end, SeaType type) {
        super(start, end);
        this.type = type;
    }

    public SeaType type() {
        return type;
    }
}
