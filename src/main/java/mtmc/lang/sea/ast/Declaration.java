package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public sealed abstract class Declaration extends Ast permits DeclarationFunc {
    public Declaration(Token start, Token end) {
        super(start, end);
    }
}
