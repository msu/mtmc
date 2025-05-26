package mtmc.lang;

public record Location(int index) {

    public static int[] getLineNos(String source, int... indices) {
        int[] out = new int[indices.length];
        int index = 0, line = 1;
        while (index < source.length()) {
            for (int i = 0; i < indices.length; i++) {
                if (indices[i] == index) {
                    out[i] = line;
                }
            }

            int cp = source.charAt(index);
            if (cp == '\n') {
                line += 1;
            }
            index += Character.charCount(cp);
        }
        return out;
    }

    public LineInfo getLineInfo(String source) {
        int index = 0, lineStart = 0;
        int lineno = 1;
        int column = 1;
        while (index < source.length()) {
            if (index == this.index) {
                break;
            }
            int cp = source.charAt(index);

            if (cp == '\n') {
                lineno += 1;
                lineStart = index + 1;
            } else {
                column += 1;
            }

            index += Character.charCount(cp);
        }

        while (index < source.length()) {
            int cp = source.charAt(index);
            index += Character.charCount(cp);
            if (cp == '\n') break;
        }
        String line = source.substring(lineStart, index);

        return new LineInfo(lineno, column, line);
    }

    public record LineInfo(int lineno, int column, String line) {}
}
