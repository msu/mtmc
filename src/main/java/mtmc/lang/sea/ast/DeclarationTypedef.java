package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class DeclarationTypedef extends Declaration implements TypeDeclaration {
    public final TypeExpr type;
    public final Token name;


    public DeclarationTypedef(Token start, TypeExpr type, Token name) {
        super(start, type.end);
        this.type = type;
        this.name = name;
    }
}
