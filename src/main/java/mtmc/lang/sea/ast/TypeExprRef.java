package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class TypeExprRef extends TypeExpr {
    public final TypeDeclaration decl;

    public TypeExprRef(Token name, TypeDeclaration decl) {
        super(name, name, decl.type());
        this.decl = decl;
    }
}
