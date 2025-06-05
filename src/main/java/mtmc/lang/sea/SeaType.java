package mtmc.lang.sea;

import java.util.List;
import java.util.Map;

public sealed interface SeaType {
    default int size() {
        if (this == CHAR) return 1;
        if (this == INT) return 2;
        if (this == VOID) return 0;
        if (this instanceof Pointer) return 2;
        throw new IllegalStateException("sizeof " + getClass().getName() + " is undefined");
    }

    default boolean isArithmetic() {
        return this == CHAR || this == INT;
    }

    default boolean isIntegral() {
        return this == CHAR
                || this == INT
                || this instanceof Pointer;
    }

    default boolean isStructure() {
        return this instanceof Struct;
    }

    default boolean isInt() {
        return this == INT;
    }

    default boolean isChar() {
        return this == CHAR;
    }

    default boolean isVoid() {
        return this == VOID;
    }

    default boolean isAnIntegralPointer() {
        return this instanceof Pointer(SeaType component) && component.isIntegral();
    }

    default boolean isAPointerToAnInt() {
        return this instanceof Pointer(SeaType component) && component.isInt();
    }

    default boolean isAPointerTo(SeaType inner) {
        return this instanceof Pointer(SeaType it) && it.equals(inner);
    }

    default boolean isAPointer() {
        return this instanceof Pointer;
    }

    default boolean isFunc() {
        return this instanceof Func;
    }

    default SeaType componentType() {
        return ((Pointer) this).component;
    }

    default SeaType resultType() {
        return ((Func) this).result();
    }

    default boolean isConvertibleTo(SeaType other) {
        if (this.isVoid() || other.isVoid()) return false;
        if (this.isArithmetic() && other.isArithmetic()) return true;
        if (this.isAPointer() && other.isAPointer()) return true;
        return this.equals(other);
    }

    default boolean isCastableTo(SeaType target) {
        if (target.isVoid()) return true;
        if (this.isAPointer() && target.isInt()) return true;
        if (this.isArithmetic() && target.isArithmetic()) return true;
        return this.equals(target);
    }

    default String repr() {
        if (this instanceof Pointer p) {
            if (p.baseType() instanceof Func(List<SeaType> params, SeaType result)) {
                var s = new StringBuilder();
                s.append(result.repr()).append("(*");
                var x = p.component;
                while (x instanceof Pointer p2) {
                    x = p2.component;
                    s.append("*");
                }
                s.append(")(");
                int i = 0;
                for (var param : params) {
                    if (i > 0) s.append(", ");
                    s.append(param.repr());
                    i = i + 1;
                }
                s.append(")");
                return s.toString();
            } else {
                return p.component.repr() + "*";
            }
        }
        if (this == CHAR) return "char";
        if (this == INT) return "int";
        if (this == VOID) return "void";
        if (this instanceof Func(List<SeaType> params, SeaType result)) {
            var s = new StringBuilder();
            s.append(result.repr());
            s.append("(");
            int i = 0;
            for (var param : params) {
                if (i > 0) s.append(", ");
                s.append(param.repr());
                i = i + 1;
            }
            s.append(")");
            return s.toString();
        }
        throw new UnsupportedOperationException("unknown type " + this);
    }

    SeaType CHAR = new Primitive("char");
    SeaType INT = new Primitive("int");
    SeaType VOID = new Primitive("void");

    record Pointer(SeaType component) implements SeaType {
        SeaType baseType() {
            var ty = component;
            while (ty instanceof Pointer(SeaType c)) {
                ty = c;
            }
            return ty;
        }
    }
    final class Primitive implements SeaType {
        // this is purely for debug info lmfao
        public final String name;

        private Primitive(String name) {
            this.name = name;
        }
    }

    record Func(List<SeaType> params, SeaType result) implements SeaType {}

    record Struct(String name, Map<String, SeaType> args) implements SeaType {}
}
