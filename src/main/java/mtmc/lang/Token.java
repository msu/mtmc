package mtmc.lang;

public interface Token {
    Location getStart();
    Location getEnd();
    String getContent();

    default int start() {
        return getStart().index();
    }

    default int end() {
        return getEnd().index();
    }
}
