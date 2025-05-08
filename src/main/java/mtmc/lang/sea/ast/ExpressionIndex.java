package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionIndex extends Expression {
    public final Expression array, index;

    public ExpressionIndex(Expression array, Expression index, Token end) {
        super(array.start, end);
        this.array = array;
        this.index = index;
    }
}
