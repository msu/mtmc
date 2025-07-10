package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

import java.util.Objects;

public abstract sealed class TypeExpr extends Ast permits TypeExprArray, TypeExprChar, TypeExprInt, TypeExprRef, TypeExprVoid, TypePointer {
    private final SeaType type;

    public TypeExpr(Token start, Token end, SeaType type) {
        super(start, end);
        this.type = Objects.requireNonNull(type);
    }

    public SeaType type() {
        return type;
    }
}
