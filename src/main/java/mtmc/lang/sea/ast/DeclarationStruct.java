package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public final class DeclarationStruct extends Declaration implements TypeDeclaration {
    public final Token name;
    public final List<Field> fields;

    public DeclarationStruct(Token start, Token name, List<Field> fields, Token end) {
        super(start, end);
        this.name = name;
        this.fields = List.copyOf(fields);
    }

    public String name() {
        return name.content();
    }

    private SeaType type;
    @Override
    public SeaType type() {
        if (type == null) {
            var fields = new LinkedHashMap<String, SeaType>();
            for (var field : this.fields) {
                fields.put(field.name(), field.type());
            }
            type = new SeaType.Struct(name(), fields);
        }
        return type;
    }

    public static final class Field extends Ast {
        public final TypeExpr type;
        public final Token name;

        public Field(TypeExpr type, Token name) {
            super(type.start, name);
            this.type = type;
            this.name = name;
        }

        public String name() {
            return name.content();
        }

        public SeaType type() {
            return type.type();
        }
    }
}
