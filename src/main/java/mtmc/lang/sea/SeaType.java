package mtmc.lang.sea;

import java.util.List;
import java.util.Map;

public sealed interface SeaType {
    default boolean isArithmetic() {
        return this == CHAR
                || this == UNSIGNED_CHAR
                || this == SIGNED_CHAR
                || this == UNSIGNED_INT
                || this == INT;
    }

    default boolean isIntegral() {
        return this == CHAR
                || this == UNSIGNED_CHAR
                || this == SIGNED_CHAR
                || this == UNSIGNED_INT
                || this == INT
                || this instanceof Pointer;
    }

    default boolean isStructure() {
        return this instanceof Struct;
    }

    default boolean isAnInt() {
        return this == UNSIGNED_INT || this == INT;
    }

    default boolean isVoid() {
        return this == VOID;
    }

    default boolean isAnIntegralPointer() {
        return this instanceof Pointer(SeaType component) && component.isIntegral();
    }

    default boolean isAPointerToAnInt() {
        return this instanceof Pointer(SeaType component) && component.isAnInt();
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

    default boolean isConvertibleTo(SeaType other) {
        if (this.isVoid() || other.isVoid()) return false;
        if (this.isIntegral() && other.isIntegral()) return true;
        if (this.isAPointer() && other.isAPointer()) return true;
        return this.equals(other);
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
        if (this == UNSIGNED_CHAR) return "unsigned char";
        if (this == SIGNED_CHAR) return "signed char";
        if (this == UNSIGNED_INT) return "unsigned int";
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

    SeaType CHAR = new Primitive();
    SeaType UNSIGNED_CHAR = new Primitive();
    SeaType SIGNED_CHAR = new Primitive();
    SeaType UNSIGNED_INT = new Primitive();
    SeaType INT = new Primitive();
    SeaType VOID = new Primitive();

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
        private Primitive() {}
    }

    record Func(List<SeaType> params, SeaType result) implements SeaType {}

    record Struct(String name, Map<String, SeaType> args) implements SeaType {}
}
