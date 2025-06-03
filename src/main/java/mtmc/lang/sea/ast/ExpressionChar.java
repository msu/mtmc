package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionChar extends Expression {
    public ExpressionChar(Token token) {
        super(token, token, SeaType.CHAR);
    }

    public Character content() {
        return start.content().charAt(0);
    }
}
