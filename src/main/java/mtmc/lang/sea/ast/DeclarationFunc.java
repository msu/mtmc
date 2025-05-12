package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

import java.util.List;

public final class DeclarationFunc extends Declaration {
    public final TypeExpr returnType;
    public final Token name;
    public final List<Param> params;
    public final StatementBlock body;

    public DeclarationFunc(TypeExpr returnType, Token name, List<Param> params, StatementBlock body, Token end) {
        super(returnType.start, end);
        this.returnType = returnType;
        this.name = name;
        this.params = params;
        this.body = body;
    }

    public static final class Param extends Ast {
        public final TypeExpr type;
        public final Token name;

        public Param(TypeExpr type, Token name) {
            super(type.start, name.end() < type.end.end() ? type.end : name);
            this.type = type;
            this.name = name;
        }
    }
}