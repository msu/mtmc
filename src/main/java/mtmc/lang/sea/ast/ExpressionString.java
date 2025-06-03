package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionString extends Expression {
    public ExpressionString(Token token) {
        super(token, token, new SeaType.Pointer(SeaType.CHAR));
    }

    public byte[] getBytes() {
        return start.content().getBytes();
    }

    public String content() {
        return start.content();
    }
}
