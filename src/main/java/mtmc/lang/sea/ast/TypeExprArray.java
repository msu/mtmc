package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class TypeExprArray extends TypeExpr {
    public final TypeExpr inner;

    public TypeExprArray(TypeExpr inner, Token end) {
        super(inner.start, end);
        this.inner = inner;
    }
}
