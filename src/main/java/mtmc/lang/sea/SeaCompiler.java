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
    private HashMap<Object, String> dataLabels = new HashMap<>();

    public SeaCompiler(Unit program) {
        this.program = program;
    }

    public Executable compile() throws CompilationException {
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

        System.out.println("generate the following assembly:");
        System.out.println("=".repeat(20));
        System.out.println(asm);
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
            case "putc" -> {
                if (!func.returnType.type().isVoid()) {
                    throw new CompilationException("'putc' must return void", func.returnType.span());
                }
                if (func.params.size() != 1 || !func.params.getFirst().type.type().isChar()) {
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
                if (func.params.size() != 1 || !func.params.getFirst().type.type().isAPointerTo(SeaType.CHAR)) {
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
                if (func.params.size() != 1 || !func.params.getFirst().type.type().isInt()) {
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
        for (DeclarationFunc.Param param : func.params) {
            frameOffsets.put(param.name.content(), paramOffset);
            paramOffset += 2;
            code.append("  push a").append(i++).append("\n");
        }

        int maxLocals = calcMaxLocals(func.body);
        if (maxLocals > 0) {
            code.append("  dec sp ").append(2 * maxLocals).append("\n");
        }

        compile(func.body, frameOffsets);
        labelNo = 0;

        // TODO: dangling return
    }

    private int calcMaxLocals(Statement body) {
        return switch (body) {
            case StatementBlock statementBlock -> {
                int vars = 0;
                int temps = 0;
                for (var child : statementBlock.statements) {
                    if (child instanceof StatementVar) {
                        vars += 1;
                    } else {
                        int r = calcMaxLocals(child);
                        temps = Math.max(temps, r);
                    }
                }
                yield vars + temps;
            }
            case StatementBreak ignored -> 0;
            case StatementContinue ignored -> 0;
            case StatementDoWhile stmt -> {
                int n = calcMaxLocals(stmt.body);
                yield n;
            }
            case StatementExpression ignored -> 0;
            case StatementFor stmt -> {
                int n = 0;
                if (stmt.initStatement != null) n += calcMaxLocals(stmt.initStatement);
                n += calcMaxLocals(stmt.body);
                yield n;
            }
            case StatementGoto ignored -> 0;
            case StatementIf statementIf -> {
                int t = calcMaxLocals(statementIf.body);
                int e = calcMaxLocals(statementIf.elseBody);
                yield Math.max(t, e);
            }
            case StatementReturn ignored -> 0;
            case StatementSyntaxError ignored -> 0;
            case StatementVar ignored -> 1;
            case StatementWhile stmt -> {
                int n = calcMaxLocals(stmt.body);
                yield n;
            }
        };
    }

    private void compile(Statement statement, Map<String, Integer> frameOffsets) {
        if (statement.getLabelAnchor() != null) {
            code.append("  ").append(statement.getLabelAnchor().content()).append(": nop\n");
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

        code.append("  ").append(currentLoopHeadLabel).append(": nop\n");
        compile(stmt.body, frameOffsets);

        compile(stmt.condition, frameOffsets, "t1");
        code.append("  li t0 0\n");
        code.append("  neq t1 t0\n");
        code.append("  jnz ").append(currentLoopHeadLabel).append("\n");
        code.append(currentLoopTailLabel).append(": nop\n");
        currentLoopHeadLabel.pop();
        currentLoopTailLabel.pop();
    }

    void compile(StatementExpression stmt, Map<String, Integer> frameOffsets) {
        compile(stmt.expression, frameOffsets, "t0");
    }

    void compile(StatementFor stmt, Map<String, Integer> frameOffsets) {
        currentLoopHeadLabel.push("for" + labelNo++);
        currentLoopTailLabel.push("endfor" + labelNo++);

        if (stmt.initExpression != null) compile(stmt.initExpression, frameOffsets, "t0");
        else if (stmt.initStatement != null) compile(stmt.initStatement, frameOffsets);

        code.append("  ").append(currentLoopHeadLabel).append(": nop\n");
        compile(stmt.condition, frameOffsets, "t1");
        code.append("  li t0 0\n");
        code.append("  neq t1 t0\n");
        code.append("  jz ").append(currentLoopTailLabel).append("\n");
        compile(stmt.body, frameOffsets);
        compile(stmt.inc, frameOffsets, "t0");
        code.append("  j ").append(currentLoopHeadLabel).append("\n");
        code.append("  ").append(currentLoopTailLabel).append(": nop\n");

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
        code.append("  jz ").append(elseLabel).append("\n");
        compile(stmt.body, frameOffsets);
        code.append("  ").append(elseLabel).append(": nop\n");
        compile(stmt.elseBody, frameOffsets);
        code.append("  ").append(endLabel).append(": nop\n");
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
            compile(stmt.initValue, frameOffsets, "t0");
            // TODO: sizeof thingy
            var offset = frameOffsets.get(stmt.name());
            code.append("  li t3 ").append(offset).append("\n");
            code.append("  swr t0 fp t3\n");
        }
    }

    void compile(StatementWhile stmt, Map<String, Integer> frameOffsets) {
        var headLabel = "while" + labelNo++;
        var tailLabel = "endwhile" + labelNo++;
        currentLoopHeadLabel.push(headLabel);
        currentLoopTailLabel.push(tailLabel);

        code.append("  ").append(headLabel).append(": nop\n");
        compile(stmt.condition, frameOffsets, "t1");
        code.append("  li t0 0\n");
        code.append("  neq t1 t0\n");
        code.append("  jz ").append(tailLabel).append("\n");
        compile(stmt.body, frameOffsets);
        code.append("  j ").append(headLabel).append("\n");
        code.append("  ").append(tailLabel).append(": nop\n");

        currentLoopHeadLabel.pop();
        currentLoopTailLabel.pop();
    }

    void compile(Expression expression, Map<String, Integer> frameOffsets, String dst) {
        switch (expression) {
            case ExpressionAccess expr -> compile(expr, frameOffsets, dst);
            case ExpressionBin expr -> compile(expr, frameOffsets, dst);
            case ExpressionCall expr -> compile(expr, frameOffsets, dst);
            case ExpressionCast expr -> compile(expr, frameOffsets, dst);
            case ExpressionChar expr -> compile(expr, frameOffsets, dst);
            case ExpressionIdent expr -> compile(expr, frameOffsets, dst);
            case ExpressionIndex expr -> compile(expr, frameOffsets, dst);
            case ExpressionInteger expr -> compile(expr, frameOffsets, dst);
            case ExpressionParens expr -> compile(expr, frameOffsets, dst);
            case ExpressionPostfix expr -> compile(expr, frameOffsets, dst);
            case ExpressionPrefix expr -> compile(expr, frameOffsets, dst);
            case ExpressionString expr -> compile(expr, frameOffsets, dst);
            case ExpressionSyntaxError expr -> compile(expr, frameOffsets, dst);
            case ExpressionTernary expr -> compile(expr, frameOffsets, dst);
            case ExpressionTypeError expr -> compile(expr, frameOffsets, dst);
        }
    }


    void compile(ExpressionAccess expr, Map<String, Integer> frameOffsets, String dst) {
        throw new RuntimeException("unimplemented");
    }

    void compile(ExpressionBin expr, Map<String, Integer> frameOffsets, String dst) {
        if (expr.op().equals("&&") || expr.op().equals("||")) {
            throw new RuntimeException("...");
        }

        if (expr.op().equals("<") || expr.op().equals(">") || expr.op().equals("<=") || expr.op().equals(">=")) {
            compile(expr.lhs, frameOffsets, "t4");
            compile(expr.rhs, frameOffsets, "t5");
            switch (expr.op()) {
                case "<" -> code.append("  gte t4 t5\n");
                case ">" -> code.append("  lte t4 t5\n");
                case "<=" -> code.append("  gt t4 t5\n");
                case ">=" -> code.append("  lt t4 t5\n");
            }
            String labelFalse = "cmpno" + labelNo++;
            String labelEnd = "endcmp" + labelNo++;
            code.append("  jnz ").append(labelFalse).append('\n');
            if (dst == null) {
                code.append("  spushi 1\n");
            } else {
                code.append("  li ").append(dst).append(" 1\n");
            }
            code.append("  j ").append(labelEnd).append('\n');
            code.append(labelFalse).append(": ");
            if (dst == null) {
                code.append("spushi 0\n");
            } else {
                code.append("li ").append(dst).append(" 0\n");
            }
            code.append(labelEnd).append(": nop\n");
            return;
        }

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

    void compile(ExpressionCall expr, Map<String, Integer> frameOffsets, String dst) {
        int ti = 0;
        for (var arg : expr.args) {
            compile(arg, frameOffsets, "a" + ti);
            ti += 1;
        }

        if (expr.functor instanceof ExpressionIdent ident) {
            code.append("  jal ").append(ident.name()).append("\n");

            var func = program.symbols.get(ident.name());

            if (!func.type().resultType().isVoid() && dst != null) {
                code.append("  pop ").append(dst).append('\n');
            }

        } else {
            throw new RuntimeException("unimplemented");
        }
    }

    void compile(ExpressionCast ignoredExpr, Map<String, Integer> ignoredFrameOffsets, String ignoredDst) {
        throw new RuntimeException("unimplemented");
    }

    void compile(ExpressionChar expr, Map<String, Integer> ignoredFrameOffsets, String dst) {
        var rune = (int) expr.content();
        if (rune >= 16) {
            String label = "num" + rune;
            if (!dataLabels.containsKey(label)) {
                data.append("  ").append(label).append(": ").append(rune).append('\n');
            }
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

    void compile(ExpressionIdent ident, Map<String, Integer> frameOffsets, String dst) {
        if (dataLabels.containsKey(ident.name())) {
            var label = dataLabels.get(ident.name());
            String insr = ident.type().isAPointerTo(SeaType.CHAR) ? "la" : "lw";
            if (dst == null) {
                code.append("  ").append(insr).append(" t0 ").append(label).append('\n');
                code.append("  push t0\n");
            } else {
                code.append("  ").append(insr).append(" ").append(dst).append(" ").append(label).append('\n');
            }
        } else {
            var offset = frameOffsets.get(ident.name());
            code.append("  li t3 ").append(offset).append("\n");
            if (dst == null) {
                code.append("  lwr t0 fp t3\n");
                code.append("  push t0\n");
            } else {
                code.append("  lwr ").append(dst).append(" fp t3\n");
            }
        }
    }

    void compile(ExpressionIndex expr, Map<String, Integer> frameOffsets, String dst) {
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

    void compile(ExpressionInteger expr, Map<String, Integer> ignored, String dst) {
        int val = expr.value;
        if (val >= 0 && val < 16) {
            if (dst == null) {
                code.append("  pushi ").append(val).append('\n');
            } else {
                code.append("  li ").append(dst).append(' ').append(val).append('\n');
            }
        } else {
            String label = "num" + val;
            if (!dataLabels.containsKey(label)) {
                data.append("  ").append(label).append(": ").append(val).append('\n');
            }
            if (dst == null) {
                code.append("  lw t0 ").append(label).append('\n');
                code.append("  push t0\n");
            } else {
                code.append("  lw ").append(dst).append(" ").append(label).append('\n');
            }
        }
    }

    void compile(ExpressionParens expr, Map<String, Integer> frameOffsets, String dst) {
        compile(expr.inner, frameOffsets, dst);
    }

    // a[i]++
    // ++a[i]
    // load a[i]
    // (hook) : value is on the stack, do whatever, leave (stack-value, assign-value)
    // write a[i] from stack?
    void compile(ExpressionPostfix expr, Map<String, Integer> frameOffsets, String dst) {
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

    void compile(ExpressionPrefix expr, Map<String, Integer> frameOffsets, String dst) {
        throw new UnsupportedOperationException();
    }

    void compile(ExpressionString expr, Map<String, Integer> frameOffsets, String dst) {
        var hash = Integer.toHexString(expr.content().hashCode()).toUpperCase();
        var label = "str" + hash;
        if (!dataLabels.containsKey(label)) {
            data.append("  ").append(label).append(": ").append(StringEscapeUtils.escapeString(expr.content())).append('\n');
        }
        if (dst == null) {
            code.append("  la t0 ").append(label).append('\n');
            code.append("  push t0");
        } else {
            code.append("  la ").append(dst).append(' ').append(label).append('\n');
        }
    }

    void compile(ExpressionSyntaxError expr, Map<String, Integer> frameOffsets, String dst) {
        throw new UnsupportedOperationException("cannot compile errors");
    }

    void compile(ExpressionTernary expr, Map<String, Integer> frameOffsets, String dst) {
        compile(expr.cond, frameOffsets, "t1");
        code.append("  li t0 0\n");
        code.append("  neq t1 t0\n");

        String labelElse = "ternelse" + labelNo++;
        String labelEnd = "endtern" + labelNo++;

        code.append("  jz ").append(labelElse).append('\n');
        compile(expr.then, frameOffsets, dst);
        code.append("  j ").append(labelEnd).append('\n');
        code.append(labelElse).append(":\n");
        compile(expr.otherwise, frameOffsets, dst);
        code.append(labelEnd).append(":\n");
        code.append("  nop\n");
    }

    void compile(ExpressionTypeError expr, Map<String, Integer> frameOffsets, String dst) {
        throw new UnsupportedOperationException();
    }

    protected void compile(DeclarationTypedef ignored) {
    }

    protected void compile(DeclarationVar item) throws CompilationException {
        Object value = null;
        if (item.initializer != null) {
            value = evalConstant(item.initializer);
        }
        var label = addGlobal(value);
        dataLabels.put(item.name.content(), label);
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

    int varNo = 0;

    String addGlobal(Object value) {
        var label = "dat" + varNo++;
        data.append("  ").append(label).append(": ");
        switch (value) {
            case String s -> {
                var str = StringEscapeUtils.escapeString(s);
                data.append(str).append('\n');
            }
            case Integer i -> {
                data.append(i).append('\n');
            }
            case null -> {
                data.append(0).append('\n');
            }
            case Object v -> throw new RuntimeException("don't know how to compile constant " + v.getClass());
        }
        ;

        return label;
    }
}
