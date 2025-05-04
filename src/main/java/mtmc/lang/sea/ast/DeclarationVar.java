package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DeclarationVar extends Declaration {
    @NotNull
    public final TypeExpr type;

    @NotNull
    public final Token name;

    @Nullable
    public final Expression init;

    public DeclarationVar(TypeExpr type, @NotNull Token name, @Nullable Expression init) {
        super(type.start, init == null ? name : init.end);
        this.type = type;
        this.name = name;
        this.init = init;
    }
}
