package mtmc.lang.sea;

import mtmc.asm.Assembler;
import mtmc.lang.CompilationException;
import mtmc.lang.Span;
import mtmc.lang.sea.ast.*;
import mtmc.os.exec.Executable;
import mtmc.util.StringEscapeUtils;

import java.io.IOException;
import java.util.*;

public class SeaCompiler {
    protected Unit program;

    private StringBuilder data = new StringBuilder();
    private StringBuilder code = new StringBuilder();
    private HashMap<String, String> globalLabels = new HashMap<>();
    private HashMap<Object, String> dataLabels = new HashMap<>();
    private int currentLineNo = -1;

    public SeaCompiler(Unit program) {
        this.program = program;
    }

    public Executable compile() throws CompilationException {
        // t0, t1 are temporary values, t3 is a stack offset, t4 & t5 are comparison temporaries
        // data-types: pointers, words, and bytes
        // globals end in the 'data' segment
        // locals end up on the stack
        // code in the 'text' segment

        // Calling Convention:
        // jal   # (mov ra, pc+sizeof(*pc), j addr)
        //
        // -- begin inner function
        // max 4 fields, a0-a3
        // push fp
        // mov fp, sp
        // sp -= (sizeof locals (max 15 words))
        //   via: 'dec sp, 14'
        // 'lwr/swr $target_reg, fp, $offset_reg' to save & load relative
        // then we have an expression stack
        // ...compilation
        // return value in the rv register
        // mov sp, fp
        // pop fp
        // ret          # 'jr $ra'

        for (var decl : program.declarations) {
            compile(decl);
        }

        String data = this.data.toString();
        String code = this.code.toString();

        String asm = "";

        if (this.program.filename != null) {
            asm += "@file " + StringEscapeUtils.escapeString(this.program.filename) + "\n";
        }

        if (!data.isBlank()) {
            asm += ".data\n" + data;
        }

        asm += '\n';
        asm += ".text\n";
        asm += """
                mov fp sp
                jal main
                mov a0 rv
                sys exit
                """;
        asm += code;

        System.out.println("generated the following assembly:");
        System.out.println("=".repeat(20));
        int lineno = 1;
        for (Iterator<String> it = asm.lines().iterator(); it.hasNext(); ) {
            String line = it.next();
            System.out.printf("%3d | %s%n", lineno, line);
            lineno += 1;
        }
        System.out.println("=".repeat(20));
        var assembler = new Assembler();
        var exec = assembler.assembleExecutable("program", asm);
        return exec;
    }

    protected void compile(Declaration decl) throws CompilationException {
        switch (decl) {
            case DeclarationFunc declarationFunc -> compile(declarationFunc);
            case DeclarationSyntaxError ignored ->
                    throw new UnsupportedOperationException("error checking should happen before compilation");
            case DeclarationTypedef declarationTypedef -> compile(declarationTypedef);
            case DeclarationVar declarationVar -> compile(declarationVar);
            case DeclarationStruct declarationStruct -> compile(declarationStruct);
        }
    }

    // the tail label of the current loop
    Stack<String> currentLoopBreakLabel = new Stack<>();
    Stack<String> currentLoopContinueLabel = new Stack<>();
    int labelNo = 0;

    protected void compile(DeclarationFunc func) throws CompilationException {
        switch (func.name.content()) {
            case "printf" -> {
                if (!func.returnType.type().isInt()) {
                    throw new CompilationException("'printf' must return void", func.returnType.span());
                }

                if (func.params.size() != 1 || !func.params.getParamType(0).isAPointerTo(SeaType.CHAR) || !func.params.isVararg()) {
                    throw new CompilationException("'printf' must have signature (char *, ...)", Span.of(func.name));
                }

                code.append("printf:\n");
                code.append("  sys printf\n");
                code.append("  mov sp a1\n"); // restore the stack
                code.append("  ret\n");
                return;
            }
            case "putc" -> {
                if (!func.returnType.type().isVoid()) {
                    throw new CompilationException("'putc' must return void", func.returnType.span());
                }
                if (func.params.size() != 1 || !func.params.getParamType(0).isChar()) {
                    throw new CompilationException("'puts' must take a char as its only argument", Span.of(func.name));
                }

                code.append("putc:\n");
                code.append("  sys wchr\n");
                code.append("  ret\n");
                return;
            }
            case "puts" -> {
                if (!func.returnType.type().isVoid()) {
                    throw new CompilationException("'puts' must return void", func.returnType.span());
                }
                if (func.params.size() != 1 || !func.params.getParamType(0).isAPointerTo(SeaType.CHAR)) {
                    throw new CompilationException("'puts' must take a char* as its only argument", Span.of(func.name));
                }

                code.append("puts:\n");
                code.append("  sys wstr\n");
                code.append("  ret\n");
                return;
            }
            case "putn" -> {
                if (!func.returnType.type().isVoid()) {
                    throw new CompilationException("'putn' must return void", func.returnType.span());
                }
                if (func.params.size() != 1 || !func.params.getParamType(0).isInt()) {
                    throw new CompilationException("'putn' must take an int as its only argument", Span.of(func.name));
                }

                code.append("putn:\n");
                code.append("  sys wint\n");
                code.append("  ret\n");
                return;
            }
        }

        code.append(func.name.content()).append(":\n");
        int lineNo = Token.getLineAndOffset(program.source, func.start.start())[0];
        if (currentLineNo != lineNo) {
            code.append("@line ").append(lineNo).append('\n');
            currentLineNo = lineNo;
        }
        code.append("  push fp\n");
        code.append("  mov fp sp\n");

        var frame = new Frame(func.params);
        for (DeclarationFunc.Param param : func.params.params()) {
            int typeSize = param.type.type().size();
            assert typeSize == 2;
            frame.add(param.name.content(), typeSize);
        }

        int maxLocalSize = calcMaxLocalSize(func.body);
        if (maxLocalSize > 0) {
            if (maxLocalSize > 15) {
                code.append("  li t0 ").append(maxLocalSize).append("\n");
                code.append("  sub sp t0\n");
            } else {
                code.append("  dec sp ").append(maxLocalSize).append("\n");
            }
        }

        compile(func.body, frame);
        labelNo = 0;

        // TODO: dangling return
    }

    private int calcMaxLocalSize(Statement body) {
        return switch (body) {
            case StatementBlock statementBlock -> {
                int vars = 0;
                int temps = 0;
                for (var child : statementBlock.statements) {
                    if (child instanceof StatementVar sv) {
                        vars += sv.type.type().size();
                    } else {
                        int r = calcMaxLocalSize(child);
                        temps = Math.max(temps, r);
                    }
                }
                yield vars + temps;
            }
            case StatementBreak ignored -> 0;
            case StatementContinue ignored -> 0;
            case StatementDoWhile stmt -> {
                int n = calcMaxLocalSize(stmt.body);
                yield n;
            }
            case StatementExpression ignored -> 0;
            case StatementFor stmt -> {
                int n = 0;
                if (stmt.initStatement != null) n += calcMaxLocalSize(stmt.initStatement);
                n += calcMaxLocalSize(stmt.body);
                yield n;
            }
            case StatementGoto ignored -> 0;
            case StatementIf statementIf -> {
                int t = calcMaxLocalSize(statementIf.body);
                int e = statementIf.elseBody == null ? 0 : calcMaxLocalSize(statementIf.elseBody);
                yield Math.max(t, e);
            }
            case StatementReturn ignored -> 0;
            case StatementSyntaxError ignored -> 0;
            case StatementVar sv -> sv.type.type().size();
            case StatementWhile stmt -> {
                int n = calcMaxLocalSize(stmt.body);
                yield n;
            }
        };
    }

    private void compile(Statement statement, Frame frame) {
        int lineNo = Token.getLineAndOffset(program.source, statement.start.start())[0];
        if (currentLineNo != lineNo) {
            code.append("@line ").append(lineNo).append('\n');
            currentLineNo = lineNo;
        }

        if (statement.getLabelAnchor() != null) {
            code.append(statement.getLabelAnchor().content()).append(":\n");
        }
        switch (statement) {
            case StatementBlock block -> compile(block, frame);
            case StatementBreak ignored -> compileBreak();
            case StatementContinue ignored -> compileContinue();
            case StatementDoWhile stmt -> compile(stmt, frame);
            case StatementExpression stmt -> compile(stmt, frame);
            case StatementFor stmt -> compileFor(stmt, frame);
            case StatementGoto stmt -> compileGoto(stmt);
            case StatementIf stmt -> compile(stmt, frame);
            case StatementReturn stmt -> compile(stmt, frame);
            case StatementSyntaxError stmt -> compile(stmt, frame);
            case StatementVar stmt -> compile(stmt, frame);
            case StatementWhile stmt -> compileWhile(stmt, frame);
        }
    }

    void compile(StatementBlock stmt, Frame frame) {
        var added = new HashSet<String>();
        for (Statement statement : stmt.statements) {
            if (statement instanceof StatementVar sv) {
                frame.add(sv.name(), sv.type.type().size());
                added.add(sv.name());
            }
            compile(statement, frame);
        }
        for (String s : added) {
            frame.remove(s);
        }
    }

    void compileBreak() {
        code.append("  j ").append(currentLoopBreakLabel.peek()).append("\n");
    }

    void compileContinue() {
        code.append("  j ").append(currentLoopContinueLabel.peek()).append("\n");
    }

    void compile(StatementDoWhile stmt, Frame frame) {
        int label = labelNo++;
        String head = "doWhile" + label;
        String cond = "doWhileCond" + label;
        String tail = "endDoWhile" + label;

        code.append(head).append(":\n");
        currentLoopContinueLabel.push(cond);
        currentLoopBreakLabel.push(tail);
        compile(stmt.body, frame);
        currentLoopContinueLabel.pop();
        currentLoopBreakLabel.pop();

        code.append(cond).append(":\n");
        compile(stmt.condition, frame, "t0");
        code.append("  eqi t0 1\n");
        code.append("  jnz ").append(head).append("\n");
        code.append(tail).append(":\n");
    }

    void compile(StatementExpression stmt, Frame frame) {
        code.append("# stmt expr\n");
        compile(stmt.expression, frame, "t0");
    }

    void compileFor(StatementFor stmt, Frame frame) {
        int label = labelNo++;
        String head = "for" + label;
        String cont = "forInc" + label;
        String tail = "endFor" + label;

        if (stmt.initExpression != null) {
            compile(stmt.initExpression, frame, "t0");
        } else if (stmt.initStatement != null) {
            frame.add(stmt.initStatement.name(), stmt.initStatement.type.type().size());
            compile(stmt.initStatement, frame);
        }

        code.append(head).append(":\n");
        compile(stmt.condition, frame, "t0");
        code.append("  eqi t0 0\n");
        code.append("  jnz ").append(tail).append("\n");

        currentLoopContinueLabel.push(stmt.inc == null ? head : cont);
        currentLoopBreakLabel.push(tail);
        compile(stmt.body, frame);
        currentLoopContinueLabel.pop();
        currentLoopBreakLabel.pop();

        if (stmt.inc != null) {
            code.append(cont).append(":\n");
            compile(stmt.inc, frame, "t0");
        }
        code.append("  j ").append(head).append("\n");
        code.append(tail).append(":\n");

        if (stmt.initStatement != null) {
            frame.remove(stmt.initStatement.name());
        }
    }

    void compileGoto(StatementGoto stmt) {
        code.append("  j ").append(stmt.label.content()).append("\n");
    }

    void compile(StatementIf stmt, Frame frame) {
        String elseLabel = "ifElse" + labelNo++;
        String endLabel = "endIf" + labelNo++;

        compile(stmt.condition, frame, "t0");
        code.append("  eqi t0 0\n");
        if (stmt.elseBody != null) {
            code.append("  jnz ").append(elseLabel).append("\n");
            compile(stmt.body, frame);
            code.append("  j ").append(endLabel).append('\n');
            code.append(elseLabel).append(":\n");
            compile(stmt.elseBody, frame);
        } else {
            code.append("  jnz ").append(endLabel).append("\n");
            compile(stmt.body, frame);
        }
        code.append(endLabel).append(":\n");
    }

    void compile(StatementReturn stmt, Frame frame) {
        if (stmt.value != null) {
            compile(stmt.value, frame, "rv");
        }
        code.append("  mov sp fp\n");
        code.append("  pop fp\n");
        code.append("  ret\n");
    }

    void compile(StatementVar stmt, Frame frame) {
        if (stmt.initValue != null) {
            code.append("# var ").append(stmt.name()).append('\n');
            compile(stmt.initValue, frame, "t0");
            var offset = frame.get(stmt.name());
            int varSize = stmt.type.type().size();
            if (varSize == 2) {
//                if (offset > 15) {
//                    String label = internConstant(-offset);
//                    code.append("  lw t3 ").append(label).append('\n');
//                } else {
//                    code.append("  li t3 ").append(-offset).append("\n");
//                }
                code.append("  swo t0 fp ").append(-offset).append("\n");
            } else {
                code.append("  mov t0 sp\n");
                code.append("  mov t1 fp\n");
                subRegister("t1", offset + varSize);
                code.append("  mcp t0 t1 ").append(varSize).append("\n");
                addRegister("sp", varSize);
            }
        }
    }

    void compileWhile(StatementWhile stmt, Frame frame) {
        int label = labelNo++;
        var headLabel = "while" + label;
        var tailLabel = "endWhile" + label;

        code.append(headLabel).append(":\n");
        compile(stmt.condition, frame, "t0");
        code.append("  eqi t0 0\n");
        code.append("  jnz ").append(tailLabel).append("\n");
        currentLoopContinueLabel.push(headLabel);
        currentLoopBreakLabel.push(tailLabel);
        compile(stmt.body, frame);
        currentLoopContinueLabel.pop();
        currentLoopBreakLabel.pop();
        code.append("  j ").append(headLabel).append("\n");
        code.append(tailLabel).append(":\n");

    }

    void compile(Expression expression, Frame frame, String dst) {
        int lineNo = Token.getLineAndOffset(program.source, expression.start.start())[0];
        if (currentLineNo != lineNo) {
            code.append("@line ").append(lineNo).append('\n');
            currentLineNo = lineNo;
        }
        switch (expression) {
            case ExpressionAccess expr -> compileAccess(expr, frame, dst);
            case ExpressionBin expr -> compileBin(expr, frame, dst);
            case ExpressionCall expr -> compileCall(expr, frame, dst);
            case ExpressionCast expr -> compileCast(expr, frame, dst);
            case ExpressionChar expr -> compileChar(expr, frame, dst);
            case ExpressionIdent expr -> compileIdent(expr, frame, dst);
            case ExpressionIndex expr -> compileIndex(expr, frame, dst);
            case ExpressionInteger expr -> compileInt(expr, frame, dst);
            case ExpressionParens expr -> compileParens(expr, frame, dst);
            case ExpressionPostfix expr -> compilePostfix(expr, frame, dst);
            case ExpressionPrefix expr -> compilePrefix(expr, frame, dst);
            case ExpressionString expr -> compileStr(expr, frame, dst);
            case ExpressionSyntaxError expr -> compileSyntaxError(expr, frame, dst);
            case ExpressionTernary expr -> compileTernary(expr, frame, dst);
            case ExpressionTypeError expr -> compileTypeError(expr, frame, dst);
            case ExpressionInitializer expr -> compileInitializer(expr, frame, dst);
        }
    }

    int getFieldOffset(SeaType.Struct struct, String field) {
        int off = 0;
        for (Map.Entry<String, SeaType> entry : struct.fields().entrySet()) {
            if (entry.getKey().equals(field)) {
                break;
            }
            off += entry.getValue().size();
        }
        return off;
    }

    void addRegister(String reg, int value) {
        assert value >= 0;
        if (value == 0) return;
        if (value > 15) {
            String tmp = reg.equals("t0") ? "t1" : "t0";
            code.append("  li ").append(tmp).append(" ").append(value).append('\n');
            code.append("  add ").append(reg).append(" t0").append('\n');
        } else {
            code.append("  inc ").append(reg).append(" ").append(value).append('\n');
        }
    }

    void subRegister(String reg, int value) {
        assert value >= 0;
        if (value == 0) return;
        if (value > 15) {
            String tmp = reg.equals("t0") ? "t1" : "t0";
            code.append("  li ").append(tmp).append(" ").append(value).append('\n');
            code.append("  sub ").append(reg).append(" ").append(tmp).append('\n');
        } else {
            code.append("  dec ").append(reg).append(" ").append(value).append('\n');
        }
    }

    void compileAccess(ExpressionAccess expr, Frame frame, String dst) {
        int fieldOffset = 0;
        Expression base = expr;
        while (base instanceof ExpressionAccess acc) {
            fieldOffset += getFieldOffset((SeaType.Struct) acc.value.type(), acc.prop.content());
            base = acc.value;
        }
        if (!(base instanceof ExpressionIdent ident)) {
            throw new UnsupportedOperationException("unimplemented");
        }

        int valueSize = expr.type().size();
        if (valueSize == 2) {
            if (globalLabels.containsKey(ident.name())) {
                var label = globalLabels.get(ident.name());
                code.append("  la t2 ").append(label).append('\n');
                if (dst == null) {
                    code.append("  lwo t0 t2 ").append(-fieldOffset).append('\n');
                    code.append("  push t0\n");
                } else {
                    code.append("  lwo ").append(dst).append(" ").append(-fieldOffset).append('\n');
                }
            } else {
                int offset = frame.get(ident.name());
                int off = -(offset + fieldOffset + valueSize);
//                code.append("  li t3 ").append().append('\n');
                if (dst == null) {
                    code.append("  lwo t0 fp ").append(off).append("\n");
                    code.append("  push t0\n");
                } else {
                    code.append("  lwo ").append(dst).append(" fp ").append(off).append("\n");
                }
            }
        } else {
            assert dst == null;
            if (globalLabels.containsKey(ident.name())) {
                var label = globalLabels.get(ident.name());
                code.append("  la t2 ").append(label).append('\n');
                addRegister("t2", fieldOffset);
            } else {
                var offset = frame.get(ident.name());
                code.append("  mov t2 fp\n");
                addRegister("t2", offset + fieldOffset + valueSize);
            }
            subRegister("sp", valueSize);
            code.append("  mcp t2 sp ").append(valueSize).append('\n');
        }
    }

    void compileBin(ExpressionBin bin, Frame frame, String dst) {
        switch (bin.op()) {
            case "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=" -> {
                int fieldOffset = 0;
                int valueSize = bin.lhs.type().size();
                LinkedList<String> nameParts = new LinkedList<>();
                Expression base = bin.lhs;
                while (base instanceof ExpressionAccess acc) {
                    nameParts.addFirst(acc.prop.content());
                    fieldOffset += getFieldOffset((SeaType.Struct) acc.value.type(), acc.prop.content());
                    base = acc.value;
                }
                if (fieldOffset > 0) {
                    fieldOffset += valueSize; // i dunno man
                }
                if (!(base instanceof ExpressionIdent id)) {
                    throw new UnsupportedOperationException("cannot compile non-ident/access");
                }

                code.append("# store ").append(String.join(".", nameParts)).append('\n');

                String name = id.name();
                if (valueSize == 2) {
                    if (bin.op().equals("=")) {
                        compile(bin.rhs, frame, "t0");
                    } else {
                        compile(id, frame, "t0");
                        compile(bin.rhs, frame, "t1");
                        switch (bin.op()) {
                            case "+=" -> code.append("  add t0 t1\n");
                            case "-=" -> code.append("  sub t0 t1\n");
                            case "*=" -> code.append("  mul t0 t1\n");
                            case "/=" -> code.append("  div t0 t1\n");
                            case "%=" -> code.append("  mod t0 t1\n");
                            case "&=" -> code.append("  and t0 t1\n");
                            case "|=" -> code.append("  or t0 t1\n");
                            case "^=" -> code.append("  xor t0 t1\n");
                            case "<<=" -> code.append("  lsh t0 t1\n");
                            case ">>=" -> code.append("  rsh t0 t1\n");
                            case String op -> throw new UnsupportedOperationException("I dunnot how to compile " + op);
                        }
                    }

                    if (globalLabels.containsKey(name)) {
                        code.append("  sw t0 ").append(globalLabels.get(name)).append('\n');
                    } else if (frame.hasParameter(name)) {
                        assert fieldOffset == 0;
                        var reg = frame.getParamRegister(name);
                        code.append("  mov ").append(reg).append(" t0\n");
                    } else {
                        var offset = frame.get(name);
//                        if (offset > 15) {
//                            String label = internConstant(-(offset + fieldOffset));
//                            code.append("  lw t1 ").append(label).append('\n');
//                        } else {
//                            code.append("  li t1 ").append(-(offset + fieldOffset)).append('\n');
//                        }
                        var off = -(offset + fieldOffset);
                        code.append("  swo t0 fp ").append(off).append("\n");
                    }
                } else {
                    assert bin.op().equals("=");
                    // TODO: large assignment
//                    compileLocation(bin.rhs, frame, "t2");
//
//                    if (globalLabels.containsKey(name)) {
//
//                    } else {
//                        var offset = frame.get(name);
//                        code.append("  mcp t2 t3 ").append(valueSize).append('\n');
//                    }
                }
            }
            case "<", ">", "<=", ">=", "==", "!=" -> {
                int label = labelNo++;
                var insr = dst == null ? "pushi" : "li " + dst;
                compileBranchingInstruction(
                        bin,
                        frame,
                        "cmpFalse" + label,
                        "cmpEnd" + label,
                        () -> code.append("  ").append(insr).append(" 1\n"),
                        () -> code.append("  ").append(insr).append(" 0\n")
                );
            }
            case "&&" -> {
                int label = labelNo++;
                var insr = dst == null ? "pushi" : "li " + dst;

                String falseLabel = "andFalse" + label;
                String endLabel = "andEnd" + label;

                compile(bin.lhs, frame, "t4");
                code.append("  eqi t4 0\n");
                code.append("  jnz ").append(falseLabel).append('\n');
                compile(bin.rhs, frame, "t4");
                code.append("  eqi t4 0\n");
                code.append("  jnz ").append(falseLabel).append('\n');
                code.append("  ").append(insr).append(" 1\n");
                code.append("  j ").append(endLabel).append('\n');
                code.append(falseLabel).append(":\n");
                code.append("  ").append(insr).append(" 0\n");
                code.append(endLabel).append(":\n");
            }
            case "||" -> {
                int label = labelNo++;
                var insr = dst == null ? "pushi" : "li " + dst;

                String trueLabel = "orTrue" + label;
                String endLabel = "orEnd" + label;

                compile(bin.lhs, frame, "t4");
                code.append("  eqi t4 0\n");
                code.append("  jz ").append(trueLabel).append('\n');
                compile(bin.rhs, frame, "t4");
                code.append("  eqi t4 0\n");
                code.append("  jz ").append(trueLabel).append('\n');
                code.append("  ").append(insr).append(" 0\n");
                code.append("  j ").append(endLabel).append('\n');
                code.append(trueLabel).append(":\n");
                code.append("  ").append(insr).append(" 1\n");
                code.append(endLabel).append(":\n");
            }
            default -> {
                compile(bin.lhs, frame, null);
                compile(bin.rhs, frame, null);
                code.append("  ");
                switch (bin.op()) {
                    case "+" -> code.append("sadd");
                    case "-" -> code.append("ssub");
                    case "*" -> code.append("smul");
                    case "/" -> code.append("sdiv");
                    case "%" -> code.append("smod");
                    case "&" -> code.append("sand");
                    case "|" -> code.append("sor");
                    case "^" -> code.append("sxor");
                    case "<<" -> code.append("sshl");
                    case ">>" -> code.append("sshr");
                    case String s -> throw new RuntimeException(" unimplemented, bin " + s);
                }
                code.append("\n");
                if (dst != null) {
                    code.append("  pop ").append(dst).append("\n");
                }
            }
        }
    }

    // TODO push/pop ra
    void compileCall(ExpressionCall expr, Frame frame, String dst) {
        if (expr.functor instanceof ExpressionIdent ident) {
            var func = program.symbols.get(ident.name());
            if (!(func.type instanceof SeaType.Func f)) {
                throw new UnsupportedOperationException("cannot invoke non-function");
            }

            int noParams = frame.parameters.size();
            for (int i = 0; i < noParams; i++) {
                code.append("  push a").append(i).append("\n");
            }
            code.append("  push ra\n");

            for (int i = 0; i < f.params().size(); i++) {
                compile(expr.args.get(i), frame, "a" + i);
            }
            if (f.isVararg()) {
                code.append("  mov a").append(f.params().size()).append(" sp\n"); // move the starting pointer as the last hidden argument
                for (int i = f.params().size(); i < expr.args.size(); i++) {
                    compile(expr.args.get(i), frame, null);
                }
            }

            code.append("  jal ").append(ident.name()).append("\n");
            code.append("  pop ra\n");
            for (int i = noParams - 1; i >= 0; i--) {
                code.append("  pop a").append(i).append("\n");
            }

            if (!f.resultType().isVoid()) {
                if (dst != null) {
                    if (!dst.equals("rv")) {
                        code.append("  mov ").append(dst).append(" rv\n");
                    }
                } else {
                    code.append("  push rv\n");
                }
            }

        } else {
            throw new RuntimeException("unimplemented");
        }
    }

    void compileCast(ExpressionCast ignoredExpr, Frame ignored, String ignoredDst) {
        throw new RuntimeException("unimplemented");
    }

    String internConstant(Object constant) {
        if (constant instanceof Character c) {
            constant = (int) c;
        }
        if (dataLabels.containsKey(constant)) {
            return dataLabels.get(constant);
        }

        String label;
        String valueStr;
        switch (constant) {
            case String s -> {
                label = "str" + Integer.toHexString(s.hashCode()).toUpperCase();
                valueStr = StringEscapeUtils.escapeString(s);
            }
            case Integer i -> {
                label = "num" + (i < 0 ? "_" + -i : i);
                valueStr = "" + i;
            }
            default ->
                    throw new UnsupportedOperationException("cannot compile constant " + constant.getClass().getName() + " : " + constant);
        }
        data.append("  ").append(label).append(": ").append(valueStr).append('\n');
        dataLabels.put(constant, label);
        return label;
    }

    void compileChar(ExpressionChar expr, Frame ignored, String dst) {
        var rune = (int) expr.content();
        if (rune >= 16) {
            String label = internConstant(rune);
            if (dst != null) {
                code.append("  lw ").append(dst).append(" ").append(label).append('\n');
            } else {
                code.append("  lw t0 ").append(label).append('\n');
                code.append("  spush\n");
            }
        } else if (dst == null) {
            code.append("  pushi ").append(rune).append("\n");
        } else {
            code.append("  li ").append(dst).append(" ").append(rune).append("\n");
        }
    }

    void compileIdent(ExpressionIdent ident, Frame frame, String dst) {
        code.append("# load ").append(ident.name()).append("\n");
        if (globalLabels.containsKey(ident.name())) {
            var label = globalLabels.get(ident.name());
            String insr = ident.type().isAPointerTo(SeaType.CHAR) ? "la" : "lw";
            if (dst == null) {
                code.append("  ").append(insr).append(" t0 ").append(label).append('\n');
                code.append("  push t0\n");
            } else {
                code.append("  ").append(insr).append(" ").append(dst).append(" ").append(label).append('\n');
            }
        } else if (frame.hasParameter(ident.name())) {
            String reg = frame.getParamRegister(ident.name());
            if (dst == null) {
                code.append("  push ").append(reg).append('\n');
            } else if (!dst.equals(reg)) {
                code.append("  mov ").append(dst).append(' ').append(reg).append('\n');
            }
        } else {
            var offset = frame.get(ident.name());
            if (dst == null) {
                code.append("  lwo t0 fp ").append(-offset).append('\n');
                code.append("  push t0\n");
            } else {
                code.append("  lwo ").append(dst).append(" fp ").append(-offset).append('\n');
            }
        }
    }

    void compileIndex(ExpressionIndex expr, Frame frame, String dst) {
        compile(expr.array, frame, null);
        compile(expr.index, frame, null);
        code.append("  pop t2\n"); // load the offset into t2
        if (expr.array.type().size() != 1) {
            code.append("  li t0 ").append(expr.array.type().size()).append('\n');
            code.append("  mul t2 t2 t0\n");
        }
        code.append("  pop t1\n"); // load the pointer into t1
        if (dst == null) {
            code.append("  lwr t0 t1 t2\n");
            code.append("  push t0\n");
        } else {
            code.append("  lwr ").append(dst).append(" t1 t2\n");
        }
    }

    void compileInt(ExpressionInteger expr, Frame ignored, String dst) {
        int val = expr.value;
        if (val >= 0 && val < 16) {
            if (dst == null) {
                code.append("  pushi ").append(val).append('\n');
            } else {
                code.append("  li ").append(dst).append(' ').append(val).append('\n');
            }
        } else {
            String label = internConstant(val);
            if (dst == null) {
                code.append("  lw t0 ").append(label).append('\n');
                code.append("  push t0\n");
            } else {
                code.append("  lw ").append(dst).append(" ").append(label).append('\n');
            }
        }
    }

    void compileParens(ExpressionParens expr, Frame frame, String dst) {
        compile(expr.inner, frame, dst);
    }

    // a[i]++
    // ++a[i]
    // load a[i]
    // (hook) : value is on the stack, do whatever, leave (stack-value, assign-value)
    // write a[i] from stack?
    void compilePostfix(ExpressionPostfix expr, Frame frame, String dst) {
        compile(expr.inner, frame, null);
        switch (expr.op()) {
//            case "++" -> {
//                compileWriteHooked(expr.inner, () -> {
//                    code.append("  sdup\n");
//                    code.append("  pop t0\n");
//                    code.append("  li t1 1\n");
//                    code.append("  sub t0 t0 t1\n");
//                });
//            }
//            case "--" -> {
//
//            }
            case String op -> throw new UnsupportedOperationException("don't know how to compile " + op);
        }
    }

    void compilePrefix(ExpressionPrefix expr, Frame frame, String dst) {
        switch (expr.op()) {
            case "!" -> {
                compile(expr.inner, frame, "t4");
                String nt = "nottrue" + labelNo++;
                String end = "endnot" + labelNo++;
                String insr = dst == null ? "pushi" : "li " + dst;
                code.append("  eqi t4 0\n");
                code.append("  jnz ").append(nt).append('\n');
                code.append("  ").append(insr).append(" 0\n");
                code.append("  j ").append(end).append('\n');
                code.append(nt).append(":\n");
                code.append("  ").append(insr).append(" 1\n");
                code.append(end).append(":\n");
            }
            case "-" -> {
                compile(expr.inner, frame, dst);
                if (dst == null) {
                    code.append("  sneg\n");
                } else {
                    code.append("  neg ").append(dst).append('\n');
                }
            }
            case String op -> throw new UnsupportedOperationException("cannot compile: " + op);
        }
    }

    void compileStr(ExpressionString expr, Frame frame, String dst) {
        String content = expr.content();
        String label = internConstant(content);
        if (dst == null) {
            code.append("  la t0 ").append(label).append('\n');
            code.append("  push t0\n");
        } else {
            code.append("  la ").append(dst).append(' ').append(label).append('\n');
        }
    }

    void compileSyntaxError(ExpressionSyntaxError expr, Frame frame, String dst) {
        throw new UnsupportedOperationException("cannot compile errors");
    }

    void compileTernary(ExpressionTernary expr, Frame frame, String dst) {
        int label = labelNo++;
        compileBranchingInstruction(
                expr.cond,
                frame,
                "ternaryElse" + label,
                "ternaryEnd" + label,
                () -> compile(expr.then, frame, null),
                () -> compile(expr.otherwise, frame, null)
        );
    }

    void compileBranchingInstruction(
            Expression expression,
            Frame frame,
            String falseLabel,
            String endLabel,
            Runnable trueBlock,
            Runnable falseBlock
    ) {
        compileBranchingInstruction(expression, frame, falseLabel, endLabel, trueBlock, falseBlock, false);
    }

    private final static List<String> BRANCHING_BIN_INSRS = List.of("<", ">", "<=", ">=", "==", "!=");

    void compileBranchingInstruction(
            Expression expression,
            Frame frame,
            String condFailedLabel,
            String endLabel,
            Runnable trueBlock,
            Runnable falseBlock,
            boolean negated
    ) {
        if (expression instanceof ExpressionBin bin) {
            if (BRANCHING_BIN_INSRS.contains(bin.op())) {
                compile(bin.lhs, frame, "t4");
                compile(bin.rhs, frame, "t5");
                String insr = switch (bin.op()) {
                    case "<" -> "lt";
                    case ">" -> "gt";
                    case "<=" -> "lte";
                    case ">=" -> "gte";
                    case "==" -> "eq";
                    case "!=" -> "neq";
                    default -> throw new IllegalStateException("Unexpected value: " + bin.op());
                };
                code.append("  ").append(insr).append(" t4 t5\n");
                String jump = negated ? "jnz" : "jz";
                code.append("  ").append(jump).append(" ").append(condFailedLabel).append('\n');
                trueBlock.run();
                if (falseBlock != null) {
                    code.append("  j ").append(endLabel).append('\n');
                    code.append(condFailedLabel).append(":\n");
                    falseBlock.run();
                    code.append(endLabel).append(":\n");
                }
                return;
            }

            if (bin.op().equals("&&")) {
                // we're essentially applying De Morgan's Law here, this is `!a || !b`
                compileBranchingInstruction(
                        bin.lhs,
                        frame,
                        condFailedLabel,
                        endLabel,
                        () -> {
                            compileBranchingInstruction(
                                    bin.rhs,
                                    frame,
                                    condFailedLabel,
                                    endLabel,
                                    trueBlock,
                                    null,
                                    false
                            );
                        },
                        falseBlock,
                        negated
                );
                return;
            }
        }

        if (expression instanceof ExpressionParens p) {
            compileBranchingInstruction(p.inner, frame, condFailedLabel, endLabel, trueBlock, falseBlock, negated);
            return;
        }

        String insr = negated ? "jz" : "jnz";
        compile(expression, frame, "t4");
        code.append("  eqi t4 0\n");
        code.append("  ").append(insr).append(" ").append(condFailedLabel).append('\n');
        trueBlock.run();
        if (falseBlock != null) {
            code.append("  j ").append(endLabel).append('\n');
            code.append(condFailedLabel).append(":\n");
            falseBlock.run();
            code.append(endLabel).append(":\n");
        }
    }

    void compileTypeError(ExpressionTypeError expr, Frame frame, String dst) {
        throw new UnsupportedOperationException();
    }

    void compileInitializer(ExpressionInitializer expr, Frame frame, String dst) {
        for (var value : expr.values) {
            compile(value, frame, null);
        }
    }

    protected void compile(DeclarationTypedef ignored) {
    }

    protected void compile(DeclarationVar item) throws CompilationException {
        Object value = null;
        if (item.initializer != null) {
            value = evalConstant(item.initializer);
        }
        var label = internConstant(value);
        globalLabels.put(item.name.content(), label);
    }

    protected void compile(DeclarationStruct struct) throws CompilationException {

    }

    protected Object evalConstant(Expression expression) throws CompilationException {
        return switch (expression) {
            case ExpressionBin bin -> {
                var lhs = evalConstant(bin.lhs);
                var rhs = evalConstant(bin.rhs);
                yield switch (bin.op.content()) {
                    case "+" -> ((int) lhs) + ((int) rhs);
                    case "-" -> ((int) lhs) - ((int) rhs);
                    case "*" -> ((int) lhs) * ((int) rhs);
                    case "/" -> ((int) lhs) / ((int) rhs);
                    case "%" -> ((int) lhs) % ((int) rhs);
                    case ">>" -> ((int) lhs) >> ((int) rhs);
                    case "<<" -> ((int) lhs) << ((int) rhs);
                    case "&" -> ((int) lhs) & ((int) rhs);
                    case "|" -> ((int) lhs) | ((int) rhs);
                    case "^" -> ((int) lhs) ^ ((int) rhs);
                    case "==" -> ((int) lhs) == ((int) rhs);
                    case "!=" -> ((int) lhs) != ((int) rhs);
                    case "<" -> ((int) lhs) < ((int) rhs);
                    case "<=" -> ((int) lhs) <= ((int) rhs);
                    case ">" -> ((int) lhs) > ((int) rhs);
                    case ">=" -> ((int) lhs) >= ((int) rhs);
                    case String op -> throw new UnsupportedOperationException("invalid constant operator: " + op);
                };
            }
            case ExpressionCall call ->
                    throw new CompilationException("invalid constant expression: function call are not allowed in constant contexts", call.functor.span());
            case ExpressionCast cast ->
                    throw new CompilationException("invalid constant expression: casting is not allowed in constant contexts", cast.type.span());
            case ExpressionChar chr -> (int) chr.content();
            case ExpressionIdent ident ->
                    throw new CompilationException("invalid constant expression: identifiers are not allowed in constant contexts", (ident).span());
            case ExpressionIndex index ->
                    throw new CompilationException("invalid constant expression: indexing is not allowed in constant contexts", (index.index).span());
            case ExpressionInteger exprInt -> exprInt.value;
            case ExpressionParens group -> evalConstant(group.inner);
            case ExpressionPostfix postfix -> {
                switch (postfix.op()) {
                    case "++" ->
                            throw new CompilationException("invalid constant expression: postfix increment is not allowed in constant contexts", postfix.span());
                    case "--" ->
                            throw new CompilationException("invalid constant expression: postfix decrement is not allowed in constant contexts", postfix.span());
                    case String op -> throw new UnsupportedOperationException("unknown postfix operator " + op);
                }
            }
            case ExpressionPrefix prefix -> switch (prefix.op()) {
                case "++" ->
                        throw new CompilationException("invalid constant expression: prefix increment is not allowed in constant contexts", prefix.span());
                case "--" ->
                        throw new CompilationException("invalid constant expression: prefix decrement is not allowed in constant contexts", prefix.span());
                case "&" ->
                        throw new CompilationException("invalid constant expression: addressing is not allowed in constant contexts", prefix.span());
                case "*" ->
                        throw new CompilationException("invalid constant expression: dereference is not allowed in constant contexts", prefix.span());
                case "~" -> {
                    var value = evalConstant(prefix.inner);
                    yield ~((Integer) value);
                }
                case "-" -> {
                    var value = evalConstant(prefix.inner);
                    yield -((Integer) value);
                }
                case "!" -> {
                    var value = evalConstant(prefix.inner);
                    yield value.equals(0);
                }
                case "sizeof" -> {
                    var value = evalConstant(prefix.inner);
                    yield switch (value) {
                        case String s -> s.length();
                        case Integer i -> 2;
                        case Object v ->
                                throw new CompilationException("cannot take sizeof " + v.getClass(), prefix.span());
                    };
                }
                default -> throw new IllegalStateException("Unexpected value: " + prefix.op());
            };
            case ExpressionString str -> str.content();
            case ExpressionSyntaxError ignored ->
                    throw new UnsupportedOperationException("errors should be checked before compilation");
            case ExpressionTernary expressionTernary -> {
                var cond = evalConstant(expressionTernary.cond);
                var condV = switch (cond) {
                    case Integer i -> i != 0;
                    default -> throw new UnsupportedOperationException("expected integer value");
                };

                if (condV) {
                    yield evalConstant(expressionTernary.then);
                } else {
                    yield evalConstant(expressionTernary.otherwise);
                }
            }
            default ->
                    throw new UnsupportedOperationException("invalid constant expression: " + expression.getClass().getSimpleName());
        };
    }

    static class Frame {
        private final LinkedHashMap<String, DeclarationFunc.Param> parameters = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> paramRegisters = new LinkedHashMap<>();
        private final LinkedHashMap<String, Integer> offsets = new LinkedHashMap<>();
        private int totalSize = 0;

        Frame(DeclarationFunc.ParamList params) {
            int i = 0;
            for (var param : params.params()) {
                int typeSize = param.type.type().size();
                assert typeSize == 2;
                parameters.put(param.name.content(), param);
                paramRegisters.put(param.name.content(), "a" + i);
                i += 1;
            }
        }

        boolean hasParameter(String name) {
            return parameters.containsKey(name);
        }

        String getParamRegister(String name) {
            return paramRegisters.get(name);
        }

        int get(String name) {
            return offsets.get(name);
        }

        void add(String name, int size) {
            offsets.put(name, totalSize);
            totalSize += size;
        }

        void remove(String name) {
            int size = offsets.get(name);
            totalSize -= size;
        }
    }
}
