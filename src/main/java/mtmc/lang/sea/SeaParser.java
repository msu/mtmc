package mtmc.lang.sea;

import mtmc.lang.sea.ast.*;
import mtmc.tokenizer.MTMCToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static mtmc.lang.sea.Token.Type.*;

public class SeaParser {
    protected List<Token> tokens;
    protected int index = 0;

    public SeaParser(@NotNull List<Token> tokens) {
        this.tokens = tokens;
    }

    private final LinkedHashMap<String, Object> symbols = new LinkedHashMap<>();

    public Declaration parseDeclaration() throws ParseException {
        TypeExpr type;
        final var t2 = peekToken2();
        if (t2.type().equals(Equal) || t2.type().equals(LeftParen)) {
            type = new TypeExprInt(peekToken());
        } else {
            type = parseSimpleType();
        }

        if (!match(LIT_IDENT)) {
            throw new ParseException(peekToken(), "expected declaration name");
        }
        final Token name = consume();

        if (take(LeftParen)) {

            var params = new ArrayList<DeclarationFunc.Param>();
            while (hasMoreTokens() && !match(RightParen)) {
                TypeExpr paramType = parseSimpleType();

                if (!match(LIT_IDENT)) {
                    throw new ParseException(peekToken(), "expected parameter name");
                }
                var paramName = consume();

                paramType = parseCompoundType(paramType, paramName);
                params.add(new DeclarationFunc.Param(paramType, paramName));

                if (!take(Comma)) break;
            }


            if (!take(RightParen)) {
                try {
                    parseSimpleType();
                    throw new ParseException(peekToken(), "expected ')', did you forget a comma?");
                } catch (ParseException ignored) {}
                throw new ParseException(peekToken(), "expected ')' after parameter list");
            }

            return new DeclarationFunc(type, name, params, lastToken());
        } else {
            type = parseCompoundType(type, name);

        }

        return null;
    }

    public TypeExpr parseCompoundType(TypeExpr simpleType, @Nullable Token name) throws ParseException {
        if (take(LeftBracket)) {
            if (!take(RightBracket)) {
                throw new ParseException("expected ']' after '[' in compound-type");
            }
            return new TypeExprArray(simpleType, lastToken());
        } else {
            return simpleType;
        }
    }

    public TypeExpr parseSimpleType() throws ParseException {
        TypeExpr type = parseTypeName();
        while (match(Star)) {
            final var tok = consume();
            type = new TypePointer(type, tok);
        }
        return type;
    }

    @NotNull
    public TypeExpr parseTypeName() throws ParseException {
        final var tok = peekToken();
        if (!tok.type().equals(LIT_IDENT)) {
            throw new ParseException("expected simple type name");
        }
        consume();

        if (tok.content().equals("int")) {
            return new TypeExprInt(tok);
        } else if (tok.content().equals("char")) {
            return new TypeExprChar(tok);
        } else if (symbols.containsKey(tok.content())) {
            if (symbols.get(tok.content()) instanceof TypeDeclaration decl) {
                return new TypeExprRef(tok, decl);
            } else {
                throw new ParseException(tok.content() + " is not a type");
            }
        } else {
            throw new ParseException("unknown type '" + tok.content() + "'");
        }
    }

    public Statement parseStatement() {
        return null;
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

    protected boolean match(@NotNull Token.Type... types) {
        for (var type : types) {
            if (peekToken().type().equals(type)) return true;
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
