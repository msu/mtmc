package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionAccess extends Expression {
    public final Expression value;
    public final Token access;
    public final Token prop;

    public ExpressionAccess(Expression value, Token access, Token prop, SeaType type) {
        super(value.start, prop, type);
        this.value = value;
        this.access = access;
        this.prop = prop;
    }
}
