package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionString extends Expression {
    public ExpressionString(Token token) {
        super(token, token);
    }

    public byte[] getBytes() {
        return start.content().getBytes();
    }

    public String content() {
        return start.content();
    }
}
