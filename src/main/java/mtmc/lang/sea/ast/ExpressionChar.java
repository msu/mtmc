package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionChar extends Expression {
    public ExpressionChar(Token token) {
        super(token, token);
    }

    public byte getByte() {
        throw new RuntimeException("UNIMPLEMENTED");
    }
}
