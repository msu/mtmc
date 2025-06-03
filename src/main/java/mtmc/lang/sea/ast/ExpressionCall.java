package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

import java.util.List;

public final class ExpressionCall extends Expression {
    public final Expression functor;
    public final List<Expression> args;

    public ExpressionCall(Expression functor, List<Expression> args, Token end, SeaType type) {
        super(functor.start, end, type);
        this.functor = functor;
        this.args = args;
    }
}
