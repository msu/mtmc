package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;
import org.jetbrains.annotations.NotNull;

public final class ExpressionIdent extends Expression {
    public ExpressionIdent(Token name) {
        super(name, name);
    }

    @NotNull
    public String getName() {
        return start.content();
    }
}
