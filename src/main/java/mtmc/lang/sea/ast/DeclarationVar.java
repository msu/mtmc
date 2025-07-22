package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class DeclarationVar extends Declaration {
    public final TypeExpr type;
    public final Token name;
    public final Expression initializer;

    public DeclarationVar(TypeExpr type, Token name, Expression initializer) {
        super(type.start, initializer == null ? name : initializer.end);
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    public String name() {
        return this.name.content();
    }
}