package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class TypeExprInt extends TypeExpr {
    public TypeExprInt(Token token) {
        super(token, token, SeaType.INT);
    }
}
