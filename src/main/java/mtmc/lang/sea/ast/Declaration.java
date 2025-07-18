package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public abstract sealed class Declaration extends Ast permits DeclarationFunc, DeclarationStruct, DeclarationSyntaxError, DeclarationTypedef, DeclarationVar {
    public Declaration(Token start, Token end) {
        super(start, end);
    }
}
