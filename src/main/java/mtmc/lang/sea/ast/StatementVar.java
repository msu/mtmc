package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementVar extends Statement {
    public final TypeExpr type;
    public final Token name;
    public final Expression initValue;

    public StatementVar(TypeExpr type, Token name, Expression initValue) {
        super(type.start, initValue == null ? name : initValue.end);
        this.type = type;
        this.name = name;
        this.initValue = initValue;
    }

    public String name() {
        return name.content();
    }
}
