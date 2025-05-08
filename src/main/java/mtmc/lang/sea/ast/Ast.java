package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public sealed abstract class Ast permits Declaration, Expression, Statement, TypeExpr, Unit {
    public final Token start, end;

    public Ast(Token start, Token end) {
        this.start = start;
        this.end = end;
    }
}
