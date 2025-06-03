package mtmc.lang.sea;

import mtmc.lang.sea.ast.Error;

import java.util.List;

public class ValidationException extends RuntimeException {
    public final List<Error> errors;

    public ValidationException(List<Error> errors, String message) {
        super(message);
        this.errors = errors;
    }
}
