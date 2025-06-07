package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaParser;
import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

import java.util.ArrayList;
import java.util.List;

public final class DeclarationFunc extends Declaration {
    public final TypeExpr returnType;
    public final Token name;
    public final ParamList params;
    public final StatementBlock body;

    public DeclarationFunc(TypeExpr returnType, Token name, ParamList paramList, StatementBlock body, Token end) {
        super(returnType.start, end);
        this.returnType = returnType;
        this.name = name;
        this.params = paramList;
        this.body = body;
    }

    public static final class Param extends Ast {
        public final TypeExpr type;
        public final Token name;

        public Param(TypeExpr type, Token name) {
            super(type.start, name.end() < type.end.end() ? type.end : name);
            this.type = type;
            this.name = name;
        }
    }

    private SeaType.Func type;
    public SeaType.Func type() {
        if (type == null) {
            var paramTypes = new ArrayList<SeaType>(params.size());
            for (Param param : params.params) {
                paramTypes.add(param.type.type());
            }
            type = new SeaType.Func(
                    paramTypes,
                    params.isVararg,
                    returnType.type()
            );
        }
        return type;
    }

    public record ParamList(List<DeclarationFunc.Param> params, boolean isVararg) {
        public int size() {
            return params.size();
        }

        public SeaType getParamType(int i) {
            return params.get(i).type.type();
        }
    }
}