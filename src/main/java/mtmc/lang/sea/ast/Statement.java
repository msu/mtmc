package mtmc.lang.sea.ast;

import mtmc.lang.sea.ParseException;
import mtmc.lang.sea.Token;

public abstract sealed class Statement extends Ast permits StatementBlock, StatementBreak, StatementContinue,
        StatementDoWhile, StatementExpression, StatementFor, StatementGoto, StatementIf,
        StatementSyntaxError, StatementVar, StatementWhile
{
    private Token labelAnchor = null;

    public Statement(Token start, Token end) {
        super(start, end);
    }

    public void setLabelAnchor(Token labelAnchor) throws ParseException {
        if (labelAnchor == null) return;
        if (this.labelAnchor != null) {
            throw new ParseException(labelAnchor, "this statement has been labeled twice!!");
        }
        this.labelAnchor = labelAnchor;
    }

    public Token getLabelAnchor() {
        return labelAnchor;
    }
}
