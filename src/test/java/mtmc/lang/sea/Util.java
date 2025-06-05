package mtmc.lang.sea;

import mtmc.lang.ParseException;

public class Util {
    public static void reportError(String src, StringBuilder sb, ParseException e) {
        sb.append("Error:\n");
        for (var msg : e.messages) {
            var lo = Token.getLineAndOffset(src, msg.start().start());
            int lineNo = lo[0];
            int column = lo[1];
            var line = Token.getLineFor(src, msg.start().start());
            String prefix = "  %03d:%03d | ".formatted(lineNo, column);
            String info = " ".repeat(prefix.length() - 2) + "| ";
            sb.append(info).append(msg.message()).append('\n');
            sb.append(prefix).append(line).append('\n');
            sb
                    .repeat(' ', prefix.length() + column - 1)
                    .repeat('^', Math.max(1, msg.end().end() - msg.start().start()));
            sb.append("\n\n");
        }
    }
}
