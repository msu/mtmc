package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class TypeExprVoid extends TypeExpr {
    public TypeExprVoid(Token token) {
        super(token, token, SeaType.VOID);
    }
}
