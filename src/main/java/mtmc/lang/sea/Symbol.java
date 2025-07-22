package mtmc.lang.sea;

import mtmc.lang.sea.ast.*;

public class Symbol {
    public final String name;
    public final SeaType type;
    public final TypeDeclaration typeDecl;
    public final boolean isParam, isGlobal;

    public Symbol(DeclarationFunc.Param param) {
        this.name = param.name.content();
        this.type = param.type.type();
        this.typeDecl = null;
        this.isParam = true;
        this.isGlobal = false;
    }

    public Symbol(DeclarationVar decl) {
        this.name = decl.name();
        this.type = decl.type.type();
        this.typeDecl = null;
        this.isParam = false;
        this.isGlobal = true;
    }

    public Symbol(StatementVar stmt) {
        this.name = stmt.name();
        this.type = stmt.type.type();
        this.typeDecl = null;
        this.isParam = false;
        this.isGlobal = false;
    }

    public Symbol(DeclarationFunc func) {
        this.name = func.name.content();
        this.type = func.type();
        this.typeDecl = null;
        this.isParam = false;
        this.isGlobal = true;
    }

    public Symbol(TypeDeclaration declaration) {
        this.name = declaration.name();
        this.type = null;
        this.typeDecl = declaration;
        this.isParam = false;
        this.isGlobal = false;
    }

    public boolean isAddressable() {
        if (this.typeDecl != null) throw new IllegalStateException("cannot address non-data symbol");
        if (this.isParam) return false; // parameters are not addressable!
        return true;
    }
}