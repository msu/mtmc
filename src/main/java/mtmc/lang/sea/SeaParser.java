package mtmc.lang.sea;

import mtmc.lang.sea.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

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
        while (hasMoreTokens() && !matchToken(RIGHT_PAREN)) {
            TypeExpr paramType = parseSimpleType();

            if (!matchToken(LIT_IDENT)) {
                throw new ParseException(peekToken(), "expected parameter name");
            }
            var paramName = consume();

            paramType = parseCompoundType(paramType, paramName);
            params.add(new DeclarationFunc.Param(paramType, paramName));

            if (!consumeToken(COMMA)) break;
        }

        if (!consumeToken(RIGHT_PAREN)) {
            try {
                parseSimpleType();
                throw new ParseException(peekToken(), "expected ')', did you forget a comma?");
            } catch (ParseException ignored) {}

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

        if (!matchToken(LIT_IDENT)) {
            throw new ParseException(peekToken(), "expected declaration name");
        }
        final Token name = consume();

        if (consumeToken(LEFT_PAREN)) {
            var params = parseParamList();

            StatementBlock body = null;
            if (!matchToken(LEFT_BRACE)) {
                if (!consumeToken(SEMICOLON)) {
                    throw new ParseException(lastToken(), "expected ';' after function declaration");
                }
            } else {
                body = parseStatementBlock();
            }

            return new DeclarationFunc(type, name, params, body, body == null ? lastToken() : body.end);
        } else {
            type = parseCompoundType(type, name);

            Expression init = null;
            if (consumeToken(EQUAL)) {
                init = parseExpression();
            }

            if (!consumeToken(SEMICOLON)) {
                throw new ParseException(lastToken(), "expected ';' after variable declaration");
            }

            return new DeclarationVar(type, name, init);
        }
    }

    public TypeExpr parseCompoundType(TypeExpr simpleType, @Nullable Token name) throws ParseException {
        if (consumeToken(LEFT_BRACKET)) {
            if (!consumeToken(RIGHT_BRACKET)) {
                throw new ParseException(peekToken(), "expected ']' after '[' in compound-type");
            }
            return new TypeExprArray(simpleType, lastToken());
        } else {
            return simpleType;
        }
    }

    public TypeExpr parseSimpleType() throws ParseException {
        TypeExpr type = parseTypeName();
        while (matchToken(STAR)) {
            final var tok = consume();
            type = new TypePointer(type, tok);
        }
        return type;
    }

    @NotNull
    public TypeExpr parseTypeName() throws ParseException {
        if (!consumeToken(LIT_IDENT)) {
            throw new ParseException(peekToken(), "expected simple type name");
        }

        var tok = lastToken();
        if (tok.content().equals("int")) {
            return new TypeExprInt(tok);
        } else if (tok.content().equals("char")) {
            return new TypeExprChar(tok);
        } else if (symbols.containsKey(tok.content())) {
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
        return null;
    }

    void recover() throws ParseException {
        Token endToken = null;
        if (openingBrace != null) {
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

            if (!hasMoreTokens()) throw new ParseException(openingBrace, "no corresponding brace!");
            endToken = lastToken();
            index = start;
        }

        while (hasMoreTokens() && peekToken() != endToken) {
            if (consumeToken(SEMICOLON)) return;
            if (matchToken(KW_IF, KW_FOR)) return;
        }
    }

    @Nullable
    public StatementBlock parseStatementBlock() throws ParseException {
        if (!consumeToken(LEFT_BRACE)) return null;
        var start = lastToken();
        var previousOpeningBrace = openingBrace;
        openingBrace = start;

        var stmts = new ArrayList<Statement>();
        while (hasMoreTokens() && !matchToken(RIGHT_BRACE)) {
            try {
                var stmt = parseStatement();
                stmts.add(stmt);
            } catch (ParseException e) {
                var stmt = new StatementSyntaxError(e.token, e.getMessage());
                stmts.add(stmt);
                recover();
            }
        }

        openingBrace = previousOpeningBrace;
        if (!consumeToken(RIGHT_BRACE)) {
            throw new ParseException(lastToken(), "expected '}' after block statement");
        }

        return new StatementBlock(start, stmts, lastToken());
    }

    public Expression parseExpression() {
        return null;
    }

    // =================================
    // UTILITIES BEYOND HERE LIE
    // =================================

    protected boolean hasMoreTokens() {
        return index < tokens.size();
    }

    protected boolean matchToken(@NotNull Token.Type... types) {
        for (var type : types) {
            if (peekToken().type().equals(type)) return true;
        }
        return false;
    }

    protected boolean consumeToken(@NotNull Token.Type... types) {
        for (Token.Type type : types) {
            if (matchToken(type)) {
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
}
