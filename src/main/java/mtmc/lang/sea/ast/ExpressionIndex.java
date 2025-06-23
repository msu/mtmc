package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionIndex extends Expression {
    public final Expression array, index;

    public ExpressionIndex(Expression array, Expression index, Token end, SeaType type) {
        super(array.start, end, type);
        this.array = array;
        this.index = index;
    }
}
