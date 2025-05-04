package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

import java.util.List;

public final class DeclarationFunc extends Declaration {
    public record Param(TypeExpr type, Token name) {}

    public final TypeExpr returnType;
    public final Token name;
    public final List<Param> params;

    public DeclarationFunc(TypeExpr returnType, Token name, List<Param> params, StatementBlock block, Token end) {
        super(returnType.start, end);
        this.returnType = returnType;
        this.name = name;
        this.params = params;
    }
}
