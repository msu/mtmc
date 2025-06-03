package mtmc.lang.sea;

import mtmc.asm.Assembler;
import mtmc.lang.CompilationException;
import mtmc.lang.Span;
import mtmc.lang.sea.ast.*;
import mtmc.os.exec.Executable;
import mtmc.util.StringEscapeUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

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
        if (func.name.content().equals("puts")) {
            if (!func.returnType.type().isVoid()) {
                throw new CompilationException("'puts' must return void", func.returnType.span());
            }
            if (func.params.size() != 1 || !func.params.getFirst().type.type().isAPointer()
                    || !func.params.getFirst().type.type().componentType().isChar()) {
                throw new CompilationException("'puts' must take a char* as its only argument", Span.of(func.name));
            }

            code.append("puts:\n");
            code.append("  sys wstr\n");
            code.append("  ret\n");
            return;
        }

        if (func.name.content().equals("putn")) {
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

        var maxLocals = 0;
        var frameOffsets = new HashMap<String, Integer>();
        for (DeclarationFunc.Param param : func.params) {
            var size = param.type.type().size();
            frameOffsets.put(param.name.content(), size);
            maxLocals += size;
        }

        code.append(func.name.content()).append(":\n");
        code.append("  push fp\n");
        code.append("  dec sp ").append(maxLocals).append("\n");
        compile(func.body, frameOffsets);
        labelNo = 0;

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
                frameOffsets.put(sv.name(), frameOffsets.size());
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

        compile(stmt.condition, frameOffsets);
        code.append("  jnz ").append(currentLoopHeadLabel).append("\n");
        code.append("  ").append(currentLoopTailLabel).append(": nop\n");
        currentLoopHeadLabel.pop();
        currentLoopTailLabel.pop();
    }

    void compile(StatementExpression stmt, Map<String, Integer> frameOffsets) {
        compile(stmt.expression, frameOffsets);
    }

    void compile(StatementFor stmt, Map<String, Integer> frameOffsets) {
        currentLoopHeadLabel.push("for" + labelNo++);
        currentLoopTailLabel.push("endfor" + labelNo++);

        if (stmt.initExpression != null) compile(stmt.initExpression, frameOffsets);
        else if (stmt.initStatement != null) compile(stmt.initStatement, frameOffsets);

        code.append("  ").append(currentLoopHeadLabel).append(": nop\n");
        compile(stmt.condition, frameOffsets);
        code.append("  jz ").append(currentLoopTailLabel).append("\n");
        compile(stmt.body, frameOffsets);
        compile(stmt.inc, frameOffsets);
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

        compile(stmt.condition, frameOffsets);
        code.append("  jz ").append(elseLabel).append("\n");
        compile(stmt.body, frameOffsets);
        code.append("  ").append(elseLabel).append(": nop\n");
        compile(stmt.elseBody, frameOffsets);
        code.append("  ").append(endLabel).append(": nop\n");
    }

    void compile(StatementReturn stmt, Map<String, Integer> frameOffsets) {
        if (stmt.value != null) {
            compile(stmt.value, frameOffsets);
            code.append("  pop rv\n");
        }
        code.append("  ret\n");
    }

    void compile(StatementVar stmt, Map<String, Integer> frameOffsets) {
        if (stmt.initValue != null) {
            compile(stmt.initValue, frameOffsets);
            // TODO: sizeof thingy
            var offset = frameOffsets.get(stmt.name());
            code.append("  pop t0\n");
            code.append("  ldi t5 ").append(offset).append("\n");
            code.append("  swr t0 fp t5\n");
        }
    }

    void compile(StatementWhile stmt, Map<String, Integer> frameOffsets) {
        var headLabel = "while" + labelNo++;
        var tailLabel = "endwhile" + labelNo++;
        currentLoopHeadLabel.push(headLabel);
        currentLoopTailLabel.push(tailLabel);

        code.append("  ").append(headLabel).append(": nop\n");
        compile(stmt.condition, frameOffsets);
        code.append("  jz ").append(tailLabel).append("\n");
        compile(stmt.body, frameOffsets);
        code.append("  j ").append(headLabel).append("\n");
        code.append("  ").append(tailLabel).append(": nop\n");

        currentLoopHeadLabel.pop();
        currentLoopTailLabel.pop();
    }

    void compile(Expression expression, Map<String, Integer> frameOffsets) {
        switch (expression) {
            case ExpressionAccess expr -> compile(expr, frameOffsets);
            case ExpressionBin expr -> compile(expr, frameOffsets);
            case ExpressionCall expr -> compile(expr, frameOffsets);
            case ExpressionCast expr -> compile(expr, frameOffsets);
            case ExpressionChar expr -> compile(expr, frameOffsets);
            case ExpressionIdent expr -> compile(expr, frameOffsets);
            case ExpressionIndex expr -> compile(expr, frameOffsets);
            case ExpressionInteger expr -> compile(expr, frameOffsets);
            case ExpressionParens expr -> compile(expr, frameOffsets);
            case ExpressionPostfix expr -> compile(expr, frameOffsets);
            case ExpressionPrefix expr -> compile(expr, frameOffsets);
            case ExpressionString expr -> compile(expr, frameOffsets);
            case ExpressionSyntaxError expr -> compile(expr, frameOffsets);
            case ExpressionTernary expr -> compile(expr, frameOffsets);
            case ExpressionTypeError expr -> compile(expr, frameOffsets);
        }
    }


    void compile(ExpressionAccess expr, Map<String, Integer> frameOffsets) {
        throw new RuntimeException("unimplemented");
    }

    void compile(ExpressionBin expr, Map<String, Integer> frameOffsets) {
        if (expr.op().equals("&&") || expr.op().equals("||")
                || expr.op().equals("<=") || expr.op().equals(">=")
                || expr.op().equals("<") || expr.op().equals(">")) {
//            String andEnd = "endand" + labelNo++;
//            compile(expr.lhs, frameOffsets);
//            code.append("  jz ").append(andEnd).append("\n");
//            compile(expr.rhs, frameOffsets);
//            code.append("  eqi 0").append(andEnd).append("\n");
//            code.append("  ").append(andEnd).append(": nop\n");
            throw new RuntimeException("unimplemented");
        }

        compile(expr.lhs, frameOffsets);
        compile(expr.rhs, frameOffsets);
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
    }

    void compile(ExpressionCall expr, Map<String, Integer> frameOffsets) {
        int ti = 0;
        for (var arg : expr.args) {
            compile(arg, frameOffsets);
            code.append("  pop a").append(ti).append("\n");
            ti += 1;
        }

        if (expr.functor instanceof ExpressionIdent ident) {
            code.append("  call ").append(ident.name()).append("\n");
        } else {
            throw new RuntimeException("unimplemented");
        }
    }

    void compile(ExpressionCast expr, Map<String, Integer> frameOffsets) {
        throw new RuntimeException("unimplemented");
    }

    void compile(ExpressionChar expr, Map<String, Integer> frameOffsets) {
        var rune = (int) expr.content();
        if (rune > 16) {
            String label = "num" + rune;
            if (!dataLabels.containsKey(label)) {
                data.append("  ").append(label).append(": ").append(rune).append('\n');
            }
            code.append("  li ").append(label).append('\n');
            code.append("  spush\n");
        } else {
            code.append("  pushi ").append(rune).append("\n");
        }
    }

    void compile(ExpressionIdent ident, Map<String, Integer> frameOffsets) {
        if (dataLabels.containsKey(ident.name())) {
            var label = dataLabels.get(ident.name());
            code.append("  lw t0 ").append(label).append('\n');
            code.append("  push t0\n");
        } else {
            var offset = frameOffsets.get(ident.name());
            code.append("  ldi t5 ").append(-offset).append("\n");
            code.append("  lwr t0 bp t5\n");
            code.append("  push t0\n");

        }
    }

    void compile(ExpressionIndex expr, Map<String, Integer> frameOffsets) {
        compile(expr.array, frameOffsets);
        compile(expr.index, frameOffsets);
        code.append("  pop t1\n"); // load the offset into t1
        if (expr.array.type().size() != 1) {
            code.append("  li t0 ").append(expr.array.type().size()).append('\n');
            code.append("  mul t1 t1 t0\n");
        }
        code.append("  pop t0\n"); // load the pointer into t0
        code.append("  lwr t2 t0 t1\n");
        code.append("  push t2\n");
    }

    void compile(ExpressionInteger expr, Map<String, Integer> ignored) {
        int val = expr.value;
        String label = "num" + val;
        if (dataLabels.containsKey(label)) {
            code.append("  lw a0 ").append(label).append('\n');
            code.append("  push a0\n");
        }
    }

    void compile(ExpressionParens expr, Map<String, Integer> frameOffsets) {
        compile(expr.inner, frameOffsets);
    }

    // a[i]++
    // ++a[i]
    // load a[i]
    // (hook) : value is on the stack, do whatever, leave (stack-value, assign-value)
    // write a[i] from stack?
    void compile(ExpressionPostfix expr, Map<String, Integer> frameOffsets) {
        compile(expr.inner, frameOffsets);
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

    void compile(ExpressionPrefix expr, Map<String, Integer> frameOffsets) {

    }

    void compile(ExpressionString expr, Map<String, Integer> frameOffsets) {

    }

    void compile(ExpressionSyntaxError expr, Map<String, Integer> frameOffsets) {

    }

    void compile(ExpressionTernary expr, Map<String, Integer> frameOffsets) {

    }

    void compile(ExpressionTypeError expr, Map<String, Integer> frameOffsets) {

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
