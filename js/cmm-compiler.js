// C-- to x366 Assembly Compiler
// Three phases: Lexer, Parser (recursive descent), Code Generator

// ======================== LEXER ========================

const KEYWORDS = new Set(['int', 'char', 'if', 'else', 'while', 'return']);

function lex(source) {
    const tokens = [];
    let pos = 0;
    let line = 1;

    while (pos < source.length) {
        // skip whitespace
        if (source[pos] === '\n') { line++; pos++; continue; }
        if (/\s/.test(source[pos])) { pos++; continue; }

        // skip // comments
        if (source[pos] === '/' && pos + 1 < source.length && source[pos + 1] === '/') {
            while (pos < source.length && source[pos] !== '\n') pos++;
            continue;
        }

        const startLine = line;

        // number literal (decimal or 0x hex)
        if (/[0-9]/.test(source[pos])) {
            let start = pos;
            if (source[pos] === '0' && pos + 1 < source.length && (source[pos + 1] === 'x' || source[pos + 1] === 'X')) {
                pos += 2;
                while (pos < source.length && /[0-9a-fA-F]/.test(source[pos])) pos++;
                tokens.push({ type: 'NUMBER', value: parseInt(source.substring(start, pos), 16), line: startLine });
            } else {
                while (pos < source.length && /[0-9]/.test(source[pos])) pos++;
                tokens.push({ type: 'NUMBER', value: parseInt(source.substring(start, pos), 10), line: startLine });
            }
            continue;
        }

        // identifier or keyword
        if (/[a-zA-Z_]/.test(source[pos])) {
            let start = pos;
            while (pos < source.length && /[a-zA-Z0-9_]/.test(source[pos])) pos++;
            const text = source.substring(start, pos);
            if (KEYWORDS.has(text)) {
                tokens.push({ type: text.toUpperCase(), value: text, line: startLine });
            } else {
                tokens.push({ type: 'IDENTIFIER', value: text, line: startLine });
            }
            continue;
        }

        // char literal
        if (source[pos] === "'") {
            pos++; // skip opening quote
            let charVal;
            if (source[pos] === '\\') {
                pos++;
                switch (source[pos]) {
                    case 'n':  charVal = 10; break;
                    case 't':  charVal = 9;  break;
                    case 'r':  charVal = 13; break;
                    case '\\': charVal = 92; break;
                    case "'":  charVal = 39; break;
                    case '0':  charVal = 0;  break;
                    default:   charVal = source.charCodeAt(pos); break;
                }
                pos++;
            } else {
                charVal = source.charCodeAt(pos);
                pos++;
            }
            if (source[pos] !== "'") throw new Error(`Line ${startLine}: Expected closing quote for char literal`);
            pos++; // skip closing quote
            tokens.push({ type: 'CHAR_LITERAL', value: charVal, line: startLine });
            continue;
        }

        // string literal
        if (source[pos] === '"') {
            pos++; // skip opening quote
            let str = '';
            while (pos < source.length && source[pos] !== '"') {
                if (source[pos] === '\\') {
                    pos++;
                    switch (source[pos]) {
                        case 'n':  str += '\n'; break;
                        case 't':  str += '\t'; break;
                        case 'r':  str += '\r'; break;
                        case '\\': str += '\\'; break;
                        case '"':  str += '"';  break;
                        case '0':  str += '\0'; break;
                        default:   str += source[pos]; break;
                    }
                } else {
                    if (source[pos] === '\n') line++;
                    str += source[pos];
                }
                pos++;
            }
            if (pos >= source.length) throw new Error(`Line ${startLine}: Unterminated string literal`);
            pos++; // skip closing quote
            tokens.push({ type: 'STRING_LITERAL', value: str, line: startLine });
            continue;
        }

        // two-character operators
        const two = source.substring(pos, pos + 2);
        if (two === '==' || two === '!=' || two === '<=' || two === '>=' || two === '&&' || two === '||') {
            tokens.push({ type: two, value: two, line: startLine });
            pos += 2;
            continue;
        }

        // single-character operators and punctuation
        const ch = source[pos];
        if ('+-*/%=<>!&'.includes(ch)) {
            tokens.push({ type: ch, value: ch, line: startLine });
            pos++;
            continue;
        }
        if ('{}()[];,'.includes(ch)) {
            tokens.push({ type: ch, value: ch, line: startLine });
            pos++;
            continue;
        }

        throw new Error(`Line ${startLine}: Unexpected character '${ch}'`);
    }

    tokens.push({ type: 'EOF', value: null, line: line });
    return tokens;
}

// ======================== PARSER ========================

function parse(tokens) {
    let pos = 0;

    function peek() { return tokens[pos]; }
    function advance() { return tokens[pos++]; }
    function check(type) { return tokens[pos].type === type; }

    function expect(type) {
        if (!check(type)) {
            throw new Error(`Line ${peek().line}: Expected '${type}' but got '${peek().type}' ('${peek().value}')`);
        }
        return advance();
    }

    function match(type) {
        if (check(type)) { advance(); return true; }
        return false;
    }

    function isType() {
        return check('INT') || check('CHAR');
    }

    function parseType() {
        let kind;
        if (match('INT')) kind = 'int';
        else if (match('CHAR')) kind = 'char';
        else throw new Error(`Line ${peek().line}: Expected type`);
        if (check('*')) { advance(); kind += '*'; }
        return kind;
    }

    function parseProgram() {
        const items = [];
        while (!check('EOF')) {
            if (isType()) {
                items.push(parseTypeLeading());
            } else {
                items.push({ type: 'top_stmt', stmt: parseStatement() });
            }
        }
        return { type: 'program', items };
    }

    // After seeing a type, determine if it's a global_decl, func_def, or local_decl
    function parseTypeLeading() {
        const line = peek().line;
        const varType = parseType();
        const name = expect('IDENTIFIER').value;

        // function definition: type name '(' ...
        if (check('(')) {
            return parseFunctionDef(varType, name, line);
        }

        // global declaration
        return parseGlobalDecl(varType, name, line);
    }

    function parseFunctionDef(returnType, name, line) {
        expect('(');
        const params = [];
        if (!check(')')) {
            do {
                const pType = parseType();
                const pName = expect('IDENTIFIER').value;
                params.push({ type: pType, name: pName });
            } while (match(','));
        }
        expect(')');
        const body = parseBlock();
        return { type: 'func_def', returnType, name, params, body, line };
    }

    function parseGlobalDecl(varType, name, line) {
        let arraySize = null;
        let adjustedType = varType;
        if (match('[')) {
            if (check('NUMBER')) {
                arraySize = advance().value;
            } else {
                arraySize = 0;
            }
            expect(']');
            // convert type to array type
            adjustedType = varType.replace('*', '') + '[]';
        }

        let init = null;
        let arrayInit = null;
        let stringInit = null;
        if (match('=')) {
            if (check('{')) {
                // brace-list init
                advance();
                arrayInit = [];
                if (!check('}')) {
                    do {
                        arrayInit.push(parseExpression());
                    } while (match(','));
                }
                expect('}');
            } else if (check('STRING_LITERAL') && adjustedType.endsWith('[]')) {
                stringInit = peek().value;
                init = parseExpression();
            } else {
                init = parseExpression();
            }
        }
        expect(';');
        return { type: 'global_decl', varType: adjustedType, name, arraySize, init, arrayInit, stringInit, line };
    }

    function parseLocalDecl(varType, name, line) {
        let arraySize = null;
        let adjustedType = varType;
        if (match('[')) {
            if (check('NUMBER')) {
                arraySize = advance().value;
            } else {
                arraySize = 0;
            }
            expect(']');
            adjustedType = varType.replace('*', '') + '[]';
        }

        let init = null;
        if (match('=')) {
            init = parseExpression();
        }
        expect(';');
        return { type: 'local_decl', varType: adjustedType, name, arraySize, init, line };
    }

    function parseBlock() {
        const line = peek().line;
        expect('{');
        const items = [];
        while (!check('}')) {
            if (isType()) {
                const declLine = peek().line;
                const vType = parseType();
                const vName = expect('IDENTIFIER').value;
                items.push(parseLocalDecl(vType, vName, declLine));
            } else {
                items.push(parseStatement());
            }
        }
        expect('}');
        return { type: 'block', items, line };
    }

    function parseStatement() {
        if (check('IF')) return parseIf();
        if (check('WHILE')) return parseWhile();
        if (check('RETURN')) return parseReturn();
        if (check('{')) return parseBlock();
        return parseExpressionStatement();
    }

    function parseIf() {
        const line = peek().line;
        expect('IF');
        expect('(');
        const condition = parseExpression();
        expect(')');
        const thenBranch = parseStatement();
        let elseBranch = null;
        if (match('ELSE')) {
            elseBranch = parseStatement();
        }
        return { type: 'if', condition, thenBranch, elseBranch, line };
    }

    function parseWhile() {
        const line = peek().line;
        expect('WHILE');
        expect('(');
        const condition = parseExpression();
        expect(')');
        const body = parseStatement();
        return { type: 'while', condition, body, line };
    }

    function parseReturn() {
        const line = peek().line;
        expect('RETURN');
        let value = null;
        if (!check(';')) {
            value = parseExpression();
        }
        expect(';');
        return { type: 'return', value, line };
    }

    function parseExpressionStatement() {
        const line = peek().line;
        const expr = parseExpression();
        // Check if this is an assignment
        if (check('=') && (expr.type === 'var_ref' || expr.type === 'array_index' || expr.type === 'unary')) {
            advance(); // consume '='
            const value = parseExpression();
            expect(';');
            return { type: 'assign', target: expr, value, line };
        }
        expect(';');
        return { type: 'expr_stmt', expr, line };
    }

    function parseExpression() {
        return parseLogicalOr();
    }

    function parseLogicalOr() {
        let left = parseLogicalAnd();
        while (check('||')) {
            advance();
            const right = parseLogicalAnd();
            left = { type: 'logical', op: '||', left, right };
        }
        return left;
    }

    function parseLogicalAnd() {
        let left = parseEquality();
        while (check('&&')) {
            advance();
            const right = parseEquality();
            left = { type: 'logical', op: '&&', left, right };
        }
        return left;
    }

    function parseEquality() {
        let left = parseComparison();
        while (check('==') || check('!=')) {
            const op = advance().value;
            const right = parseComparison();
            left = { type: 'binary', op, left, right };
        }
        return left;
    }

    function parseComparison() {
        let left = parseAdditive();
        while (check('<') || check('>') || check('<=') || check('>=')) {
            const op = advance().value;
            const right = parseAdditive();
            left = { type: 'binary', op, left, right };
        }
        return left;
    }

    function parseAdditive() {
        let left = parseMultiplicative();
        while (check('+') || check('-')) {
            const op = advance().value;
            const right = parseMultiplicative();
            left = { type: 'binary', op, left, right };
        }
        return left;
    }

    function parseMultiplicative() {
        let left = parseUnary();
        while (check('*') || check('/') || check('%')) {
            const op = advance().value;
            const right = parseUnary();
            left = { type: 'binary', op, left, right };
        }
        return left;
    }

    function parseUnary() {
        if (check('-') || check('!') || check('&')) {
            const op = advance().value;
            const operand = parseUnary();
            return { type: 'unary', op, operand };
        }
        // '*' as dereference (unary) - only if not in a multiplicative context
        if (check('*')) {
            // Look ahead: if next token after * looks like a primary (identifier, number, paren, etc.)
            // and the current context is unary, treat as dereference
            const saved = pos;
            advance();
            // Try parsing as unary operand
            try {
                const operand = parseUnary();
                return { type: 'unary', op: '*', operand };
            } catch (e) {
                pos = saved;
            }
        }
        return parsePrimary();
    }

    function parsePrimary() {
        // number
        if (check('NUMBER')) {
            const tok = advance();
            return { type: 'number', value: tok.value, line: tok.line };
        }

        // char literal
        if (check('CHAR_LITERAL')) {
            const tok = advance();
            return { type: 'char', value: tok.value, line: tok.line };
        }

        // string literal
        if (check('STRING_LITERAL')) {
            const tok = advance();
            return { type: 'string', value: tok.value, line: tok.line };
        }

        // identifier: variable ref, array index, or function call
        if (check('IDENTIFIER')) {
            const tok = advance();
            const name = tok.value;

            // function call
            if (check('(')) {
                advance();
                const args = [];
                if (!check(')')) {
                    do {
                        args.push(parseExpression());
                    } while (match(','));
                }
                expect(')');
                return { type: 'call', name, args, line: tok.line };
            }

            // array index
            if (check('[')) {
                advance();
                const index = parseExpression();
                expect(']');
                return { type: 'array_index', name, index, line: tok.line };
            }

            return { type: 'var_ref', name, line: tok.line };
        }

        // parenthesized expression
        if (check('(')) {
            advance();
            const expr = parseExpression();
            expect(')');
            return expr;
        }

        throw new Error(`Line ${peek().line}: Unexpected token '${peek().value}'`);
    }

    return parseProgram();
}

// ======================== CODE GENERATOR ========================

function generate(ast) {
    let currentOutput = [];
    let labelCounter = 0;
    const stringLiterals = new Map(); // label -> decoded string
    let stringCounter = 0;
    const globals = new Map(); // name -> variable info
    let locals = null; // null when outside function
    let nextLocalOffset = -2;

    function appendLine(line) {
        currentOutput.push(line);
    }

    function newLabel(prefix) {
        return prefix + '_' + labelCounter++;
    }

    function registerStringLiteral(value) {
        const label = 'str_' + stringCounter++;
        stringLiterals.set(label, value);
        return label;
    }

    function lookupVar(name) {
        if (locals && locals.has(name)) return locals.get(name);
        if (globals.has(name)) return globals.get(name);
        throw new Error('Undefined variable: ' + name);
    }

    function makeVariable(varType, name, isGlobal, bpOffset) {
        return {
            name,
            varType,
            global: isGlobal,
            bpOffset,
            arraySize: 0,
            stringInit: null
        };
    }

    function isCharType(varType) {
        return varType === 'char' || varType === 'char*' || varType === 'char[]';
    }

    function isPointerType(varType) {
        return varType === 'int*' || varType === 'char*';
    }

    function isArrayType(varType) {
        return varType === 'int[]' || varType === 'char[]';
    }

    function elementSize(varType) {
        return isCharType(varType) ? 1 : 2;
    }

    function formatBpOffset(offset) {
        return offset >= 0 ? '+' + offset : '' + offset;
    }

    function emitLineMarker(line) {
        if (line != null) appendLine('; @LINE ' + line);
    }

    function emitStore(v) {
        if (v.global) {
            appendLine('    MOV [' + v.name + '], AX  ; store ' + v.name);
        } else {
            appendLine('    MOV [BP' + formatBpOffset(v.bpOffset) + '], AX  ; store ' + v.name);
        }
    }

    // Phase 1: register all globals
    for (const item of ast.items) {
        if (item.type === 'global_decl') {
            registerGlobal(item);
        }
    }

    // Phase 2: generate code into a temporary buffer (so string literals are discovered)
    const codeOutput = [];
    const savedOutput = currentOutput;
    currentOutput = codeOutput;

    // emit global initializers and top-level statements
    for (const item of ast.items) {
        if (item.type === 'global_decl') {
            emitGlobalInit(item);
        } else if (item.type === 'top_stmt') {
            compileStatement(item.stmt);
        }
    }

    // check for main
    const hasMain = ast.items.some(item => item.type === 'func_def' && item.name === 'main');
    if (hasMain) appendLine('    CALL main');
    appendLine('    SYSCALL EXIT');

    // emit function definitions
    for (const item of ast.items) {
        if (item.type === 'func_def') {
            appendLine('');
            compileFunctionDef(item);
        }
    }

    // Phase 3: switch back to main output, emit data section, then append code
    currentOutput = savedOutput;
    emitDataSection();
    appendLine('');
    for (const line of codeOutput) {
        currentOutput.push(line);
    }

    return currentOutput.join('\n') + '\n';

    function registerGlobal(decl) {
        const v = makeVariable(decl.varType, decl.name, true, 0);

        if (decl.arraySize != null) {
            v.arraySize = decl.arraySize;
        }

        // char[] = "string" pattern
        if (isArrayType(decl.varType) && isCharType(decl.varType) && decl.stringInit != null) {
            v.stringInit = decl.stringInit;
            if (v.arraySize === 0) v.arraySize = decl.stringInit.length + 1;
        }

        globals.set(v.name, v);
    }

    function emitGlobalInit(decl) {
        const g = globals.get(decl.name);
        if (g.stringInit != null) return;

        if (decl.arrayInit != null) {
            emitLineMarker(decl.line);
            for (let i = 0; i < decl.arrayInit.length; i++) {
                compileExpression(decl.arrayInit[i]);
                appendLine('    MOV BX, ' + g.name);
                appendLine('    MOV CX, ' + (i * elementSize(g.varType)));
                appendLine('    ADD BX, CX');
                appendLine('    MOV [BX+0], AX');
            }
        } else if (decl.init != null) {
            emitLineMarker(decl.line);
            compileExpression(decl.init);
            emitStore(g);
        }
    }

    function emitDataSection() {
        // Emit read-only data first (string literals, char[] string inits)
        appendLine('.RODATA');
        for (const [name, g] of globals) {
            if (g.stringInit != null) {
                appendLine(g.name + ':');
                for (let i = 0; i < g.stringInit.length; i++) {
                    appendLine('    DB ' + g.stringInit.charCodeAt(i));
                }
                appendLine('    DB 0');
            }
        }
        for (const [label, value] of stringLiterals) {
            appendLine(label + ':');
            for (let i = 0; i < value.length; i++) {
                if (i === 0) {
                    appendLine('    DB ' + value.charCodeAt(i) + '  ; "' + value.replace(/\n/g, '\\n') + '"');
                } else {
                    appendLine('    DB ' + value.charCodeAt(i));
                }
            }
            appendLine('    DB 0');
        }

        // Emit mutable global data
        appendLine('.DATA');
        for (const [name, g] of globals) {
            if (g.stringInit != null) continue; // already emitted in rodata
            appendLine(g.name + ':');
            if (isArrayType(g.varType)) {
                const directive = isCharType(g.varType) ? 'DB' : 'DW';
                appendLine('    ' + directive + ' ' + g.arraySize + ' DUP(0)  ; ' + g.name + '[' + g.arraySize + ']');
            } else if (g.varType === 'char') {
                appendLine('    DB 0  ; char ' + g.name);
            } else {
                const typeStr = isPointerType(g.varType) ? (isCharType(g.varType) ? 'char*' : 'int*') : 'int';
                appendLine('    DW 0  ; ' + typeStr + ' ' + g.name);
            }
        }
    }

    function countLocalsInBlock(block) {
        let count = 0;
        for (const item of block.items) {
            if (item.type === 'local_decl') {
                if (item.arraySize != null) {
                    const bytes = item.arraySize * elementSize(item.varType);
                    count += Math.ceil(bytes / 2);
                } else {
                    count++;
                }
            } else if (item.type === 'block') {
                count += countLocalsInBlock(item);
            } else if (item.type === 'if') {
                count += countLocalsInStmt(item.thenBranch);
                if (item.elseBranch) count += countLocalsInStmt(item.elseBranch);
            } else if (item.type === 'while') {
                count += countLocalsInStmt(item.body);
            }
        }
        return count;
    }

    function countLocalsInStmt(stmt) {
        if (stmt.type === 'block') return countLocalsInBlock(stmt);
        if (stmt.type === 'if') {
            let n = countLocalsInStmt(stmt.thenBranch);
            if (stmt.elseBranch) n += countLocalsInStmt(stmt.elseBranch);
            return n;
        }
        if (stmt.type === 'while') return countLocalsInStmt(stmt.body);
        return 0;
    }

    function compileFunctionDef(func) {
        emitLineMarker(func.line);
        appendLine(func.name + ':');

        locals = new Map();
        nextLocalOffset = -2;

        const paramRegs = ['AX', 'BX', 'CX', 'DX', 'SI', 'DI'];

        if (func.params.length > 6) {
            throw new Error('Function ' + func.name + ' has more than 6 parameters');
        }

        for (const p of func.params) {
            const v = makeVariable(p.type, p.name, false, nextLocalOffset);
            nextLocalOffset -= 2;
            locals.set(v.name, v);
        }

        const totalSlots = func.params.length + countLocalsInBlock(func.body);

        // Function prolog: save caller's frame, set up local stack
        appendLine('    PUSH BP           ; save caller\'s base pointer');
        appendLine('    MOV BP, SP        ; set up new stack frame');
        if (totalSlots > 0)
            appendLine('    SUB SP, ' + (totalSlots * 2) + '        ; allocate ' + totalSlots + ' local slots');

        for (let i = 0; i < func.params.length; i++) {
            const v = locals.get(func.params[i].name);
            appendLine('    MOV [BP' + formatBpOffset(v.bpOffset) + '], ' + paramRegs[i] + '  ; param ' + v.name);
        }

        compileBlock(func.body);

        // Function epilog: tear down stack frame and return
        appendLine('    MOV SP, BP        ; deallocate locals');
        appendLine('    POP BP            ; restore caller\'s base pointer');
        appendLine('    RET');

        locals = null;
    }

    function compileBlock(block) {
        for (const item of block.items) {
            if (item.type === 'local_decl') {
                compileLocalDecl(item);
            } else {
                compileStatement(item);
            }
        }
    }

    function compileLocalDecl(decl) {
        if (decl.arraySize != null) {
            const bytes = decl.arraySize * elementSize(decl.varType);
            const slots = Math.ceil(bytes / 2);
            nextLocalOffset -= (slots - 1) * 2;
            const v = makeVariable(decl.varType, decl.name, false, nextLocalOffset);
            v.arraySize = decl.arraySize;
            nextLocalOffset -= 2;
            locals.set(v.name, v);
        } else {
            const v = makeVariable(decl.varType, decl.name, false, nextLocalOffset);
            nextLocalOffset -= 2;
            locals.set(v.name, v);

            if (decl.init != null) {
                emitLineMarker(decl.line);
                compileExpression(decl.init);
                emitStore(v);
            }
        }
    }

    function compileStatement(stmt) {
        switch (stmt.type) {
            case 'if':       compileIf(stmt); break;
            case 'while':    compileWhile(stmt); break;
            case 'return':   compileReturn(stmt); break;
            case 'assign':   compileAssign(stmt); break;
            case 'block':    compileBlock(stmt); break;
            case 'expr_stmt':
                emitLineMarker(stmt.line);
                compileExpression(stmt.expr);
                break;
            default:
                throw new Error('Unknown statement type: ' + stmt.type);
        }
    }

    function compileIf(stmt) {
        const elseLabel = newLabel('if_else');
        const endLabel = newLabel('if_end');

        emitLineMarker(stmt.line);
        appendLine('');
        appendLine('; If statement');
        compileExpression(stmt.condition);
        appendLine('    CMP AX, 0');
        appendLine('    JE ' + elseLabel);

        appendLine('; Then block');
        compileStatement(stmt.thenBranch);
        appendLine('    JMP ' + endLabel);

        appendLine(elseLabel + ':');
        if (stmt.elseBranch) {
            appendLine('; Else block');
            compileStatement(stmt.elseBranch);
        }

        appendLine(endLabel + ':');
    }

    function compileWhile(stmt) {
        const startLabel = newLabel('while_start');
        const endLabel = newLabel('while_end');

        emitLineMarker(stmt.line);
        appendLine('');
        appendLine('; While loop');
        appendLine(startLabel + ':');
        compileExpression(stmt.condition);
        appendLine('    CMP AX, 0');
        appendLine('    JE ' + endLabel);
        compileStatement(stmt.body);
        appendLine('    JMP ' + startLabel);
        appendLine(endLabel + ':');
    }

    function compileReturn(stmt) {
        emitLineMarker(stmt.line);
        appendLine('');
        appendLine('; Return statement');
        if (stmt.value != null) {
            compileExpression(stmt.value);
        }
        // Early return epilog
        appendLine('    MOV SP, BP        ; deallocate locals');
        appendLine('    POP BP            ; restore caller\'s base pointer');
        appendLine('    RET');
    }

    function compileAssign(stmt) {
        emitLineMarker(stmt.line);
        appendLine('');
        appendLine('; Assignment');

        // Evaluate RHS first
        compileExpression(stmt.value);

        // Store to LHS
        const target = stmt.target;

        if (target.type === 'var_ref') {
            const v = lookupVar(target.name);
            if (v.global) {
                appendLine('    MOV [' + v.name + '], AX  ; store ' + v.name);
            } else {
                appendLine('    MOV [BP' + formatBpOffset(v.bpOffset) + '], AX  ; store ' + v.name);
            }
        } else if (target.type === 'array_index') {
            const v = lookupVar(target.name);
            appendLine('    PUSH AX');
            compileExpression(target.index);
            if (elementSize(v.varType) === 2) {
                appendLine('    ADD AX, AX');
            }
            if (v.global) {
                appendLine('    MOV BX, ' + target.name + '  ; &' + target.name);
            } else if (isArrayType(v.varType)) {
                appendLine('    LEA BX, [BP' + formatBpOffset(v.bpOffset) + ']');
            } else {
                appendLine('    MOV BX, [BP' + formatBpOffset(v.bpOffset) + ']  ; load ' + v.name);
            }
            appendLine('    ADD BX, AX');
            appendLine('    POP AX');
            if (isCharType(v.varType)) {
                appendLine('    MOV [BX+0], AL');
            } else {
                appendLine('    MOV [BX+0], AX');
            }
        } else if (target.type === 'unary' && target.op === '*') {
            // dereference assignment: *ptr = value
            appendLine('    PUSH AX');
            compileExpression(target.operand);
            appendLine('    MOV BX, AX');
            appendLine('    POP AX');
            appendLine('    MOV [BX+0], AX');
        } else {
            throw new Error('Invalid assignment target');
        }
    }

    function compileExpression(expr) {
        switch (expr.type) {
            case 'binary':  return compileBinary(expr);
            case 'logical': compileLogical(expr); return 'int';
            case 'unary':   return compileUnary(expr);
            case 'call':    compileCall(expr); return 'int';
            case 'var_ref': return compileVarRef(expr);
            case 'array_index': return compileArrayIndex(expr);
            case 'number':  appendLine('    MOV AX, ' + expr.value); return 'int';
            case 'char':    appendLine('    MOV AX, ' + expr.value); return 'char';
            case 'string': {
                const label = registerStringLiteral(expr.value);
                appendLine('    MOV AX, ' + label);
                return 'char*';
            }
            default:
                throw new Error('Unknown expression type: ' + expr.type);
        }
    }

    function isLeaf(expr) {
        return expr.type === 'number' || expr.type === 'char' || expr.type === 'var_ref';
    }

    function compileLeafIntoBX(expr) {
        if (expr.type === 'number') {
            appendLine('    MOV BX, ' + expr.value);
        } else if (expr.type === 'char') {
            appendLine('    MOV BX, ' + expr.value);
        } else if (expr.type === 'var_ref') {
            const v = lookupVar(expr.name);
            if (isArrayType(v.varType)) {
                if (v.global) {
                    appendLine('    MOV BX, ' + v.name + '  ; &' + v.name);
                } else {
                    appendLine('    LEA BX, [BP' + formatBpOffset(v.bpOffset) + ']  ; &' + v.name);
                }
            } else if (v.global) {
                appendLine('    MOV BX, [' + v.name + ']  ; load ' + v.name);
            } else {
                appendLine('    MOV BX, [BP' + formatBpOffset(v.bpOffset) + ']  ; load ' + v.name);
            }
        }
    }

    function compileRightIntoBX(right) {
        if (isLeaf(right)) {
            compileLeafIntoBX(right);
        } else {
            appendLine('    PUSH AX');
            compileExpression(right);
            appendLine('    MOV BX, AX');
            appendLine('    POP AX');
        }
    }

    function compileBinary(expr) {
        const cmps = {
            '==': 'SETE AX',  '!=': 'SETNE AX',
            '<':  'SETL AX',  '>':  'SETG AX',
            '<=': 'SETLE AX', '>=': 'SETGE AX',
        };

        // Pointer arithmetic: ptr + int or ptr - int
        if (expr.op === '+' || expr.op === '-') {
            const leftType = compileExpression(expr.left);
            if (isPointerType(leftType)) {
                // Scale the integer operand by element size
                if (isLeaf(expr.right)) {
                    compileLeafIntoBX(expr.right);
                } else {
                    appendLine('    PUSH AX');
                    compileExpression(expr.right);
                    appendLine('    MOV BX, AX');
                    appendLine('    POP AX');
                }
                if (elementSize(leftType) === 2) {
                    appendLine('    ADD BX, BX');  // scale by sizeof(int)
                }
                appendLine(expr.op === '+' ? '    ADD AX, BX' : '    SUB AX, BX');
                return leftType;
            }
            compileRightIntoBX(expr.right);
            appendLine(expr.op === '+' ? '    ADD AX, BX' : '    SUB AX, BX');
            return 'int';
        }

        if (expr.op === '*') {
            compileExpression(expr.left);
            compileRightIntoBX(expr.right);
            appendLine('    MUL BX');
            return 'int';
        } else if (expr.op === '/') {
            compileExpression(expr.left);
            compileRightIntoBX(expr.right);
            appendLine('    DIV BX');
            return 'int';
        } else if (expr.op === '%') {
            compileExpression(expr.left);
            compileRightIntoBX(expr.right);
            appendLine('    DIV BX');
            appendLine('    MOV AX, DX');
            return 'int';
        } else if (cmps[expr.op]) {
            compileExpression(expr.left);
            compileRightIntoBX(expr.right);
            appendLine('    CMP AX, BX');
            appendLine('    ' + cmps[expr.op]);
            return 'int';
        } else {
            throw new Error('Unknown binary operator: ' + expr.op);
        }
    }

    function compileLogical(expr) {
        if (expr.op === '||') {
            const trueLabel = newLabel('or_true');
            const endLabel = newLabel('or_end');
            compileExpression(expr.left);
            appendLine('    CMP AX, 0');
            appendLine('    JNE ' + trueLabel);
            compileExpression(expr.right);
            appendLine('    CMP AX, 0');
            appendLine('    JNE ' + trueLabel);
            appendLine('    MOV AX, 0');
            appendLine('    JMP ' + endLabel);
            appendLine(trueLabel + ':');
            appendLine('    MOV AX, 1');
            appendLine(endLabel + ':');
        } else if (expr.op === '&&') {
            const falseLabel = newLabel('and_false');
            const endLabel = newLabel('and_end');
            compileExpression(expr.left);
            appendLine('    CMP AX, 0');
            appendLine('    JE ' + falseLabel);
            compileExpression(expr.right);
            appendLine('    CMP AX, 0');
            appendLine('    JE ' + falseLabel);
            appendLine('    MOV AX, 1');
            appendLine('    JMP ' + endLabel);
            appendLine(falseLabel + ':');
            appendLine('    MOV AX, 0');
            appendLine(endLabel + ':');
        }
    }

    function compileUnary(expr) {
        switch (expr.op) {
            case '-':
                compileExpression(expr.operand);
                appendLine('    NEG AX');
                return 'int';
            case '!': {
                const zeroLabel = newLabel('not_zero');
                const endLabel = newLabel('not_end');
                compileExpression(expr.operand);
                appendLine('    CMP AX, 0');
                appendLine('    JE ' + zeroLabel);
                appendLine('    MOV AX, 0');
                appendLine('    JMP ' + endLabel);
                appendLine(zeroLabel + ':');
                appendLine('    MOV AX, 1');
                appendLine(endLabel + ':');
                return 'int';
            }
            case '*': {
                // dereference
                const ptrType = compileExpression(expr.operand);
                appendLine('    MOV BX, AX');
                if (ptrType === 'char*') {
                    appendLine('    MOV AL, [BX+0]');
                    return 'char';
                } else {
                    appendLine('    MOV AX, [BX+0]');
                    return 'int';
                }
            }
            case '&': {
                // address-of
                if (expr.operand.type === 'var_ref') {
                    const v = lookupVar(expr.operand.name);
                    if (v.global) {
                        appendLine('    MOV AX, ' + expr.operand.name + '  ; &' + expr.operand.name);
                    } else {
                        appendLine('    LEA AX, [BP' + formatBpOffset(v.bpOffset) + ']  ; &' + v.name);
                    }
                    return isCharType(v.varType) ? 'char*' : 'int*';
                } else if (expr.operand.type === 'array_index') {
                    const v = lookupVar(expr.operand.name);
                    compileArrayAddress(expr.operand);
                    return isCharType(v.varType) ? 'char*' : 'int*';
                } else {
                    throw new Error('Can only take address of variables or array elements');
                }
            }
            default:
                throw new Error('Unknown unary operator: ' + expr.op);
        }
    }

    function compileCall(expr) {
        appendLine('');
        appendLine('; Call ' + expr.name);

        if (isSysCall(expr.name)) {
            compileSysCall(expr);
        } else {
            if (expr.args.length > 6) {
                throw new Error('Cannot pass more than 6 arguments');
            }

            for (let i = 0; i < expr.args.length; i++) {
                compileExpression(expr.args[i]);
                appendLine('    PUSH AX');
            }

            const regs = ['AX', 'BX', 'CX', 'DX', 'SI', 'DI'];
            for (let i = expr.args.length - 1; i >= 0; i--) {
                appendLine('    POP ' + regs[i]);
            }

            appendLine('    CALL ' + expr.name);
        }
    }

    function isSysCall(name) {
        return name === 'print_int' || name === 'print_char' || name === 'print_string' ||
               name === 'read_int' || name === 'read_char' || name === 'exit';
    }

    function compileSysCall(call) {
        switch (call.name) {
            case 'print_int':
                if (call.args.length !== 1) throw new Error('print_int expects 1 argument');
                compileExpression(call.args[0]);
                appendLine('    SYSCALL PRINT_INT');
                break;
            case 'print_char':
                if (call.args.length !== 1) throw new Error('print_char expects 1 argument');
                compileExpression(call.args[0]);
                appendLine('    SYSCALL PRINT_CHAR');
                break;
            case 'print_string':
                if (call.args.length !== 1) throw new Error('print_string expects 1 argument');
                compileExpression(call.args[0]);
                appendLine('    SYSCALL PRINT_STRING');
                break;
            case 'read_int':
                if (call.args.length !== 0) throw new Error('read_int expects 0 arguments');
                appendLine('    SYSCALL READ_INT');
                break;
            case 'read_char':
                if (call.args.length !== 0) throw new Error('read_char expects 0 arguments');
                appendLine('    SYSCALL READ_CHAR');
                break;
            case 'exit':
                appendLine('    SYSCALL EXIT');
                break;
            default:
                throw new Error('Unknown syscall: ' + call.name);
        }
    }

    function compileVarRef(expr) {
        const v = lookupVar(expr.name);
        if (isArrayType(v.varType)) {
            if (v.global) {
                appendLine('    MOV AX, ' + v.name + '  ; &' + v.name);
            } else {
                appendLine('    LEA AX, [BP' + formatBpOffset(v.bpOffset) + ']  ; &' + v.name);
            }
            return isCharType(v.varType) ? 'char*' : 'int*';
        } else if (v.global) {
            appendLine('    MOV AX, [' + v.name + ']  ; load ' + v.name);
        } else {
            appendLine('    MOV AX, [BP' + formatBpOffset(v.bpOffset) + ']  ; load ' + v.name);
        }
        return v.varType;
    }

    function compileArrayIndex(expr) {
        const v = lookupVar(expr.name);
        compileArrayAddress(expr);
        appendLine('    MOV BX, AX');
        if (isCharType(v.varType)) {
            appendLine('    MOV AL, [BX+0]');
            return 'char';
        } else {
            appendLine('    MOV AX, [BX+0]');
            return 'int';
        }
    }

    function compileArrayAddress(expr) {
        const v = lookupVar(expr.name);
        compileExpression(expr.index);
        if (elementSize(v.varType) === 2) {
            appendLine('    ADD AX, AX');
        }
        if (v.global) {
            appendLine('    MOV BX, ' + expr.name + '  ; &' + expr.name);
        } else if (isArrayType(v.varType)) {
            appendLine('    LEA BX, [BP' + formatBpOffset(v.bpOffset) + ']');
        } else {
            appendLine('    MOV BX, [BP' + formatBpOffset(v.bpOffset) + ']  ; load ' + v.name);
        }
        appendLine('    ADD AX, BX');
    }
}

// ======================== EXPORTS ========================

export function compileCmm(source) {
    try {
        const tokens = lex(source);
        const ast = parse(tokens);
        const assembly = generate(ast);
        return { assembly, error: null };
    } catch (e) {
        return { assembly: null, error: e.message };
    }
}
