package mtmc.lang.sea.ast;

import mtmc.lang.ParseException;

public interface SyntaxError extends Error {
    ParseException exception();
}
