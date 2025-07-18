package mtmc.lang.sea;

import java.sql.Blob;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public sealed interface SeaType {
    default int size() {
        if (this == CHAR) return 2;
        if (this == INT) return 2;
        if (this == VOID) return 0;
        if (this instanceof Pointer) return 2;
        if (this instanceof Struct struct) {
            int size = 0;
            for (Map.Entry<String, SeaType> fieldSet : struct.fields.entrySet()) {
                SeaType type = fieldSet.getValue();
                size += type.size();
            }
            return size;
        }
        throw new IllegalStateException("sizeof " + getClass().getName() + " is undefined");
    }

    default boolean isArithmetic() {
        return this == CHAR || this == INT;
    }

    default boolean isIntegral() {
        return this == CHAR || this == INT || this instanceof Pointer;
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

    default void checkConversionTo(SeaType other) throws ConversionError {
        if (this.isVoid() || other.isVoid()) throw new ConversionError(this, other, "void is not assignable to void");
        if (this.isArithmetic() && other.isArithmetic()) return;
        if (this.isAPointer() && other.isAPointer()) return;
        if (this instanceof Initializer initializer && other instanceof Struct s) {
            // this is kinda complex
            // the way this should work is we process named assignments and then based on the last
            // index of the named assignments, we start putting in values
            // the challenge here is that the blob may have too many values or may initialize them improperly
            if (initializer.values.size() != s.fields.size()) {
                throw new ConversionError(this, other, "initializer has too many or too few values");
            }

            int i = 0;
            for (Map.Entry<String, SeaType> entry : s.fields.entrySet()) {
                var name = entry.getKey();
                var ty = entry.getValue();
                var valueTy = initializer.values.get(i);

                try {
                    valueTy.checkConversionTo(ty);
                } catch (ConversionError error) {
                    throw new ConversionError(ty, valueTy,
                            "value cannot be assigned to " + ty.repr() + " for '" + name + "'", error);
                }

                i += 1;
            }
            return;
        }
        if (!this.equals(other)) {
            throw new ConversionError(this, other, this.repr() + " is not convertible to " + other.repr());
        }
    }

    class ConversionError extends Exception {
        public final SeaType fromType, toType;

        private ConversionError(SeaType fromType, SeaType toType, String message) {
            super(message);
            this.fromType = fromType;
            this.toType = toType;
        }

        private ConversionError(SeaType fromType, SeaType toType, String message, ConversionError parent) {
            super(message, parent);
            this.fromType = fromType;
            this.toType = toType;
        }
    }

    default boolean isCastableTo(SeaType target) {
        if (target.isVoid()) return true;
        if (this.isAPointer() && target.isInt()) return true;
        if (this.isArithmetic() && target.isArithmetic()) return true;
        return this.equals(target);
    }

    default String repr() {
        if (this instanceof Pointer p) {
            if (p.baseType() instanceof Func(List<SeaType> params, boolean isVararg, SeaType result)) {
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
        if (this instanceof Func(List<SeaType> params, boolean isVararg, SeaType result)) {
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
        if (this instanceof Initializer(List<SeaType> values)) {
            var s = new StringBuilder();
            s.append("{");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) s.append(", ");
                s.append(values.get(i).repr());
            }
            s.append("}");
            return s.toString();
        }
        if (this instanceof Struct s) {
            return "struct " + s.name;
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

        @Override
        public String toString() {
            return name;
        }
    }

    record Func(List<SeaType> params, boolean isVararg, SeaType result) implements SeaType {
        public Func(List<SeaType> params, SeaType result) {
            this(params, false, result);
        }
    }

    record Struct(String name, LinkedHashMap<String, SeaType> fields) implements SeaType {
        public SeaType field(String name) {
            return fields.get(name);
        }
    }

    record Initializer(List<SeaType> values) implements SeaType {
    }
}
