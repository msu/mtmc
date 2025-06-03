package mtmc.lang.sea;

import mtmc.lang.sea.ast.TypeDeclaration;
import org.jetbrains.annotations.Nullable;

public record Symbol(Token token, @Nullable  TypeDeclaration typedef, SeaType type) {

    public Symbol(Token token, TypeDeclaration decl) {
        this(token, decl, SeaType.INT);
    }

    public Symbol(Token token, SeaType type) {
        this(token, null, type);
    }
}
