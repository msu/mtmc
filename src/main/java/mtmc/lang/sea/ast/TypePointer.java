package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class TypePointer extends TypeExpr {
    public final TypeExpr component;

    public TypePointer(TypeExpr component, Token star) {
        super(component.start, star, new SeaType.Pointer(component.type()));
        this.component = component;
    }
}
