package mtmc.lang.sea;

import mtmc.lang.sea.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static mtmc.lang.sea.Token.Type.*;

public class SeaParser {
    private final List<Token> tokens;
    private int index = 0;


    private Token openingBrace = null;
    private final LinkedHashMap<String, Object> symbols;

    public SeaParser(@NotNull List<Token> tokens) {
        this.tokens = tokens;
        this.symbols = new LinkedHashMap<>();
    }

    public SeaParser(SeaParser other) {
        this.tokens = List.copyOf(other.tokens);
        this.index = other.index;
        this.symbols = new LinkedHashMap<>(other.symbols);
    }

    public Unit parseUnit() throws ParseException {
        final var declarations = new ArrayList<Declaration>();

        while (hasMoreTokens()) {
            Token start = peekToken();

            try {
                var decl = parseDeclaration();
                declarations.add(decl);
            } catch (ParseException e) {
                if (openingBrace != null) {
                    consumeMatchingBrace(openingBrace);
                } else {
                    consumeThrough(SEMICOLON);
                }
            }

            if (peekToken().equals(start)) {
                throw new ParseException(peekToken(), "INFINITE LOOP DETECTED");
            }
        }

        return new Unit(declarations, this.symbols);
    }

    public List<DeclarationFunc.Param> parseParamList() throws ParseException {
        var params = new ArrayList<DeclarationFunc.Param>();
        while (hasMoreTokens() && !match(RIGHT_PAREN)) {
            TypeExpr paramType = parseSimpleType();

            if (!match(LIT_IDENT)) {
                throw new ParseException(peekToken(), "expected parameter name");
            }
            var paramName = consume();

            paramType = parseCompoundType(paramType, paramName);
            params.add(new DeclarationFunc.Param(paramType, paramName));

            if (!take(COMMA)) break;
        }

        if (!take(RIGHT_PAREN)) {
            try {
                parseSimpleType();
                throw new ParseException(peekToken(), "expected ')', did you forget a comma?");
            } catch (ParseException ignored) {
            }

            throw new ParseException(peekToken(), "expected ')' after parameter list");
        }

        return params;
    }

    @NotNull
    public Declaration parseDeclaration() throws ParseException {
        TypeExpr type;
        final var t2 = peekToken2();
        if (t2.type().equals(EQUAL) || t2.type().equals(SEMICOLON) || t2.type().equals(LEFT_PAREN)) {
            type = new TypeExprInt(peekToken());
        } else {
            type = parseSimpleType();
        }

        if (!match(LIT_IDENT)) {
            throw new ParseException(peekToken(), "expected declaration name");
        }
        final Token name = consume();

        if (take(LEFT_PAREN)) {
            var params = parseParamList();

            StatementBlock body = null;
            if (!match(LEFT_BRACE)) {
                if (!take(SEMICOLON)) {
                    throw new ParseException(lastToken(), "expected ';' after function declaration");
                }
            } else {
                body = parseStatementBlock();
            }

            return new DeclarationFunc(type, name, params, body, body == null ? lastToken() : body.end);
        } else {
            type = parseCompoundType(type, name);

            Expression init = null;
            if (take(EQUAL)) {
                init = parseExpression();
            }

            if (!take(SEMICOLON)) {
                throw new ParseException(lastToken(), "expected ';' after variable declaration");
            }

            return new DeclarationVar(type, name, init);
        }
    }

    public TypeExpr parseCompoundType(TypeExpr simpleType, @Nullable Token ignored) throws ParseException {
        if (take(LEFT_BRACKET)) {
            if (!take(RIGHT_BRACKET)) {
                throw new ParseException(peekToken(), "expected ']' after '[' in compound-type");
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
            throw new ParseException(peekToken(), "expected simple type name");
        }
        var tok = lastToken();
        if (symbols.containsKey(tok.content())) {
            if (symbols.get(tok.content()) instanceof TypeDeclaration decl) {
                return new TypeExprRef(tok, decl);
            } else {
                throw new ParseException(tok, tok.content() + " is not a type");
            }
        } else {
            throw new ParseException(tok, "unknown type '" + tok.content() + "'");
        }
    }

    public Statement parseStatement() throws ParseException {
        Token label = null;
        if (peekToken2().type().equals(COLON)) {
            if (!take(LIT_IDENT)) {
                throw new ParseException(peekToken(), "expected label!");
            }
            label = lastToken();
            consume();
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
            if (!take(SEMICOLON)) throw new ParseException(lastToken(), "expected ';' after do-while statement");
            doWhileStmt.setLabelAnchor(label);
            return doWhileStmt;
        }

       var varStmt = parseStatementVar();
        if (varStmt != null) {
            if (!take(SEMICOLON)) throw new ParseException(lastToken(), "expected ';' after variable statement");
            varStmt.setLabelAnchor(label);
            return varStmt;
        }

        var gotoStmt = parseGotoStatement();
        if (gotoStmt != null) {
            if (!take(SEMICOLON)) throw new ParseException(lastToken(), "expected ';' after goto statement");
            gotoStmt.setLabelAnchor(label);
            return gotoStmt;
        }

        var continueStmt = parseStatementContinue();
        if (continueStmt != null) {
            if (!take(SEMICOLON)) throw new ParseException(lastToken(), "expected ';' after continue statement");
            continueStmt.setLabelAnchor(label);
            return continueStmt;
        }

        var breakStmt = parseStatementBreak();
        if (breakStmt != null) {
            if (!take(SEMICOLON)) throw new ParseException(lastToken(), "expected ';' after break statement");
            breakStmt.setLabelAnchor(label);
            return breakStmt;
        }

        Expression expr = parseExpression();
        if (expr != null) {
            var stmt = new StatementExpression(expr);
            stmt.setLabelAnchor(label);
        }

        throw new ParseException(lastToken(), "expected statement");
    }

    StatementIf parseStatementIf() throws ParseException {
        if (!take(KW_IF)) return null;
        var start = lastToken();

        if (!take(LEFT_PAREN)) throw new ParseException(lastToken(), "expected '(' after 'if'");
        var cond = parseExpression();
        if (cond == null) throw new ParseException(lastToken(), "expected 'if' condition");
        if (!take(RIGHT_PAREN)) throw new ParseException(lastToken(), "expected ')' after 'if'");

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

        if (!take(LEFT_PAREN)) throw new ParseException(lastToken(), "expected '(' after 'for'");

        Expression initExpr = null;
        StatementVar initStmt = null;
        if (peekTypeName()) {
            initStmt = parseStatementVar();
        } else {
            initExpr = parseExpression();
        }

        if (!take(SEMICOLON)) throw new ParseException(peekToken(), "expected ';' after for initializer");

        @Nullable
        Expression condition = parseExpression();

        if (!take(SEMICOLON)) throw new ParseException(peekToken(), "expected ';' after for condition");
        Expression incr = parseExpression();

        if (!take(RIGHT_PAREN)) throw new ParseException(peekToken(), "expected ')' after for-loop condition");

        var body = parseStatement();

        return new StatementFor(start, initExpr, initStmt, condition, incr, body);
    }

    StatementVar parseStatementVar() throws ParseException {
        if (!peekTypeName()) return null;

        TypeExpr type = parseSimpleType();

        if (!take(LIT_IDENT)) throw new ParseException(peekToken(), "expected variable name");
        Token name = lastToken();
        type = parseCompoundType(type, name);

        Expression value = null;
        if (take(EQUAL)) {
            value = parseExpression();
            if (value == null) {
                throw new ParseException(lastToken(), "expected initializer value after '='");
            }
        }

        return new StatementVar(type, name, value);
    }

    StatementWhile parseStatementWhile() throws ParseException {
        if (!take(KW_WHILE)) return null;
        var start = lastToken();

        if (!take(LEFT_PAREN)) throw new ParseException(lastToken(), "expected '(' after 'while'");
        Expression condition = parseExpression();
        if (condition == null) throw new ParseException(lastToken(), "expected while-condition");
        if (!take(RIGHT_PAREN)) throw new ParseException(lastToken(), "expected ')' after 'while'");

        var body = parseStatement();

        return new StatementWhile(start, condition, body);
    }

    StatementDoWhile parseStatementDoWhile() throws ParseException {
        if (!take(KW_WHILE)) return null;
        var start = lastToken();

        var body = parseStatement();

        if (!take(LEFT_PAREN)) throw new ParseException(lastToken(), "expected '(' after do body");
        Expression condition = parseExpression();
        if (condition == null) throw new ParseException(lastToken(), "expected do-while-condition");
        if (!take(RIGHT_PAREN)) throw new ParseException(lastToken(), "expected ')' after do-while condition");

        return new StatementDoWhile(start, body, condition, lastToken());
    }

    StatementGoto parseGotoStatement() throws ParseException {
        if (!take(KW_GOTO)) return null;
        var start = lastToken();

        if (!take(LIT_IDENT)) throw new ParseException(peekToken(), "expected label name after 'goto'");
        var labelName = lastToken();

        return new StatementGoto(start, labelName);
    }

    StatementBreak parseStatementBreak() {
        if (!take(KW_BREAK)) return null;
        return new StatementBreak(lastToken());
    }

    StatementContinue parseStatementContinue() {
        if (!take(KW_CONTINUE)) return null;
        return new StatementContinue(lastToken());
    }

    void recover(Token ob) throws ParseException {
        Token endToken = null;
        if (ob != null) {
            int start = index;
            var open = 1;
            while (hasMoreTokens() && open > 0) {
                var token = consume();
                if (token.type().equals(LEFT_BRACE)) {
                    open++;
                } else if (token.type().equals(RIGHT_BRACE)) {
                    open--;
                }
            }

            if (!hasMoreTokens()) throw new ParseException(ob, "unterminated '{'");
            endToken = lastToken();
            index = start;
        }

        while (hasMoreTokens() && peekToken() != endToken) {
            if (take(SEMICOLON)) return;
            if (match(KW_IF, KW_FOR)) return;
        }
    }

    @Nullable
    public StatementBlock parseStatementBlock() throws ParseException {
        if (!take(LEFT_BRACE)) return null;
        var start = lastToken();
        var previousOpeningBrace = openingBrace;
        openingBrace = start;

        var stmts = new ArrayList<Statement>();
        while (hasMoreTokens() && !match(RIGHT_BRACE)) {
            try {
                var stmt = parseStatement();
                stmts.add(stmt);
            } catch (ParseException e) {
                var stmt = new StatementSyntaxError(e.token, e.getMessage());
                stmts.add(stmt);
                recover(start);
            }
        }

        openingBrace = previousOpeningBrace;
        if (!take(RIGHT_BRACE)) {
            throw new ParseException(lastToken(), "expected '}' after block statement");
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
                AMPERSAND_EQ, BAR_EQ))
        {
            var op = lastToken();
            var rhs = parseTernaryExpression();
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of assignment expression");
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
                then = new ExpressionSyntaxError(expr, lastToken(), "expected then-expression after ternary '?'");
                consumeTo(COLON);
            }

            if (!take(COLON)) {
                expr = new ExpressionSyntaxError(expr, lastToken(), "expected ':' in ternary expression");
            }

            var otherwise = parseTernaryExpression();
            if (otherwise == null) {
                otherwise = new ExpressionSyntaxError(expr, lastToken(), "expected else-expression after ternary ':'");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of logical-or expression");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of logical-and expression");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of bitwise-or expression");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of bitwise-xor expression");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of bitwise-and expression");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of equality expression");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of comparison expression");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of shift expression");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of additive expression");
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
            if (rhs == null)
                rhs = new ExpressionSyntaxError(expr, lastToken(), "expected right hand side of multiplicative expression");
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
            if (!take(RIGHT_PAREN))
                return new ExpressionSyntaxError(null, lastToken(), "expected type name after cast parens");

            var expr = parsePrefixExpression();
            if (expr == null) return new ExpressionSyntaxError(null, lastToken(), "expected expression after cast");

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
            return new ExpressionSyntaxError(null, lastToken(), "expected expression after unary operator");
        }
        while (!stack.isEmpty()) {
            var op = stack.removeLast();
            expr = new ExpressionPrefix(op, expr);
        }

       return expr;
    }

    public Expression parsePostfixExpression() {
        var expr = parsePrimaryExpression();
        if (expr == null) return null;

        while (hasMoreTokens()) {
            if (take(PLUS2, DASH2)) {
                expr = new ExpressionPostfix(expr, lastToken());
            } else if (take(DOT, ARROW)) {
                var access = lastToken();
                if (!take(LIT_IDENT))
                    return new ExpressionSyntaxError(expr, lastToken(), "expected property name after accessor");
                expr = new ExpressionAccess(expr, access, lastToken());
            } else if (take(LEFT_PAREN)) {
                var args = new ArrayList<Expression>();
                while (hasMoreTokens() && !match(RIGHT_PAREN)) {
                    var arg = parseExpression();
                    if (arg instanceof ExpressionSyntaxError) {
                        consumeTo(COMMA, RIGHT_PAREN);
                    } else if (arg == null) {
                        arg = new ExpressionSyntaxError(expr, lastToken(), "expected call argument");
                    }
                    args.add(arg);

                    if (!take(COMMA)) break;
                }

                if (!take(RIGHT_PAREN)) {
                    var trailing = parseExpression();
                    if (trailing instanceof ExpressionSyntaxError) {
                        return new ExpressionSyntaxError(expr, trailing.start, "expected ')', did you forget a comma?");
                    } else {
                        return new ExpressionSyntaxError(expr, lastToken(), "expected ')' after calling arguments");
                    }
                }

                return new ExpressionCall(expr, args, lastToken());
            } else if (take(LEFT_BRACKET)) {
                var index = parseExpression();
                if (index == null) return new ExpressionSyntaxError(expr, lastToken(), "expected index after '['");
                if (!take(RIGHT_BRACKET))
                    return new ExpressionSyntaxError(index, lastToken(), "expected ']' after array index");
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
            if (inner == null) return new ExpressionSyntaxError(null, lastToken(), "expected expression after '('");
            if (!take(RIGHT_PAREN))
                return new ExpressionSyntaxError(inner, lastToken(), "expected ')' after grouped expression");
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

    void consumeMatchingBrace(Token token) throws ParseException {
        assert token.type().equals(LEFT_BRACE);
        var open = 1;
        var parser = new SeaParser(this);
        parser.index = parser.tokens.indexOf(token);
        parser.consume();
        while (open > 0 && parser.hasMoreTokens()) {
            if (parser.take(LEFT_BRACE)) open += 1;
            else if (parser.take(RIGHT_BRACE)) open -= 1;
            else consume();
        }

        if (open > 0) {
            throw new ParseException(token, "unterminated '{'");
        }
    }

    public List<Token> remainingTokens() {
        return tokens.subList(index, tokens.size());
    }
}
