package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionIdent extends Expression {
    public final boolean isAddressable;

    public ExpressionIdent(Token token, SeaType type, boolean isAddressable) {
        super(token, token, type);
        this.isAddressable = isAddressable;
    }

    public String name() {
        return start.content();
    }
}
