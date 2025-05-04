package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public abstract sealed class Statement extends Ast permits StatementBlock, StatementSyntaxError {
    public Statement(Token start, Token end) {
        super(start, end);
    }
}
