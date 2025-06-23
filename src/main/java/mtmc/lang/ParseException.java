package mtmc.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ParseException extends Exception {
    public final List<Message> messages;

    public ParseException(Message message, Message ...rest) {
        var messages = new ArrayList<Message>(1 + rest.length);
        messages.add(message);
        messages.addAll(Arrays.asList(rest));
        this.messages = Collections.unmodifiableList(messages);
    }

    public ParseException(ParseException parent, Message message, Message ...rest) {
        var messages = new ArrayList<Message>( 1 + rest.length + parent.messages.size());
        messages.add(message);
        messages.addAll(Arrays.asList(rest));
        messages.addAll(parent.messages);
        this.messages = Collections.unmodifiableList(messages);
    }

    public record Message(Span span, String message) {
        public Message(Token token, String message) {
            this(Span.of(token), message);
        }

        public Token start() {
            return span.start();
        }

        public Token end() {
            return span.end();
        }
    }

    public String report(String source) {
        return "TODO: I ain't no snitch";
    }
}
