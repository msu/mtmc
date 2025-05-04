package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public class TypePointer extends TypeExpr {
    public final TypeExpr parent;

    public TypePointer(TypeExpr parent, Token star) {
        super(parent.start, star);
        this.parent = parent;
    }
}
