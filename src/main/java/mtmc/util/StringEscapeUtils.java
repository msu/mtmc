package mtmc.util;

public final class StringEscapeUtils {
    private StringEscapeUtils() {
    }

    public static String escapeString(String s) {
        s = s
                .replace("\"", "\\\"")
                .replace("\\", "\\\\")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\f", "\\f")
                .replace("\r", "\\r");

        var out = new StringBuilder();
        out.append('"');
        s.codePoints()
                .forEach(c -> {
                    if (c < 32 || c > 0x7f) {
                        out.append("\\u{").append(Integer.toHexString(c)).append("}");
                    } else {
                        out.append((char) c);
                    }
                });
        out.append('"');
        return out.toString();
    }
}
