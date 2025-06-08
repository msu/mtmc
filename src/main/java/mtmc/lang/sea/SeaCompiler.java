package mtmc.lang.sea;

import mtmc.asm.Assembler;
import mtmc.lang.CompilationException;
import mtmc.lang.Span;
import mtmc.lang.sea.ast.*;
import mtmc.os.exec.Executable;
import mtmc.util.StringEscapeUtils;

import java.util.*;

public class SeaCompiler {
    protected Unit program;

    private StringBuilder data = new StringBuilder();
    private StringBuilder code = new StringBuilder();
    private HashMap<String, String> globalLabels = new HashMap<>();
    private HashMap<Object, String> dataLabels = new HashMap<>();

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
        // max 4 args, a0-a3
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
        if (!data.isBlank()) {
            asm += ".data\n" + data;
        }

        asm += '\n';
        asm += ".text\n";
        asm += """
                mov fp sp
                jal main
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
        }
    }

    // the tail label of the current loop
    Stack<String> currentLoopTailLabel = new Stack<>();
    Stack<String> currentLoopHeadLabel = new Stack<>();
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
        code.append("  push ra\n");
        code.append("  push fp\n");
        code.append("  mov fp sp\n");

        var paramOffset = 0;
        var frameOffsets = new HashMap<String, Integer>();
        int i = 0;
        for (DeclarationFunc.Param param : func.params.params()) {
            frameOffsets.put(param.name.content(), paramOffset);
            paramOffset += 2;
            code.append("  push a").append(i++).append("\n");
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

        compile(func.body, frameOffsets);
        labelNo = 0;

        // TODO: dangling return
    }

    private int calcMaxLocalSize(Statement body) {
        return switch (body) {
            case StatementBlock statementBlock -> {
                int vars = 0;
                int temps = 0;
                for (var child : statementBlock.statements) {
                    if (child instanceof StatementVar) {
                        vars += 2;
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
            case StatementVar ignored -> 2;
            case StatementWhile stmt -> {
                int n = calcMaxLocalSize(stmt.body);
                yield n;
            }
        };
    }

    private void compile(Statement statement, Map<String, Integer> frameOffsets) {
        if (statement.getLabelAnchor() != null) {
            code.append(statement.getLabelAnchor().content()).append(":\n");
        }
        switch (statement) {
            case StatementBlock block -> compile(block, frameOffsets);
            case StatementBreak ignored -> compileBreak();
            case StatementContinue ignored -> compileContinue();
            case StatementDoWhile stmt -> compile(stmt, frameOffsets);
            case StatementExpression stmt -> compile(stmt, frameOffsets);
            case StatementFor stmt -> compile(stmt, frameOffsets);
            case StatementGoto stmt -> compileGoto(stmt);
            case StatementIf stmt -> compile(stmt, frameOffsets);
            case StatementReturn stmt -> compile(stmt, frameOffsets);
            case StatementSyntaxError stmt -> compile(stmt, frameOffsets);
            case StatementVar stmt -> compile(stmt, frameOffsets);
            case StatementWhile stmt -> compile(stmt, frameOffsets);
        }
    }

    void compile(StatementBlock stmt, Map<String, Integer> frameOffsets) {
        var added = new HashSet<String>();
        for (Statement statement : stmt.statements) {
            if (statement instanceof StatementVar sv) {
                frameOffsets.put(sv.name(), 2 * frameOffsets.size());
                added.add(sv.name());
            }
            compile(statement, frameOffsets);
        }
        for (String s : added) {
            frameOffsets.remove(s);
        }
    }

    void compileBreak() {
        code.append("  j ").append(currentLoopTailLabel).append("\n");
    }

    void compileContinue() {
        code.append("  j ").append(currentLoopHeadLabel).append("\n");
    }

    void compile(StatementDoWhile stmt, Map<String, Integer> frameOffsets) {
        currentLoopHeadLabel.push("dw" + labelNo++);
        currentLoopTailLabel.push("enddw" + labelNo++);

        code.append(currentLoopHeadLabel).append(":\n");
        compile(stmt.body, frameOffsets);

        compile(stmt.condition, frameOffsets, "t1");
        code.append("  li t0 0\n");
        code.append("  neq t1 t0\n");
        code.append("  jnz ").append(currentLoopHeadLabel).append("\n");
        code.append(currentLoopTailLabel).append(":\n");
        currentLoopHeadLabel.pop();
        currentLoopTailLabel.pop();
    }

    void compile(StatementExpression stmt, Map<String, Integer> frameOffsets) {
        code.append("# stmt expr\n");
        compile(stmt.expression, frameOffsets, "t0");
    }

    void compile(StatementFor stmt, Map<String, Integer> frameOffsets) {
        currentLoopHeadLabel.push("for" + labelNo++);
        currentLoopTailLabel.push("endfor" + labelNo++);

        if (stmt.initExpression != null) compile(stmt.initExpression, frameOffsets, "t0");
        else if (stmt.initStatement != null) compile(stmt.initStatement, frameOffsets);

        code.append(currentLoopHeadLabel).append(":\n");
        compile(stmt.condition, frameOffsets, "t1");
        code.append("  li t0 0\n");
        code.append("  neq t1 t0\n");
        code.append("  jz ").append(currentLoopTailLabel).append("\n");
        compile(stmt.body, frameOffsets);
        compile(stmt.inc, frameOffsets, "t0");
        code.append("  j ").append(currentLoopHeadLabel).append("\n");
        code.append(currentLoopTailLabel).append(":\n");

        currentLoopHeadLabel.pop();
        currentLoopTailLabel.pop();
    }

    void compileGoto(StatementGoto stmt) {
        code.append("  j ").append(stmt.label.content()).append("\n");
    }

    void compile(StatementIf stmt, Map<String, Integer> frameOffsets) {
        String elseLabel = "ifElse" + labelNo++;
        String endLabel = "endif" + labelNo++;

        compile(stmt.condition, frameOffsets, "t1");
        code.append("  li t0 0\n");
        code.append("  neq t1 t0\n");
        if (stmt.elseBody != null) {
            code.append("  jz ").append(elseLabel).append("\n");
            compile(stmt.body, frameOffsets);
            code.append("  j ").append(endLabel).append('\n');
            code.append(elseLabel).append(":\n");
            compile(stmt.elseBody, frameOffsets);
        } else {
            code.append("  jz ").append(endLabel).append("\n");
            compile(stmt.body, frameOffsets);
        }
        code.append(endLabel).append(":\n");
    }

    void compile(StatementReturn stmt, Map<String, Integer> frameOffsets) {
        if (stmt.value != null) {
            compile(stmt.value, frameOffsets, "rv");
        }
        code.append("  mov sp fp\n");
        code.append("  pop fp\n");
        code.append("  pop ra\n");
        code.append("  ret\n");
    }

    void compile(StatementVar stmt, Map<String, Integer> frameOffsets) {
        if (stmt.initValue != null) {
            code.append("# var ").append(stmt.name()).append('\n');
            compile(stmt.initValue, frameOffsets, "t0");
            // TODO: sizeof thingy
            var offset = frameOffsets.get(stmt.name());
            if (offset > 15) {
                String label = internConstant(-offset);
                code.append("  lw t3 ").append(label).append('\n');
            } else {
                code.append("  li t3 ").append(-offset).append("\n");
            }
            code.append("  swr t0 fp t3\n");
        }
    }

    void compile(StatementWhile stmt, Map<String, Integer> frameOffsets) {
        var headLabel = "while" + labelNo++;
        var tailLabel = "endwhile" + labelNo++;
        currentLoopHeadLabel.push(headLabel);
        currentLoopTailLabel.push(tailLabel);

        code.append(headLabel).append(":\n");
        compile(stmt.condition, frameOffsets, "t1");
        code.append("  li t0 0\n");
        code.append("  neq t1 t0\n");
        code.append("  jz ").append(tailLabel).append("\n");
        compile(stmt.body, frameOffsets);
        code.append("  j ").append(headLabel).append("\n");
        code.append(tailLabel).append(":\n");

        currentLoopHeadLabel.pop();
        currentLoopTailLabel.pop();
    }

    void compile(Expression expression, Map<String, Integer> frameOffsets, String dst) {
        switch (expression) {
            case ExpressionAccess expr -> compileAccess(expr, frameOffsets, dst);
            case ExpressionBin expr -> compileBin(expr, frameOffsets, dst);
            case ExpressionCall expr -> compileCall(expr, frameOffsets, dst);
            case ExpressionCast expr -> compileCast(expr, frameOffsets, dst);
            case ExpressionChar expr -> compileChar(expr, frameOffsets, dst);
            case ExpressionIdent expr -> compileIdent(expr, frameOffsets, dst);
            case ExpressionIndex expr -> compileIndex(expr, frameOffsets, dst);
            case ExpressionInteger expr -> compileInt(expr, frameOffsets, dst);
            case ExpressionParens expr -> compileParens(expr, frameOffsets, dst);
            case ExpressionPostfix expr -> compilePostfix(expr, frameOffsets, dst);
            case ExpressionPrefix expr -> compilePrefix(expr, frameOffsets, dst);
            case ExpressionString expr -> compileStr(expr, frameOffsets, dst);
            case ExpressionSyntaxError expr -> compileSyntaxError(expr, frameOffsets, dst);
            case ExpressionTernary expr -> compileTernary(expr, frameOffsets, dst);
            case ExpressionTypeError expr -> compileTypeError(expr, frameOffsets, dst);
        }
    }


    void compileAccess(ExpressionAccess expr, Map<String, Integer> frameOffsets, String dst) {
        throw new RuntimeException("unimplemented");
    }

    void compileBin(ExpressionBin expr, Map<String, Integer> frameOffsets, String dst) {
        switch (expr.op()) {
            case "<", ">", "<=", ">=", "==", "!=" -> {
                int label = labelNo++;
                var insr = dst == null ? "pushi" : "li " + dst;
                compileBranchingInstruction(
                        expr,
                        frameOffsets,
                        "cmpFalse" + label,
                        "cmpEnd" + label,
                        () -> code.append("  ").append(insr).append(" 1\n"),
                        () -> code.append("  ").append(insr).append(" 0\n")
                );
            }
            case "&&" -> {
                int label = labelNo++;
                var insr = dst == null ? "pushi" : "li " + dst;
                compileBranchingInstruction(
                        expr,
                        frameOffsets,
                        "andFalse" + label,
                        "andEnd" + label,
                        () -> code.append("  ").append(insr).append(" 1\n"),
                        () -> code.append("  ").append(insr).append(" 0\n")
                );
            }
            case "||" -> {
                int label = labelNo++;
                var insr = dst == null ? "pushi" : "li " + dst;
                compileBranchingInstruction(
                        expr,
                        frameOffsets,
                        "orElse" + label,
                        "orEnd" + label,
                        () -> code.append("  ").append(insr).append(" 1\n"),
                        () -> code.append("  ").append(insr).append(" 0\n")
                );
            }
            default -> {
                compile(expr.lhs, frameOffsets, null);
                compile(expr.rhs, frameOffsets, null);
                code.append("  ");
                switch (expr.op()) {
                    case "+" -> code.append("sadd");
                    case "-" -> code.append("ssub");
                    case "*" -> code.append("smul");
                    case "/" -> code.append("sdiv");
                    case "%" -> code.append("srem");
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

    void compileCall(ExpressionCall expr, Map<String, Integer> frameOffsets, String dst) {
        if (expr.functor instanceof ExpressionIdent ident) {
            var func = program.symbols.get(ident.name());
            if (!(func.type() instanceof SeaType.Func f)) {
                throw new UnsupportedOperationException("cannot invoke non-function");
            }

            for (int i = 0; i < f.params().size(); i++) {
                compile(expr.args.get(i), frameOffsets, "a" + i);
            }
            code.append("  mov a").append(f.params().size()).append(" sp\n"); // move the starting pointer as the last hidden argument
            for (int i = f.params().size(); i < expr.args.size(); i++) {
                compile(expr.args.get(i), frameOffsets, null);
            }

            code.append("  jal ").append(ident.name()).append("\n");

            if (!func.type().resultType().isVoid()) {
                if (dst != null) {
                    code.append("  mov ").append(dst).append(" rv\n");
                } else {
                    code.append("  push rv\n");
                }
            }

        } else {
            throw new RuntimeException("unimplemented");
        }
    }

    void compileCast(ExpressionCast ignoredExpr, Map<String, Integer> ignoredFrameOffsets, String ignoredDst) {
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

    void compileChar(ExpressionChar expr, Map<String, Integer> ignoredFrameOffsets, String dst) {
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

    void compileIdent(ExpressionIdent ident, Map<String, Integer> frameOffsets, String dst) {
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
        } else {
            var offset = frameOffsets.get(ident.name());
            code.append("  li t3 ").append(-offset).append("\n");
            if (dst == null) {
                code.append("  lwr t0 fp t3\n");
                code.append("  push t0\n");
            } else {
                code.append("  lwr ").append(dst).append(" fp t3\n");
            }
        }
    }

    void compileIndex(ExpressionIndex expr, Map<String, Integer> frameOffsets, String dst) {
        compile(expr.array, frameOffsets, null);
        compile(expr.index, frameOffsets, null);
        code.append("  pop t1\n"); // load the offset into t1
        if (expr.array.type().size() != 1) {
            code.append("  li t0 ").append(expr.array.type().size()).append('\n');
            code.append("  mul t1 t1 t0\n");
        }
        code.append("  pop t0\n"); // load the pointer into t0
        if (dst == null) {
            code.append("  lwr t2 t0 t1\n");
            code.append("  push t2\n");
        } else {
            code.append("  lwr ").append(dst).append(" t0 t1\n");
        }
    }

    void compileInt(ExpressionInteger expr, Map<String, Integer> ignored, String dst) {
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

    void compileParens(ExpressionParens expr, Map<String, Integer> frameOffsets, String dst) {
        compile(expr.inner, frameOffsets, dst);
    }

    // a[i]++
    // ++a[i]
    // load a[i]
    // (hook) : value is on the stack, do whatever, leave (stack-value, assign-value)
    // write a[i] from stack?
    void compilePostfix(ExpressionPostfix expr, Map<String, Integer> frameOffsets, String dst) {
        compile(expr.inner, frameOffsets, null);
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

    void compilePrefix(ExpressionPrefix expr, Map<String, Integer> frameOffsets, String dst) {
        switch (expr.op()) {
            case "!" -> {
                compile(expr.inner, frameOffsets, "t4");
                String nt = "nottrue" + labelNo++;
                String end = "endnot" + labelNo++;
                String insr = dst == null ? "pushi" : "li " + dst;
                code.append("  li t5 0\n");
                code.append("  neq t4 t5\n");
                code.append("  jz ").append(nt).append('\n');
                code.append("  ").append(insr).append(" 0\n");
                code.append("  j ").append(end).append('\n');
                code.append(nt).append(":\n");
                code.append("  ").append(insr).append(" 1\n");
                code.append(end).append(":\n");
            }
            case "-" -> {
                compile(expr.inner, frameOffsets, dst);
                if (dst == null) {
                    code.append("  sneg\n");
                } else {
                    code.append("  neg ").append(dst).append('\n');
                }
            }
            case String op -> throw new UnsupportedOperationException("cannot compile: " + op);
        }
    }

    void compileStr(ExpressionString expr, Map<String, Integer> frameOffsets, String dst) {
        String content = expr.content();
        String label = internConstant(content);
        if (dst == null) {
            code.append("  la t0 ").append(label).append('\n');
            code.append("  push t0\n");
        } else {
            code.append("  la ").append(dst).append(' ').append(label).append('\n');
        }
    }

    void compileSyntaxError(ExpressionSyntaxError expr, Map<String, Integer> frameOffsets, String dst) {
        throw new UnsupportedOperationException("cannot compile errors");
    }

    void compileTernary(ExpressionTernary expr, Map<String, Integer> frameOffsets, String dst) {
        int label = labelNo++;
        compileBranchingInstruction(
                expr.cond,
                frameOffsets,
                "ternaryElse" + label,
                "ternaryEnd" + label,
                () -> compile(expr.then, frameOffsets, null),
                () -> compile(expr.otherwise, frameOffsets, null)
        );
    }

    void compileBranchingInstruction(
            Expression expression,
            Map<String, Integer> frameOffsets,
            String falseLabel,
            String endLabel,
            Runnable then,
            Runnable other
    ) {
        compileBranchingInstruction(expression, frameOffsets, falseLabel, endLabel, then, other, false);
    }

    private final static List<String> BRANCHING_BIN_INSRS = List.of("<", ">", "<=", ">=", "==", "!=");

    void compileBranchingInstruction(
            Expression expression,
            Map<String, Integer> frameOffsets,
            String falseLabel,
            String endLabel,
            Runnable then,
            Runnable other,
            boolean negated
    ) {
        if (expression instanceof ExpressionBin bin) {
            if (BRANCHING_BIN_INSRS.contains(bin.op())) {
                compile(bin.lhs, frameOffsets, "t4");
                compile(bin.rhs, frameOffsets, "t5");
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
                code.append("  ").append(jump).append(" ").append(falseLabel).append('\n');
                then.run();
                if (other != null) {
                    code.append("  j ").append(endLabel).append('\n');
                    code.append(falseLabel).append(":\n");
                    other.run();
                    code.append(endLabel).append(":\n");
                }
                return;
            }

            if (bin.op().equals("&&")) {
                // we're essentially applying De Morgan's Law here, this is `!a || !b`
                compileBranchingInstruction(
                        bin.lhs,
                        frameOffsets,
                        falseLabel,
                        endLabel,
                        () -> {
                            compileBranchingInstruction(
                                    bin.rhs,
                                    frameOffsets,
                                    falseLabel,
                                    endLabel,
                                    then,
                                    null,
                                    false
                            );
                        },
                        other,
                        negated
                );
                return;
            }

            if (bin.op().equals("||")) {
                compileBranchingInstruction(
                        bin.lhs,
                        frameOffsets,
                        falseLabel,
                        endLabel,
                        () -> {
                            compileBranchingInstruction(
                                    bin.rhs,
                                    frameOffsets,
                                    falseLabel,
                                    endLabel,
                                    other,
                                    null,
                                    !negated
                            );
                        },
                        then,
                        !negated
                );
                return;
            }
        } else if (expression instanceof ExpressionPrefix pfx) {
            if (pfx.op().equals("!")) {
                compileBranchingInstruction(
                        pfx.inner,
                        frameOffsets,
                        falseLabel,
                        endLabel,
                        then,
                        other,
                        !negated
                );
                return;
            }
        } else if (expression instanceof ExpressionParens p) {
            compileBranchingInstruction(p.inner, frameOffsets, falseLabel, endLabel, then, other, negated);
        }

        String insr = negated ? "jnz" : "jz";
        compile(expression, frameOffsets, "t4");
        code.append("  li t5 0\n");
        code.append("  neq t4 t5\n");
        code.append("  ").append(insr).append(" ").append(falseLabel).append('\n');
        then.run();
        if (other != null) {
            code.append("  j ").append(endLabel).append('\n');
            code.append(falseLabel).append(":\n");
            other.run();
            code.append(endLabel).append(":\n");
        }
    }

    void compileTypeError(ExpressionTypeError expr, Map<String, Integer> frameOffsets, String dst) {
        throw new UnsupportedOperationException();
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
}
