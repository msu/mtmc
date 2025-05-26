package mtmc.lang.sea;

import mtmc.lang.sea.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static mtmc.lang.sea.Token.Type.*;

public class SeaParser {
    private final List<Token> tokens;
    private int index = 0;

    private final LinkedHashMap<String, Object> symbols;
    private Vector<LinkedHashMap<String, Token>> scope = new Vector<>();

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
                declarations.add(new DeclarationSyntaxError(start, e.getMessage()));
                if (bodyBrace != null) {
                    consumeMatchingBrace(bodyBrace);
                } else {
                    consumeThrough(SEMICOLON);
                }
            }

            if (peekToken().equals(start)) {
                throw new ParseException(start.span(), "INFINITE LOOP DETECTED");
            }
        }

        if (!symbols.containsKey("main")) {
            declarations.add(new DeclarationSyntaxError(Token.SOF, "no main function defined"));
        }

        return new Unit(declarations, this.symbols);
    }

    public List<DeclarationFunc.Param> parseParamList() throws ParseException {
        var params = new ArrayList<DeclarationFunc.Param>();
        while (hasMoreTokens() && !match(RIGHT_PAREN)) {
            TypeExpr paramType = parseSimpleType();

            if (!match(LIT_IDENT)) {
                throw new ParseException(peekToken().span(), "expected parameter name");
            }
            var paramName = consume();

            paramType = parseCompoundType(paramType, paramName);
            params.add(new DeclarationFunc.Param(paramType, paramName));

            if (!take(COMMA)) break;
        }

        if (!take(RIGHT_PAREN)) {
            try {
                parseSimpleType();
                throw new ParseException(lastToken().endSpan(), "expected ')', did you forget a comma?");
            } catch (ParseException ignored) {
            }

            throw new ParseException(lastToken().endSpan(), "expected ')' after parameter list");
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
            throw new ParseException(peekToken().span(), "expected new type name after");
        }
        var name = lastToken();
        if (symbols.containsKey(name.content())) {
            throw new ParseException(name.span(), "the symbol '" + name.content() + "' was previously defined");
        }

        type = parseCompoundType(type, name);

        if (!take(SEMICOLON)) {
            throw new ParseException(lastToken().endSpan(), "expected ';' after typedef");
        }

        var typedef = new DeclarationTypedef(start, type, name, type.end.end() < name.end() ? name : type.end);
        symbols.put(name.content(), typedef);
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
            throw new ParseException(peekToken().span(), "expected declaration name");
        }
        final Token name = consume();
        if (symbols.containsKey(name.content())) {
            throw new ParseException(name.span(), "the symbol '" + name.content() + "' was previously defined");
        }

        if (take(LEFT_PAREN)) {
            var lparen = lastToken();
            var params = parseParamList();
            if (params.size() > 4) {
                throw new ParseException(spanOf(lparen, lastToken()), "a function can have at most 4 parameters!");
            }

            StatementBlock body = null;
            if (!match(LEFT_BRACE)) {
                if (!take(SEMICOLON)) {
                    throw new ParseException(lastToken().span(), "expected ';' after function declaration");
                }
            } else {
                bodyBrace = peekToken();
                scope = new Vector<>();
                body = parseStatementBlock();
                scope = null;
                bodyBrace = null;
            }

            var func = new DeclarationFunc(type, name, params, body, body == null ? lastToken() : body.end);
            symbols.put(name.content(), func);
            return func;
        } else {
            type = parseCompoundType(type, name);

            Expression init = null;
            if (take(EQUAL)) {
                init = parseExpression();
            }

            if (!take(SEMICOLON)) {
                throw new ParseException(lastToken().span(), "expected ';' after variable declaration");
            }

            var decl = new DeclarationVar(type, name, init);
            symbols.put(name.content(), decl);
            return decl;
        }
    }

    public TypeExpr parseCompoundType(TypeExpr simpleType, @Nullable Token ignored) throws ParseException {
        if (take(LEFT_BRACKET)) {
            if (!take(RIGHT_BRACKET)) {
                throw new ParseException(peekToken().span(), "expected ']' after '[' in compound-type");
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
        return symbols.get(ident.content()) instanceof TypeDeclaration;
    }

    @NotNull
    public TypeExpr parseTypeName() throws ParseException {
        if (take(KW_INT)) return new TypeExprInt(lastToken());
        if (take(KW_CHAR)) return new TypeExprChar(lastToken());

        if (!take(LIT_IDENT)) {
            throw new ParseException(peekToken().span(), "expected simple type name");
        }
        var tok = lastToken();
        if (symbols.containsKey(tok.content())) {
            if (symbols.get(tok.content()) instanceof TypeDeclaration decl) {
                return new TypeExprRef(tok, decl);
            } else {
                throw new ParseException(tok.span(), tok.content() + " is not a type");
            }
        } else {
            throw new ParseException(tok.span(), "unknown type '" + tok.content() + "'");
        }
    }

    public Statement parseStatement() throws ParseException {
        Token label = null;
        if (peekToken2().type().equals(COLON)) {
            if (!take(LIT_IDENT)) {
                throw new ParseException(peekToken().span(), "expected label!");
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
                throw new ParseException(lastToken().endSpan(), "expected ';' after do-while statement");
            doWhileStmt.setLabelAnchor(label);
            return doWhileStmt;
        }

        var varStmt = parseStatementVar();
        if (varStmt != null) {
            if (!take(SEMICOLON))
                throw new ParseException(lastToken().endSpan(), "expected ';' after variable statement");
            varStmt.setLabelAnchor(label);
            return varStmt;
        }

        var gotoStmt = parseGotoStatement();
        if (gotoStmt != null) {
            if (!take(SEMICOLON)) throw new ParseException(lastToken().endSpan(), "expected ';' after goto statement");
            gotoStmt.setLabelAnchor(label);
            return gotoStmt;
        }

        var continueStmt = parseStatementContinue();
        if (continueStmt != null) {
            if (!take(SEMICOLON))
                throw new ParseException(lastToken().endSpan(), "expected ';' after continue statement");
            continueStmt.setLabelAnchor(label);
            return continueStmt;
        }

        var breakStmt = parseStatementBreak();
        if (breakStmt != null) {
            if (!take(SEMICOLON)) throw new ParseException(lastToken().endSpan(), "expected ';' after break statement");
            breakStmt.setLabelAnchor(label);
            return breakStmt;
        }

        var returnStmt = parseStatementReturn();
        if (returnStmt != null) {
            if (!take(SEMICOLON))
                throw new ParseException(lastToken().endSpan(), "expected ';' after return statement");
            returnStmt.setLabelAnchor(label);
            return returnStmt;
        }

        Expression expr = parseExpression();
        if (expr != null) {
            if (!take(SEMICOLON)) throw new ParseException(lastToken().endSpan(), "expected ';' after expression");
            var stmt = new StatementExpression(expr);
            stmt.setLabelAnchor(label);
            return stmt;
        }

        throw new ParseException(lastToken().span(), "expected statement");
    }

    StatementIf parseStatementIf() throws ParseException {
        if (!take(KW_IF)) return null;
        var start = lastToken();

        if (!take(LEFT_PAREN)) throw new ParseException(lastToken().span(), "expected '(' after 'if'");
        var cond = parseExpression();
        if (cond == null) throw new ParseException(lastToken().endSpan(), "expected 'if' condition");
        if (!take(RIGHT_PAREN)) throw new ParseException(lastToken().endSpan(), "expected ')' after 'if'");

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

        if (!take(LEFT_PAREN)) throw new ParseException(lastToken().span(), "expected '(' after 'for'");

        scope.add(new LinkedHashMap<>());
        Expression initExpr = null;
        StatementVar initStmt = null;
        if (peekTypeName()) {
            initStmt = parseStatementVar();
        } else {
            initExpr = parseExpression();
        }

        if (!take(SEMICOLON)) throw new ParseException(lastToken().span(), "expected ';' after for initializer");

        @Nullable
        Expression condition = parseExpression();

        if (!take(SEMICOLON)) throw new ParseException(lastToken().span(), "expected ';' after for condition");
        Expression incr = parseExpression();

        if (!take(RIGHT_PAREN))
            throw new ParseException(lastToken().endSpan(), "expected ')' after for-loop condition");

        var body = parseStatement();

        scope.removeLast();
        return new StatementFor(start, initExpr, initStmt, condition, incr, body);
    }

    boolean defineLocal(Token token) {
        for (var frame : scope) {
            if (frame.containsKey(token.content())) {
                return false;
            }
        }

        scope.getLast().put(token.content(), token);
        return true;
    }

    StatementVar parseStatementVar() throws ParseException {
        if (!peekTypeName()) return null;

        TypeExpr type = parseSimpleType();

        if (!take(LIT_IDENT)) throw new ParseException(peekToken().span(), "expected variable name");
        Token name = lastToken();
        if (!defineLocal(name)) {
            throw new ParseException(name.span(), "the symbol '" + name.content() + "' shadows a previously defined symbol");
        }

        type = parseCompoundType(type, name);

        Expression value = null;
        if (take(EQUAL)) {
            value = parseExpression();
            if (value == null) {
                throw new ParseException(lastToken().span(), "expected initializer value after '='");
            }
        }

        return new StatementVar(type, name, value);
    }

    StatementWhile parseStatementWhile() throws ParseException {
        if (!take(KW_WHILE)) return null;
        var start = lastToken();

        if (!take(LEFT_PAREN)) throw new ParseException(lastToken().span(), "expected '(' after 'while'");
        Expression condition = parseExpression();
        if (condition == null) throw new ParseException(lastToken().span(), "expected while-condition");
        if (!take(RIGHT_PAREN)) throw new ParseException(lastToken().endSpan(), "expected ')' after 'while'");

        var body = parseStatement();
        return new StatementWhile(start, condition, body);
    }

    StatementDoWhile parseStatementDoWhile() throws ParseException {
        if (!take(KW_WHILE)) return null;
        var start = lastToken();

        var body = parseStatement();

        if (!take(LEFT_PAREN)) throw new ParseException(lastToken().span(), "expected '(' after do body");
        Expression condition = parseExpression();
        if (condition == null) throw new ParseException(lastToken().span(), "expected do-while-condition");
        if (!take(RIGHT_PAREN))
            throw new ParseException(lastToken().endSpan(), "expected ')' after do-while condition");

        return new StatementDoWhile(start, body, condition, lastToken());
    }

    StatementGoto parseGotoStatement() throws ParseException {
        if (!take(KW_GOTO)) return null;
        var start = lastToken();

        if (!take(LIT_IDENT)) throw new ParseException(start.span(), "expected label name after 'goto'");
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
                var stmt = new StatementSyntaxError(before, e.getMessage());
                stmts.add(stmt);
                recover(start);
                if (peekToken() == before) break;
            }
        }
        scope.removeLast();

        if (!take(RIGHT_BRACE)) {
            throw new ParseException(lastToken().span(), "expected '}' after block statement");
        }

        return new StatementBlock(start, stmts, lastToken());
    }

    public Expression parseExpression() {
        return parseAssignExpression();
    }

    public Expression parseAssignExpression() {
        var expr = parseTernaryExpression();
        if (expr == null) return null;

        if (take(EQUAL, STAR_EQ, SLASH_EQ, PERCENT_EQ, PLUS_EQ, DASH_EQ, LEFT_ARROW2_EQ, RIGHT_ARROW2_EQ,
                AMPERSAND_EQ, BAR_EQ)) {
            var op = lastToken();
            var rhs = parseTernaryExpression();
            if (rhs == null) {
                rhs = new ExpressionSyntaxError(peekToken(), "expected right hand side of assignment expression");
            }

            // TODO: symbol checking & lvalue analysis

            expr = new ExpressionBin(expr, op, rhs);
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

            var condType = expr.getType();
            if (!condType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot interpret " + condType.repr() + " as a boolean");
            }

            var thenType = then.getType();
            var otherType = otherwise.getType();
            // TODO: maybe equality here?
            if (!otherType.isConvertibleTo(thenType)) {
                otherwise = new ExpressionTypeError(otherwise, "the ternary branches disagree, " + otherType.repr() + " is not assignable to " + then.getType());
            }

            expr = new ExpressionTernary(expr, then, otherwise);
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

            var lhsType = expr.getType();
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot logical or the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot logical or the type " + rhsType.repr());
            }
            expr = new ExpressionBin(expr, op, rhs);
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

            var lhsType = expr.getType();
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot logical and the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot logical and the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs);
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

            var lhsType = expr.getType();
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot bitwise-or the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isArithmetic()) {
                rhs = new ExpressionTypeError(rhs, "cannot bitwise-or the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs);
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

            var lhsType = expr.getType();
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot bitwise-xor the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isArithmetic()) {
                rhs = new ExpressionTypeError(rhs, "cannot bitwise-xor the type " + rhsType.repr());
            }
            expr = new ExpressionBin(expr, op, rhs);
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

            var lhsType = expr.getType();
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot bitwise-and the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isArithmetic()) {
                rhs = new ExpressionTypeError(rhs, "cannot bitwise-and the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs);
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

            var lhsType = expr.getType();
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot compare the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot compare the type " + rhsType.repr());
            }
            expr = new ExpressionBin(expr, op, rhs);
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

            var lhsType = expr.getType();
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot compare the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot compare the type " + rhsType.repr());
            }
            expr = new ExpressionBin(expr, op, rhs);
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

            var lhsType = expr.getType();
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot " + term + " the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isArithmetic()) {
                rhs = new ExpressionTypeError(rhs, "cannot " + term + " the type " + rhsType.repr());
            }
            expr = new ExpressionBin(expr, op, rhs);
        }

        return expr;
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
                default -> throw new UnsupportedOperationException();
            };

            var lhsType = expr.getType();
            if (!lhsType.isIntegral()) {
                expr = new ExpressionTypeError(expr, "cannot " + term + " the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isIntegral()) {
                rhs = new ExpressionTypeError(rhs, "cannot " + term + " the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs);
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
                case "*" -> "multiply";
                case "/" -> "divide";
                case "%" -> "take the remainder of";
                default -> throw new UnsupportedOperationException();
            };

            var lhsType = expr.getType();
            if (!lhsType.isArithmetic()) {
                expr = new ExpressionTypeError(expr, "cannot " + term + " the type " + lhsType.repr());
            }

            var rhsType = rhs.getType();
            if (!rhsType.isArithmetic()) {
                rhs = new ExpressionTypeError(rhs, "cannot " + term + " the type " + rhsType.repr());
            }

            expr = new ExpressionBin(expr, op, rhs);
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

            var expr = parsePrefixExpression();
            if (expr == null) {
                return new ExpressionSyntaxError(lastToken(), "expected expression after cast");
            }

            return new ExpressionCast(startToken, type, expr);
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
            var exprTy = expr.getType();
            switch (op.content()) {
                case "++", "--" -> {
                    if (!exprTy.isIntegral()) {
                        expr = new ExpressionTypeError(expr, "prefix operator '" + op.content() +
                                "' is undefined on the type " + exprTy.repr());
                    }
                    // TODO: lvalue analysis
                }
                case "+", "-", "~", "!" -> {
                    if (!exprTy.isArithmetic()) {
                        expr = new ExpressionTypeError(expr, "prefix operator '" + op.content() +
                                "' is undefined on the type " + exprTy.repr());
                    }
                }
                case "*" -> {
                    if (!exprTy.isAPointer()) {
                        expr = new ExpressionTypeError(expr, "cannot deference the type " + exprTy.repr());
                    }
                }
                case "&" -> {
                    // TODO: lvalue analysis
                }
            }
            expr = new ExpressionPrefix(op, expr);
        }

        return expr;
    }

    public Expression parsePostfixExpression() {
        var expr = parsePrimaryExpression();
        if (expr == null) return null;

        while (hasMoreTokens()) {
            if (take(PLUS2, DASH2)) {
                var op = lastToken();
                var exprType = expr.getType();
                if (!exprType.isIntegral()) {
                    expr = new ExpressionTypeError(expr, "cannot increment non-integer type " + exprType.repr());
                }
                // TODO: lvalue analysis

                expr = new ExpressionPostfix(expr, op);
            } else if (take(DOT, ARROW)) {
                var access = lastToken();
                if (!take(LIT_IDENT)) {
                    return new ExpressionSyntaxError(lastToken(), "expected property name after accessor");
                }
                var prop = new ExpressionIdent(lastToken());

                var parentTy = expr.getType();
                if (!(parentTy instanceof SeaType.Struct(String ignored, Map<String, SeaType> fields))) {
                    expr = new ExpressionTypeError(expr, "cannot access non-structure types");
                } else if (!fields.containsKey(prop.name())) {
                    expr = new ExpressionTypeError(prop, "the struct " + parentTy.repr() + " has no field '" + prop.name() + "'");
                }

                expr = new ExpressionAccess(expr, access, prop);
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

                var functorTy = expr.getType();
                if (!(functorTy instanceof SeaType.Func(List<SeaType> params, SeaType ignored))) {
                    expr = new ExpressionTypeError(expr, "cannot invoke " + functorTy.repr());
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
                        s.append(args.get(i).getType().repr());
                    }
                    s.append(")");
                    expr = new ExpressionTypeError(expr, s.toString());
                } else {
                    for (int i = 0; i < params.size(); i++) {
                        var paramTy = params.get(i);
                        var argTy = args.get(i).getType();
                        if (argTy.isConvertibleTo(paramTy)) {
                            continue;
                        }

                        String s = "argument of type " + argTy.repr() + " is not convertible to " + paramTy.repr();
                        args.set(i, new ExpressionTypeError(args.get(i), s));
                    }
                }

                expr = new ExpressionCall(expr, args, lastToken());
            } else if (take(LEFT_BRACKET)) {
                var index = parseExpression();
                if (index == null) {
                    return new ExpressionSyntaxError(lastToken(), "expected index after '['");
                }
                if (!take(RIGHT_BRACKET)) {
                    return new ExpressionSyntaxError(lastToken(), "expected ']' after array index");
                }
                if (!index.getType().isArithmetic()) {
                    expr = new ExpressionSyntaxError(index, "index must be an integral type");
                }
                var exprType = expr.getType();
                if (!exprType.isAPointer()) {
                    expr = new ExpressionTypeError(expr, "cannot index " + exprType.repr());
                } else if (exprType.componentType().isVoid()) {
                    expr = new ExpressionTypeError(expr, "cannot index void*, it has no size");
                }
                expr = new ExpressionIndex(expr, index, lastToken());
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
            return new ExpressionIdent(consume());
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

    protected List<Token> consumeThrough(@NotNull Token.Type... types) {
        var tokens = new ArrayList<Token>();
        while (hasMoreTokens()) {
            var token = consume();
            tokens.add(token);
            if (Arrays.stream(types).anyMatch(t -> t.equals(token.type()))) {
                return tokens;
            }
            index += 1;
        }
        return null;
    }


    protected List<Token> consumeTo(@NotNull Token.Type... types) {
        var tokens = new ArrayList<Token>();
        while (hasMoreTokens()) {
            var token = peekToken();
            if (Arrays.stream(types).anyMatch(t -> t.equals(token.type()))) {
                return tokens;
            }
            tokens.add(token);
            index += 1;
        }
        return null;
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
            throw new ParseException(openingBrace.span(), "unterminated '{'");
        }
    }

    public List<Token> remainingTokens() {
        return tokens.subList(index, tokens.size());
    }

    protected Span spanOf(Token start, Token end) {
        return new Span(start.start(), end.end());
    }
}
