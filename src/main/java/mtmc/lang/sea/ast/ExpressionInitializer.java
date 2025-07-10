package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ExpressionInitializer extends Expression {
    public final List<Expression> values;

    private static SeaType.Initializer blobType(List<Expression> values) {
        var types = new ArrayList<SeaType>();

        for (var value : values) {
            types.add(value.type());
        }

        return new SeaType.Initializer(types);
    }

    public ExpressionInitializer(Token start, List<Expression> values, Token end) {
        super(start, end, blobType(values));
        this.values = List.copyOf(values);
    }

}
