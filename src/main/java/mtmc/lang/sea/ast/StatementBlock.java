package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

import java.util.List;

public final class StatementBlock extends Statement {
    public final List<Statement> statements;

    public StatementBlock(Token start, List<Statement> children, Token end) {
        super(start, end);
        this.statements = List.copyOf(children);
    }
}
