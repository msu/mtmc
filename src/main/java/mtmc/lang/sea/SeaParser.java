package mtmc.lang.sea;

import mtmc.lang.ParseException;
import mtmc.lang.ParseException.Message;
import mtmc.lang.Span;
import mtmc.lang.sea.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static mtmc.lang.sea.Token.Type.*;

public class SeaParser {
    private final List<Token> tokens;
    private int index = 0;

    private final LinkedHashMap<String, Symbol> symbols;
    private Vector<LinkedHashMap<String, Symbol>> scope = new Vector<>();

    {
        scope.add(new LinkedHashMap<>());
    }

    public SeaParser(@NotNull List<Token> tokens) {
        this.tokens = tokens;
        this.symbols = new LinkedHashMap<>();
    }

    public Unit parseUnit() throws ParseException {
        final var declarations = new ArrayList<Declaration>();

        while (hasMoreTokens()) {
            Token start = peekToken();

            try {
                var decl = parseDeclaration();
                declarations.add(decl);
            } catch (ParseException e) {
                declarations.add(new DeclarationSyntaxError(start, e));
                if (bodyBrace != null) {
                    consumeMatchingBrace(bodyBrace);
                } else {
                    consumeThrough(SEMICOLON);
                }
            }

            if (peekToken().equals(start)) {
                var msg = new Message(start, "fatal(langdev): infinite loop detected");
                throw new ParseException(msg);
            }
        }

        if (!symbols.containsKey("main")) {
            var msg = new Message(Token.SOF, "no entrypoint, 'int main(int, char**)' was defined");
            declarations.add(new DeclarationSyntaxError(Token.SOF, new ParseException(msg)));
        }

        return new Unit(declarations, this.symbols);
    }

    public List<DeclarationFunc.Param> parseParamList() throws ParseException {
        var params = new ArrayList<DeclarationFunc.Param>();
        while (hasMoreTokens() && !match(RIGHT_PAREN)) {
            TypeExpr paramType = parseSimpleType();

            if (!match(LIT_IDENT)) {
                throw new ParseException(new Message(paramType.span(), "expected parameter name after type declarator"));
            }
            var paramName = consume();

            paramType = parseCompoundType(paramType, paramName);
            params.add(new DeclarationFunc.Param(paramType, paramName));

            if (!take(COMMA)) break;
        }

        if (!take(RIGHT_PAREN)) {
            try {
                parseSimpleType();
                var msg = new Message(peekToken(), "expected ')', did you forget a comma?");
                throw new ParseException(msg);
            } catch (ParseException ignored) {
            }

            var msg = new Message(peekToken(), "expected ')' after parameter list");
            throw new ParseException(msg);
        }

        return params;
    }

    public DeclarationTypedef parseDeclarationTypedef() throws ParseException {
        if (!take(KW_TYPEDEF)) {
            return null;
        }
        var start = lastToken();

        var type = parseSimpleType();

        if (!take(LIT_IDENT)) {
            throw new ParseException(new Message(type.span(), "expected typedef name after type declarator"));
        }
        var name = lastToken();
        if (symbols.containsKey(name.content())) {
            throw new ParseException(new Message(name, "the symbol '" + name.content() + "' was previously defined"));
        }

        type = parseCompoundType(type, name);

        if (!take(SEMICOLON)) {
            throw new ParseException(new Message(lastToken(), "expected ';' after typedef"));
        }

        var typedef = new DeclarationTypedef(start, type, name, type.end.end() < name.end() ? name : type.end);
        symbols.put(name.content(), new Symbol(name, typedef));
        return typedef;
    }

    Token bodyBrace = null;

    @NotNull
    public Declaration parseDeclaration() throws ParseException {
        var typedef = parseDeclarationTypedef();
        if (typedef != null) return typedef;

        TypeExpr type;
        final var t2 = peekToken2();
        if (t2.type().equals(EQUAL) || t2.type().equals(SEMICOLON) || t2.type().equals(LEFT_PAREN)) {
            type = new TypeExprInt(peekToken());
        } else {
            type = parseSimpleType();
        }

        if (!match(LIT_IDENT)) {
            throw new ParseException(new Message(type.span(), "expected declaration name after type expression"));
        }
        final Token name = consume();
        if (symbols.containsKey(name.content())) {
            throw new ParseException(new Message(name, "the symbol '" + name.content() + "' was previously defined"));
        }

        if (take(LEFT_PAREN)) {
            var lparen = lastToken();
            var params = parseParamList();
            if (params.size() > 4) {
                throw new ParseException(new Message(Span.of(lparen, lastToken()), "a function can have at most 4 parameters!"));
            }

            StatementBlock body = null;
            if (!match(LEFT_BRACE)) {
                if (!take(SEMICOLON)) {
                    throw new ParseException(new Message(lastToken(), "expected ';' after function declaration"));
                }
            } else {
                bodyBrace = peekToken();
                scope = new Vector<>();
                var paramScope = new LinkedHashMap<String, Symbol>();
                scope.add(paramScope);
                for (DeclarationFunc.Param param : params) {
                    if (!defineLocal(param.name, param.type.type())) {
                        throw new ParseException(new Message(name, "the parameter name '" + name.content() + "' was used twice!"));
                    }
                }
                body = parseStatementBlock();
                scope.removeLast();
                scope = null;
                bodyBrace = null;
            }

            var func = new DeclarationFunc(type, name, params, body, body == null ? lastToken() : body.end);
            symbols.put(name.content(), new Symbol(name, func.type()));
            return func;
        } else {
            type = parseCompoundType(type, name);

            Expression init = null;
            if (take(EQUAL)) {
                init = parseExpression();
            }

            if (!take(SEMICOLON)) {
                throw new ParseException(new Message(init == null ? Span.of(lastToken()) : init.span(), "expected ';' after variable declaration"));
            }

            var decl = new DeclarationVar(type, name, init);
            symbols.put(name.content(), new Symbol(name, type.type()));
            return decl;
        }
    }

    public TypeExpr parseCompoundType(TypeExpr simpleType, @Nullable Token ignored) throws ParseException {
        if (take(LEFT_BRACKET)) {
            if (!take(RIGHT_BRACKET)) {
                var msg = new Message(peekToken(), "expected '[]' in array type");
                throw new ParseException(msg);
            }
            return new TypeExprArray(simpleType, lastToken());
        } else {
            return simpleType;
        }
    }

    public TypeExpr parseSimpleType() throws ParseException {
        TypeExpr type = parseTypeName();
        while (match(STAR)) {
            final var tok = consume();
            type = new TypePointer(type, tok);
        }
        return type;
    }

    public boolean peekTypeName() {
        if (match(KW_INT, KW_CHAR)) return true;
        if (!match(LIT_IDENT)) return false;
        var ident = peekToken();
        if (!symbols.containsKey(ident.content())) return false;
        return symbols.get(ident.content()).typedef() != null;
    }

    @NotNull
    public TypeExpr parseTypeName() throws ParseException {
        if (take(KW_INT)) return new TypeExprInt(lastToken());
        if (take(KW_CHAR)) return new TypeExprChar(lastToken());
        if (take(KW_VOID)) return new TypeExprVoid(lastToken());

        if (!take(LIT_IDENT)) {
            throw new ParseException(new Message(peekToken(), "expected a simple type name"));
        }
        var tok = lastToken();
        if (symbols.containsKey(tok.content())) {
            var sym = symbols.get(tok.content());
            if (sym.typedef() != null) {
                return new TypeExprRef(tok, sym.typedef());
            } else {
                throw new ParseException(new Message(tok, tok.content() + " is not a type"));
            }
        } else {
            throw new ParseException(new Message(tok, "unknown type '" + tok.content() + "'"));
        }
    }

    public Statement parseStatement() throws ParseException {
        Token label = null;
        if (peekToken2().type().equals(COLON)) {
            if (!take(LIT_IDENT)) {
                throw new ParseException(new Message(peekToken(), "expected label!"));
            }
            label = lastToken();
            consume();
        }

        var blockStmt = parseStatementBlock();
        if (blockStmt != null) {
            blockStmt.setLabelAnchor(label);
            return blockStmt;
        }

        var ifStmt = parseStatementIf();
        if (ifStmt != null) {
            ifStmt.setLabelAnchor(label);
            return ifStmt;
        }

        var forStmt = parseStatementFor();
        if (forStmt != null) {
            forStmt.setLabelAnchor(label);
            return forStmt;
        }

        var whileStmt = parseStatementWhile();
        if (whileStmt != null) {
            whileStmt.setLabelAnchor(label);
            return whileStmt;
        }

        var doWhileStmt = parseStatementDoWhile();
        if (doWhileStmt != null) {
            if (!take(SEMICOLON))
                throw new ParseException(new Message(lastToken(), "expected ';' after do-while statement"));
            doWhileStmt.setLabelAnchor(label);
            return doWhileStmt;
        }

        var varStmt = parseStatementVar();
        if (varStmt != null) {
            if (!take(SEMICOLON))
                throw new ParseException(new Message(lastToken(), "expected ';' after variable statement"));
            varStmt.setLabelAnchor(label);
            return varStmt;
        }

        var gotoStmt = parseGotoStatement();
        if (gotoStmt != null) {
            if (!take(SEMICOLON))
                throw new ParseException(new Message(lastToken(), "expected ';' after goto statement"));
            gotoStmt.setLabelAnchor(label);
            return gotoStmt;
        }

        var continueStmt = parseStatementContinue();
        if (continueStmt != null) {
            if (!take(SEMICOLON))
                throw new ParseException(new Message(lastToken(), "expected ';' after continue statement"));
            continueStmt.setLabelAnchor(label);
            return continueStmt;
        }

        var breakStmt = parseStatementBreak();
        if (breakStmt != null) {
            if (!take(SEMICOLON))
                throw new ParseException(new Message(lastToken(), "expected ';' after break statement"));
            breakStmt.setLabelAnchor(label);
            return breakStmt;
        }

        var returnStmt = parseStatementReturn();
        if (returnStmt != null) {
            if (!take(SEMICOLON))
                throw new ParseException(new Message(lastToken(), "expected ';' after return statement"));
            returnStmt.setLabelAnchor(label);
            return returnStmt;
        }

        Expression expr = parseExpression();
        if (expr != null) {
            if (!take(SEMICOLON)) throw new ParseException(new Message(lastToken(), "expected ';' after expression"));
            var stmt = new StatementExpression(expr);
            stmt.setLabelAnchor(label);
            return stmt;
        }

        throw new ParseException(new Message(lastToken(), "expected statement"));
    }

    StatementIf parseStatementIf() throws ParseException {
        if (!take(KW_IF)) return null;
        var start = lastToken();

        if (!take(LEFT_PAREN)) throw new ParseException(new Message(lastToken(), "expected '(' after 'if'"));
        var cond = parseExpression();
        if (cond == null) throw new ParseException(new Message(lastToken(), "expected 'if' condition"));
        if (!take(RIGHT_PAREN)) throw new ParseException(new Message(lastToken(), "expected ')' after 'if'"));

        var body = parseStatement();

        Statement elseBody = null;
        if (take(KW_ELSE)) {
            elseBody = parseStatement();
        }

        return new StatementIf(start, cond, body, elseBody);
    }

    StatementFor parseStatementFor() throws ParseException {
        if (!take(KW_FOR)) return null;
        var start = lastToken();

        if (!take(LEFT_PAREN)) throw new ParseException(new Message(lastToken(), "expected '(' after 'for'"));

        scope.add(new LinkedHashMap<>());
        Expression initExpr = null;
        StatementVar initStmt = null;
        if (peekTypeName()) {
            initStmt = parseStatementVar();
        } else {
            initExpr = parseExpression();
        }

        if (!take(SEMICOLON)) {
            throw new ParseException(new Message(peekToken(), "expected ';' after for initializer"));
        }

        @Nullable
        Expression condition = parseExpression();

        if (!take(SEMICOLON)) throw new ParseException(new Message(peekToken(), "expected ';' after for condition"));
        Expression incr = parseExpression();

        if (!take(RIGHT_PAREN))
            throw new ParseException(new Message(peekToken(), "expected ')' after for-loop condition"));

        var body = parseStatement();

        scope.removeLast();
        return new StatementFor(start, initExpr, initStmt, condition, incr, body);
    }

    boolean defineLocal(Token token, SeaType type) {
        for (var frame : scope) {
            if (frame.containsKey(token.content())) {
                return false;
            }
        }

        scope.getLast().put(token.content(), new Symbol(token, type));
        return true;
    }

    Symbol resolveSymbol(String name) {
        for (var frame : scope) {
            if (frame.containsKey(name)) {
                return frame.get(name);
            }
        }
        if (symbols.containsKey(name)) {
            return symbols.get(name);
        }
        return null;
    }

    StatementVar parseStatementVar() throws ParseException {
        if (!peekTypeName()) return null;

        TypeExpr type = parseSimpleType();

        if (!take(LIT_IDENT)) throw new ParseException(new Message(peekToken(), "expected variable name"));
        Token name = lastToken();
        type = parseCompoundType(type, name);

        if (!defineLocal(name, type.type())) {
            throw new ParseException(new Message(name, "the symbol '" + name.content() + "' shadows a previously defined symbol"));
        }

        Expression value = null;
        if (take(EQUAL)) {
            value = parseExpression();
            if (value == null) {
                throw new ParseException(new Message(lastToken(), "expected initializer value after '='"));
            }
            if (!value.type().isConvertibleTo(type.type())) {
                value = new ExpressionTypeError(value, "cannot assign " + value.type().repr() + " to " + type.type().repr());
            }
        }

        return new StatementVar(type, name, value);
    }

    StatementWhile parseStatementWhile() throws ParseException {
        if (!take(KW_WHILE)) return null;
        var start = lastToken();

        if (!take(LEFT_PAREN)) throw new ParseException(new Message(lastToken(), "expected '(' after 'while'"));
        Expression condition = parseExpression();
        if (condition == null) throw new ParseException(new Message(lastToken(), "expected while-condition"));
        if (!take(RIGHT_PAREN)) throw new ParseException(new Message(lastToken(), "expected ')' after 'while'"));

        var body = parseStatement();
        return new StatementWhile(start, condition, body);
    }

    StatementDoWhile parseStatementDoWhile() throws ParseException {
        if (!take(KW_WHILE)) return null;
        var start = lastToken();

        var body = parseStatement();

        if (!take(LEFT_PAREN)) throw new ParseException(new Message(lastToken(), "expected '(' after do body"));
        Expression condition = parseExpression();
        if (condition == null) throw new ParseException(new Message(lastToken(), "expected do-while-condition"));
        if (!take(RIGHT_PAREN))
            throw new ParseException(new Message(lastToken(), "expected ')' after do-while condition"));

        return new StatementDoWhile(start, body, condition, lastToken());
    }

    StatementGoto parseGotoStatement() throws ParseException {
        if (!take(KW_GOTO)) return null;
        var start = lastToken();

        if (!take(LIT_IDENT)) throw new ParseException(new Message(peekToken(), "expected label name after 'goto'"));
        var labelName = lastToken();

        return new StatementGoto(start, labelName);
    }

    StatementBreak parseStatementBreak() {
        if (!take(KW_BREAK)) return null;
        return new StatementBreak(lastToken());
    }

    StatementReturn parseStatementReturn() {
        if (!take(KW_RETURN)) return null;
        var start = lastToken();

        Expression value = null;
        if (!match(SEMICOLON)) {
            value = parseExpression();
        }

        return new StatementReturn(start, value);
    }

    StatementContinue parseStatementContinue() {
        if (!take(KW_CONTINUE)) return null;
        return new StatementContinue(lastToken());
    }

    void recover(Token openingBrace) throws ParseException {
        var pos = index;
        index = tokens.indexOf(openingBrace) + 1;
        int open = 1;
        while (hasMoreTokens() && open > 0 && index < pos) {
            if (take(LEFT_BRACE)) open++;
            else if (take(RIGHT_BRACE)) open--;
            else consume();
        }

        // we want to consume all open braces from other blocks
        index = pos;
        while (open > 1) {
            if (take(LEFT_BRACE)) open++;
            else if (take(RIGHT_BRACE)) open--;
            else consume();
        }

        while (hasMoreTokens()) {
            if (match(RIGHT_BRACE, SEMICOLON)) return;
            if (match(KW_IF, KW_FOR, KW_DO, KW_WHILE, KW_CONTINUE, KW_BREAK, KW_RETURN, KW_GOTO)) return;
        }
    }

    @Nullable
    public StatementBlock parseStatementBlock() throws ParseException {
        if (!take(LEFT_BRACE)) return null;
        var start = lastToken();

        scope.add(new LinkedHashMap<>());
        var stmts = new ArrayList<Statement>();
        while (hasMoreTokens() && !match(RIGHT_BRACE)) {
            var before = peekToken();
            try {
                var stmt = parseStatement();
                stmts.add(stmt);
            } catch (ParseException e) {
                var stmt = new StatementSyntaxError(before, e);
                stmts.add(stmt);
                recover(start);
                if (peekToken() == before) break;
            }
        }
        scope.removeLast();

        if (!take(RIGHT_BRACE)) {
            var msg = new Message(lastToken(), "expected '}' after block statement");
            throw new ParseException(msg);
        }

        return new StatementBlock(start, stmts, lastToken());
    }

    public Expression parseExpression() {
        return parseAssignExpression();
    }

    public Expression parseAssignExpression() {
        var expr = parseTernaryExpression();
        if (expr == null) return null;

        Token.Type[] types = { EQUAL, STAR_EQ, SLASH_EQ, PERCENT_EQ, PLUS_EQ, DASH_EQ, LEFT_ARROW2_EQ, RIGHT_ARROW2_EQ,
                AMPERSAND_EQ, BAR_EQ };

        if (take(types)) {
            var op = lastToken();
            var rhs = parseTernaryExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(peekToken(), "expected right hand side of assignment expression");
            }

            if (expr.valueKind() != Expression.ValueKind.LValue) {
                expr = new ExpressionTypeError(expr, "cannot assign to rvalue");
            }
            if (!rhs.type().isConvertibleTo(expr.type())) {
                rhs = new ExpressionTypeError(rhs, "cannot assign " + rhs.type().repr() + " to " + expr.type().repr());
            }

            expr = new ExpressionBin(expr, op, rhs, SeaType.VOID);
        }

        if (match(types)) {
            expr = new ExpressionSyntaxError(expr, peekToken(), "chained assignments are not allowed");
        }

        return expr;
    }

    public Expression parseTernaryExpression() {
        var expr = parseOrExpression();
        if (expr == null) return null;

        if (take(QUESTION)) {
            var then = parseTernaryExpression();
            if (then == null) {
                then = new ExpressionSyntaxError(lastToken(), "expected then-expression after ternary '?'");
                consumeTo(COLON);
            }

            if (!take(COLON)) {
                expr = new ExpressionSyntaxError(lastToken(), "expected ':' in ternary value");
            }

            var otherwise = parseTernaryExpression();
            if (otherwise == null) {
                otherwise = new ExpressionSyntaxError(lastToken(), "expected else-expression after ternary ':'");
            }

            var condType = expr.type();
            if (!condType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot interpret " + condType.repr() + " as a boolean");
            }

            var thenType = then.type();
            var otherType = otherwise.type();
            if (!otherType.isConvertibleTo(thenType)) {
                otherwise = new ExpressionTypeError(otherwise, "the ternary branches disagree, " + otherType.repr() + " is not assignable to " + then.type());
            }

            expr = new ExpressionTernary(expr, then, otherwise, thenType);
        }

        return expr;
    }

    public Expression parseOrExpression() {
        var expr = parseAndExpression();
        if (expr == null) return null;

        while (take(BAR2)) {
            var op = lastToken();
            var rhs = parseAndExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of logical-or expression");
            }

            var lhsType = expr.type();
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot logical or the type " + lhsType.repr());
            }

            var rhsType = rhs.type();
            if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot logical or the type " + rhsType.repr());
            }
            expr = new ExpressionBin(expr, op, rhs, SeaType.INT);
        }

        return expr;
    }

    public Expression parseAndExpression() {
        var expr = parseBinaryOrExpression();
        if (expr == null) return null;

        while (take(AMPERSAND2)) {
            var op = lastToken();
            var rhs = parseBinaryOrExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of logical-and expression");
            }

            var lhsType = expr.type();
            var rhsType = rhs.type();
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot logical and the type " + lhsType.repr());
            } else if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot logical and the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs, SeaType.INT);
        }

        return expr;
    }

    public Expression parseBinaryOrExpression() {
        var expr = parseBinaryXOrExpression();
        if (expr == null) return null;

        while (take(BAR)) {
            var op = lastToken();
            var rhs = parseBinaryXOrExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of bitwise-or expression");
            }

            var lhsType = expr.type();
            var rhsType = rhs.type();
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot bitwise-or the type " + lhsType.repr());
            } else if (!rhsType.isArithmetic()) {
                rhs = new ExpressionTypeError(rhs, "cannot bitwise-or the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs, SeaType.INT);
        }

        return expr;
    }

    public Expression parseBinaryXOrExpression() {
        var expr = parseBinaryAndExpression();
        if (expr == null) return null;

        while (take(CARET)) {
            var op = lastToken();
            var rhs = parseBinaryAndExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of bitwise-xor expression");
            }

            var lhsType = expr.type();
            var rhsType = rhs.type();
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot bitwise-xor the type " + lhsType.repr());
            } else if (!rhsType.isArithmetic()) {
                rhs = new ExpressionTypeError(rhs, "cannot bitwise-xor the type " + rhsType.repr());
            }

            SeaType type = SeaType.INT;
            expr = new ExpressionBin(expr, op, rhs, type);
        }

        return expr;
    }

    public Expression parseBinaryAndExpression() {
        var expr = parseEqualityExpression();
        if (expr == null) return null;

        while (take(AMPERSAND)) {
            var op = lastToken();
            var rhs = parseEqualityExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of bitwise-and expression");
            }

            var lhsType = expr.type();
            var rhsType = rhs.type();
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot bitwise-and the type " + lhsType.repr());
            } else if (!rhsType.isArithmetic()) {
                rhs = new ExpressionTypeError(rhs, "cannot bitwise-and the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs, SeaType.INT);
        }

        return expr;
    }

    public Expression parseEqualityExpression() {
        var expr = parseComparisonExpression();
        if (expr == null) return null;

        while (take(EQUAL2, BANG_EQ)) {
            var op = lastToken();
            var rhs = parseComparisonExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of equality expression");
            }

            var lhsType = expr.type();
            var rhsType = rhs.type();
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot compare the type " + lhsType.repr());
            } else if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot compare the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs, SeaType.INT);
        }

        return expr;
    }

    public Expression parseComparisonExpression() {
        var expr = parseShiftExpression();
        if (expr == null) return null;

        while (take(LEFT_ARROW, LEFT_ARROW_EQ, RIGHT_ARROW, RIGHT_ARROW_EQ)) {
            var op = lastToken();
            var rhs = parseShiftExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of comparison expression");
            }

            var lhsType = expr.type();
            var rhsType = rhs.type();
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot compare the type " + lhsType.repr());
            } else if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot compare the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs, SeaType.INT);
        }

        return expr;
    }

    public Expression parseShiftExpression() {
        var expr = parseAdditiveExpression();
        if (expr == null) return null;

        while (take(LEFT_ARROW2, RIGHT_ARROW2)) {
            var op = lastToken();
            var rhs = parseAdditiveExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of shift expression");
            }

            String term = switch (op.content()) {
                case "<<" -> "left-shift";
                case ">>" -> "right-shift";
                default -> throw new UnsupportedOperationException();
            };

            var lhsType = expr.type();
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot " + term + " the type " + lhsType.repr());
            }

            var rhsType = rhs.type();
            if (!rhsType.isArithmetic()) {
                rhs = new ExpressionTypeError(rhs, "cannot " + term + " the type " + rhsType.repr());
            }
            expr = new ExpressionBin(expr, op, rhs, arithmetic(lhsType, rhsType));
        }

        return expr;
    }

    SeaType arithmetic(SeaType lhs, SeaType rhs) {
        if (lhs.equals(rhs)) return lhs;
        if (lhs.isInt() || rhs.isInt()) return SeaType.INT;
        return lhs;
    }

    public Expression parseAdditiveExpression() {
        var expr = parseMultiplicativeExpression();
        if (expr == null) return null;

        while (take(PLUS, DASH)) {
            var op = lastToken();
            var rhs = parseMultiplicativeExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of additive expression");
            }

            String term = switch (op.content()) {
                case "+" -> "add";
                case "-" -> "subtract";
                default -> throw new IllegalStateException("impossible");
            };

            var lhsType = expr.type();
            var rhsType = rhs.type();

            SeaType type;
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot " + term + " " + lhsType.repr() + " with " + rhsType.repr());
                type = SeaType.INT;
            } else if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot " + term + " " + lhsType.repr() + " with " + rhsType.repr());
                type = SeaType.INT;
            } else if (lhsType.isAPointer() && rhsType.isAPointer()) {
                if (!op.content().equals("-")) {
                    expr = new ExpressionTypeError(expr, "cannot " + term + " " + lhsType.repr() + " with " + rhsType.repr());
                }
                type = SeaType.INT;
            } else if (lhsType.isArithmetic() && rhsType.isArithmetic()) {
                type = arithmetic(lhsType, rhsType);
            } else if (lhsType.isAPointer() && rhsType.isArithmetic()) {
                type = lhsType;
            } else if (rhsType.isAPointer() && lhsType.isArithmetic()) {
                type = rhsType;
            } else {
                throw new IllegalStateException("unreachable?");
            }

            expr = new ExpressionBin(expr, op, rhs, type);
        }

        return expr;
    }

    public Expression parseMultiplicativeExpression() {
        var expr = parseCastExpression();
        if (expr == null) return null;

        while (take(STAR, SLASH, PERCENT)) {
            var op = lastToken();
            var rhs = parseCastExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(lastToken(), "expected right hand side of multiplicative expression");
            }

            String term = switch (op.content()) {
                case "*" -> "product";
                case "/" -> "division";
                case "%" -> "remainder";
                default -> throw new UnsupportedOperationException();
            };

            var lhsType = expr.type();
            var rhsType = rhs.type();
            SeaType type;
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot take the " + term + " of " + lhsType.repr() + " and " + rhsType.repr());
                type = SeaType.INT;
            } else if (!rhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot take the " + term + " of " + lhsType.repr() + " and " + rhsType.repr());
                type = SeaType.INT;
            } else {
                type = arithmetic(lhsType, rhsType);
            }

            expr = new ExpressionBin(expr, op, rhs, type);
        }

        return expr;
    }

    public Expression parseCastExpression() {
        var start = index;
        if (!take(LEFT_PAREN)) return parsePrefixExpression();
        var startToken = lastToken();

        try {
            var type = parseSimpleType();
            if (!take(RIGHT_PAREN)) {
                return new ExpressionSyntaxError(lastToken(), "expected type name after cast parens");
            }

            var exprValue = parsePrefixExpression();
            if (exprValue == null) {
                return new ExpressionSyntaxError(lastToken(), "expected expression after cast");
            }

            if (!exprValue.type().isCastableTo(type.type())) {
                exprValue = new ExpressionTypeError(exprValue, "cannot cast " + exprValue.type().repr() + " to " + type.type().repr());
            }

            return new ExpressionCast(startToken, type, exprValue);
        } catch (ParseException e) {
            index = start;
            return parsePrefixExpression();
        }
    }

    public Expression parsePrefixExpression() {
        var stack = new Vector<Token>();
        while (match(PLUS2, DASH2, AMPERSAND, STAR, PLUS, DASH, TILDE, BANG, KW_SIZEOF)) {
            stack.addLast(consume());
        }

        var expr = parsePostfixExpression();
        if (expr == null) {
            if (stack.isEmpty()) return null;
            return new ExpressionSyntaxError(lastToken(), "expected expression after unary operator");
        }
        while (!stack.isEmpty()) {
            var op = stack.removeLast();
            var exprTy = expr.type();
            var type = switch (op.content()) {
                case "++", "--" -> {
                    if (!exprTy.isIntegral()) {
                        expr = new ExpressionTypeError(expr, "prefix operator '" + op.content() +
                                "' is undefined on the type " + exprTy.repr());
                        yield SeaType.INT;
                    }
                    if (expr.valueKind() != Expression.ValueKind.LValue) {
                        expr = new ExpressionTypeError(expr, "cannot modify rvalue");
                    }
                    yield exprTy;
                }
                case "-", "~", "!" -> {
                    if (!exprTy.isArithmetic()) {
                        expr = new ExpressionTypeError(expr, "prefix operator '" + op.content() +
                                "' is undefined on the type " + exprTy.repr());
                        yield SeaType.INT;
                    }

                    if (op.content().equals("!")) {
                        yield SeaType.INT;
                    } else {
                        yield exprTy;
                    }
                }
                case "*" -> {
                    if (!exprTy.isAPointer()) {
                        expr = new ExpressionTypeError(expr, "cannot deference the type " + exprTy.repr());
                        yield SeaType.INT;
                    }
                    yield exprTy.componentType();
                }
                case "&" -> {
                    if (expr.valueKind() != Expression.ValueKind.LValue) {
                        expr = new ExpressionTypeError(expr, "cannot reference non-lvalue");
                    }
                    yield new SeaType.Pointer(expr.type());
                }
                default -> throw new IllegalStateException("Unexpected value: " + op.content());
            };
            expr = new ExpressionPrefix(op, expr, type);
        }

        return expr;
    }

    public Expression parsePostfixExpression() {
        var expr = parsePrimaryExpression();
        if (expr == null) return null;

        while (hasMoreTokens()) {
            if (take(PLUS2, DASH2)) {
                var op = lastToken();
                var exprType = expr.type();
                if (!exprType.isIntegral()) {
                    expr = new ExpressionTypeError(expr, "cannot increment non-integer type " + exprType.repr());
                }
                if (expr.valueKind() != Expression.ValueKind.LValue) {
                    expr = new ExpressionTypeError(expr, "cannot modify rvalue");
                }

                expr = new ExpressionPostfix(expr, op, exprType);
            } else if (take(DOT, ARROW)) {
                var access = lastToken();
                if (!take(LIT_IDENT)) {
                    return new ExpressionSyntaxError(lastToken(), "expected property name after accessor");
                }
                // TODO: access
                var prop = new ExpressionIdent(lastToken(), SeaType.INT);

                var parentTy = expr.type();
                if (!(parentTy instanceof SeaType.Struct(String ignored, Map<String, SeaType> fields))) {
                    expr = new ExpressionTypeError(expr, "cannot access non-structure types");
                } else if (!fields.containsKey(prop.name())) {
                    expr = new ExpressionTypeError(prop, "the struct " + parentTy.repr() + " has no field '" + prop.name() + "'");
                }
                // TODO: fields
                var type = SeaType.INT;

                expr = new ExpressionAccess(expr, access, prop, type);
            } else if (take(LEFT_PAREN)) {
                var args = new ArrayList<Expression>();
                while (hasMoreTokens() && !match(RIGHT_PAREN)) {
                    var arg = parseExpression();
                    if (arg instanceof ExpressionSyntaxError) {
                        consumeTo(COMMA, RIGHT_PAREN);
                    } else if (arg == null) {
                        arg = new ExpressionSyntaxError(lastToken(), "expected call argument");
                    }
                    args.add(arg);

                    if (!take(COMMA)) break;
                }

                if (!take(RIGHT_PAREN)) {
                    var trailing = parseExpression();
                    if (trailing instanceof ExpressionSyntaxError) {
                        return new ExpressionSyntaxError(trailing.start, "expected ')', did you forget a comma?");
                    } else {
                        return new ExpressionSyntaxError(lastToken(), "expected ')' after function call arguments");
                    }
                }

                SeaType type;
                var functorTy = expr.type();
                if (!(functorTy instanceof SeaType.Func(List<SeaType> params, SeaType result))) {
                    expr = new ExpressionTypeError(expr, "cannot invoke " + functorTy.repr());
                    type = SeaType.INT;
                } else if (args.size() != params.size()) {
                    var s = new StringBuilder();
                    s.append("argument mismatch, expected (");
                    for (int i = 0; i < params.size(); i++) {
                        if (i > 0) s.append(", ");
                        s.append(params.get(i).repr());
                    }
                    s.append("), instead found (");
                    for (int i = 0; i < args.size(); i++) {
                        if (i > 0) s.append(", ");
                        s.append(args.get(i).type().repr());
                    }
                    s.append(")");
                    expr = new ExpressionTypeError(expr, s.toString());
                    type = result;
                } else {
                    for (int i = 0; i < params.size(); i++) {
                        var paramTy = params.get(i);
                        var argTy = args.get(i).type();
                        if (argTy.isConvertibleTo(paramTy)) {
                            continue;
                        }

                        String s = "argument of type " + argTy.repr() + " is not convertible to " + paramTy.repr();
                        args.set(i, new ExpressionTypeError(args.get(i), s));
                    }
                    type = result;
                }

                expr = new ExpressionCall(expr, args, lastToken(), type);
            } else if (take(LEFT_BRACKET)) {
                var index = parseExpression();
                if (index == null) {
                    return new ExpressionSyntaxError(lastToken(), "expected index after '['");
                }
                if (!take(RIGHT_BRACKET)) {
                    return new ExpressionSyntaxError(lastToken(), "expected ']' after array index");
                }
                if (!index.type().isArithmetic()) {
                    expr = new ExpressionTypeError(index, "index must be an integral type");
                }

                SeaType resultType;
                var arrayType = expr.type();
                if (!arrayType.isAPointer()) {
                    expr = new ExpressionTypeError(expr, "cannot index " + arrayType.repr());
                    resultType = SeaType.INT;
                } else if (arrayType.componentType().isVoid()) {
                    expr = new ExpressionTypeError(expr, "cannot index void*, it has no size");
                    resultType = arrayType.componentType();
                } else {
                    resultType = arrayType.componentType();
                }
                expr = new ExpressionIndex(expr, index, lastToken(), resultType);
            } else {
                break;
            }
        }

        return expr;
    }

    public Expression parsePrimaryExpression() {
        if (match(LIT_INT)) {
            return new ExpressionInteger(consume());
        } else if (match(LIT_STR)) {
            return new ExpressionString(consume());
        } else if (match(LIT_CHAR)) {
            return new ExpressionChar(consume());
        } else if (match(LIT_IDENT)) {
            var ident = consume();
            var sym = resolveSymbol(ident.content());

            if (sym == null || sym.type() == null) {
                var expr = new ExpressionIdent(ident, SeaType.INT);
                return new ExpressionTypeError(expr, "undefined symbol '" + ident.content() + "'");
            } else {
                return new ExpressionIdent(ident, sym.type());
            }
        } else if (take(LEFT_PAREN)) {
            var start = lastToken();
            var inner = parseExpression();
            if (inner == null) return new ExpressionSyntaxError(lastToken(), "expected expression after '('");
            if (!take(RIGHT_PAREN))
                return new ExpressionSyntaxError(lastToken(), "expected ')' after grouped expression");
            return new ExpressionParens(start, inner, lastToken());
        } else if (match(LEFT_BRACE)) {
            throw new RuntimeException("unimplemented: INITIALIZER_LIST");
        } else {
            return null;
        }
    }

    // =================================
    // UTILITIES BEYOND HERE LIE
    // =================================

    protected boolean hasMoreTokens() {
        return index < tokens.size();
    }

    protected boolean match(@NotNull Token.Type... types) {
        var token = peekToken();
        for (var type : types) {
            if (token.type().equals(type)) return true;
        }
        return false;
    }

    protected boolean take(@NotNull Token.Type... types) {
        for (Token.Type type : types) {
            if (match(type)) {
                consume();
                return true;
            }
        }
        return false;
    }

    protected Token consume() {
        if (index >= tokens.size()) {
            return Token.EOF;
        }
        var token = tokens.get(index);
        index += 1;
        return token;
    }

    protected void consumeThrough(@NotNull Token.Type... types) {
        while (hasMoreTokens()) {
            var token = consume();
            if (Arrays.stream(types).anyMatch(t -> t.equals(token.type()))) {
                return;
            }
            index += 1;
        }
    }


    protected void consumeTo(@NotNull Token.Type... types) {
        while (hasMoreTokens()) {
            var token = peekToken();
            if (Arrays.stream(types).anyMatch(t -> t.equals(token.type()))) {
                return;
            }
            index += 1;
        }
    }

    protected Token lastToken() {
        if (index == 0) return Token.SOF;
        return tokens.get(index - 1);
    }

    protected Token peekToken() {
        if (index >= tokens.size()) return Token.EOF;
        return tokens.get(index);
    }

    protected Token peekToken2() {
        if (index + 1 >= tokens.size()) return Token.EOF;
        return tokens.get(index + 1);
    }

    void consumeMatchingBrace(Token openingBrace) throws ParseException {
        assert openingBrace.type().equals(LEFT_BRACE);
        index = tokens.indexOf(openingBrace) + 1;
        var open = 1;
        while (hasMoreTokens() && open > 0) {
            if (take(LEFT_BRACE)) open++;
            else if (take(RIGHT_BRACE)) open--;
            else consume();
        }
        if (open > 0) {
            var msg = new Message(openingBrace, "unterminated '{'");
            throw new ParseException(msg);
        }
    }

    public List<Token> remainingTokens() {
        return tokens.subList(index, tokens.size());
    }
}
