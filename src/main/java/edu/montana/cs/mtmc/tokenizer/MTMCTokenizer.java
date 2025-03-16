package edu.montana.cs.mtmc.tokenizer;

import edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.stream.Stream;

import static edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType.*;

public class MTMCTokenizer {

    String src;
    LinkedList<MTMCToken> tokens;
    int currentToken = 0;
    
    public MTMCTokenizer(String source, String lineCommentStart) {
        this.src = source;
        tokens = new MTMCScanner(source, lineCommentStart).tokenize();
    }

    public MTMCToken currentToken() {
        return tokens.get(currentToken);
    }

    public MTMCToken consume() {
        return tokens.get(currentToken++);
    }

    public String consumeAsString() {
        return tokens.get(currentToken++).getStringValue();
    }

    public MTMCToken matchAndConsume(TokenType... type) {
        if (match(type)) {
            return consume();
        } else {
            return null;
        }
    }

    public boolean matchAndConsume(String identifier) {
        if (currentToken().getType().equals(IDENTIFIER) &&
                currentToken().getStringValue().equals(identifier)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean match(TokenType... type) {
        for (TokenType tokenType : type) {
            if (currentToken().getType().equals(tokenType)) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        currentToken = 0;
    }

    public boolean more() {
        return !currentToken().getType().equals(EOF);
    }

    public MTMCToken previousToken() {
        return tokens.get(Math.max(0, currentToken - 1));
    }

    public Stream<MTMCToken> stream() {
        return tokens.stream();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            MTMCToken token = tokens.get(i);
            if (i == currentToken) {
                sb.append("-->[");
            }
            sb.append(token.getStringValue());
            if (i == currentToken) {
                sb.append("]<--");
            }
            sb.append(" ");
        }
        return sb.toString();
    }

    // collapse all adjacent tokens into a string
    public String collapseTokensAsString() {
        if (more()) {
            StringBuilder sb = new StringBuilder();
            MTMCToken last, next;
            do {
                last = consume();
                sb.append(last.getStringValue());
                next = currentToken();
            } while (more() && last.end == next.start);
            return sb.toString();
        } else {
            return "";
        }
    }

    public Integer consumeAsInteger() {
        return consume().getIntegerValue();
    }

    public MTMCToken require(TokenType tokenType, Runnable notFound) {
        if (match(tokenType)) {
            return consume();
        } else {
            notFound.run();
            return null;
        }
    }

}