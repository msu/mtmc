package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class TypeExprChar extends TypeExpr {
    public final Token token;

    public TypeExprChar(Token token) {
        super(token, token, SeaType.CHAR);
        this.token = token;
    }
}
