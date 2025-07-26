(function() {
    const types = [ '.byte', '.int', '.image' ];
    
    const sections = [ '.data', '.text' ];
    
    const instructions = [
        'mov', 'lbo', 'lwo', 'eqi', 'eq', 'lb', 'lw', 'li', 'sys',
        'j', 'jnz', 'jz', 'jal', 'inc', 'dec', 'push', 'pop',
        'sw', 'sb', 'swo', 'sbo', 'ret', 'mul', 'add', 'sub',
        'div', 'and', 'or', 'xor', 'lt', 'lti', 'gt', 'gti', 'la',
        'lbr', 'lwr', 'sbr', 'swr', 'seti', 'nop', 'mod', 'shl',
        'shr', 'min', 'max', 'not', 'lnot', 'neg', 'imm', 'dup',
        'swap', 'drop', 'over', 'rot', 'sop', 'pushi', 'neq', 'gte',
        'gti', 'gtei', 'lte', 'ltei', 'neqi', 'jr'
    ];
    
    const registers = [
        't0', 't1', 't2', 't3', 't4', 't5',
        'a0', 'a1', 'a2', 'a3', 'a4', 'a5',
        'ra', 'rv', 'fp', 'sp', 'bp', 'pc'
    ];
    
    const syscalls = [
        'exit', 'wstr', 'rstr', 'wint', 'rint', 'wchr', 'rchr', 'drawimg', 
        'fbreset', 'fbflush', 'fbrect', 'fbline', 'rnd', 'sleep', 'timer', 
        'joystick', 'scolor', 'memcpy', 'dfile', 'drawimgsz', 'drawimgclip'
    ];
    
    function spacePad(value, length) {
        while (value.length < length) {
            value += " ";
        }
        
        return value;
    }
    
    monaco.languages.register({ id: 'mtmc16-asm' });

    monaco.languages.setMonarchTokensProvider('mtmc16-asm', {
        // Set defaultToken to invalid to see what you do not tokenize yet
        // defaultToken: 'invalid',

        defaultToken: 'invalid',
        ignoreCase: true,

        keywords: instructions,
        typeKeywords: types,
        registers: registers,
        syscalls: syscalls,
        sections: sections,

        operators: [],

        // we include these common regular expressions
        symbols:  /[]+/,

        // C# style strings
        escapes: /\\(?:[abfnrtv\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,

        // The main tokenizer for our languages
        tokenizer: {
          root: [
            // identifiers and keywords 
            [/[a-z_0-9]+[:]/, 'type.variable'],

            [/\.[a-z_$][\w$]*/, { cases: { '@typeKeywords': 'keyword',
                                           '@sections': 'section' } }],
                             
            [/[a-z_$][\w$]*/, { cases: { '@syscalls': 'syscall',
                                         '@registers': 'constant',
                                         '@keywords': 'keyword',
                                         '@default': 'identifier' } }],
            [/[A-Z][\w\$]*/, 'type.identifier' ],  // to show class names nicely

            // whitespace
            { include: '@whitespace' },

            // delimiters and operators
            [/[{}]/, '@brackets'],
            [/@symbols/, { cases: { '@operators': 'operator',
                                    '@default'  : '' } } ],

            // @ annotations.
            // As an example, we emit a debugging log message on these tokens.
            // Note: message are supressed during the first load -- change some lines to see them.
            [/@\s*[a-zA-Z_\$][\w\$]*/, { token: 'annotation', log: 'annotation token: $0' }],

            // numbers
            [/0[bB][01_]+/, 'number.binary'],
            [/0[xX][0-9a-fA-F]+/, 'number.hex'],
            [/[\-]?\d+/, 'number'],

            // delimiter: after number because of .\d floats
            [/[]/, 'delimiter'],

            // strings
            [/"([^"\\]|\\.)*$/, 'string.invalid' ],  // non-teminated string
            [/"/,  { token: 'string.quote', bracket: '@open', next: '@string' } ],

            // characters
            [/'[^\\']'/, 'string'],
            [/(')(@escapes)(')/, ['string','string.escape','string']],
            [/'/, 'string.invalid']
          ],

          comment: [
            [/#.*$/, 'comment' ]
          ],

          string: [
            [/[^\\"]+/,  'string'],
            [/@escapes/, 'string.escape'],
            [/\\./,      'string.escape.invalid'],
            [/"/,        { token: 'string.quote', bracket: '@close', next: '@pop' } ]
          ],

          whitespace: [
            [/[ \t\r\n]+/, 'white'],
            [/#.*$/,    'comment']
          ]
        }
    });

    monaco.editor.defineTheme("mtmc16-asm", {
        base: "vs",
        inherit: true,
        rules: [
            { token: "type.variable", foreground: "444444", fontStyle: "bold" },
            { token: "string", foreground: "1E9347" },
            { token: "comment", foreground: "969696" },
            { token: "number", foreground: "CE54B8" },
            { token: "number.hex", foreground: "CE54B8" },
            { token: "syscall", foreground: "d15b00", fontStyle: "bold" },
            { token: "section", foreground: "336cdd", fontStyle: "bold italic" }

        ],
        colors: {
            "editor.foreground": "#000000",
            "editor.background": "#FFFFFF"
        }
    });

    monaco.languages.registerCompletionItemProvider("mtmc16-asm", {
        provideCompletionItems: (model, position) => {

            var word = model.getWordUntilPosition(position);
            var previous = model.getLineContent(position.lineNumber).substring(0, word.startColumn-1).trim();
            var range = {
                    startLineNumber: position.lineNumber,
                    endLineNumber: position.lineNumber,
                    startColumn: word.startColumn,
                    endColumn: word.endColumn
            };

            var suggestions = [];

            // TODO: This needs a suggestion tree. Also, how to get labels?
            if (previous.length < 1) {
                for (var keyword of instructions) {
                    suggestions.push({
                        label: keyword,
                        kind: monaco.languages.CompletionItemKind.Keyword,
                        insertText: spacePad(keyword, 5),
                        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                        range: range
                    });
                }
            } else if (previous.endsWith('.')) {
                for (var keyword of types) {
                    suggestions.push({
                        label: keyword,
                        kind: monaco.languages.CompletionItemKind.Keyword,
                        insertText: keyword.substring(1) + ' ',
                        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                        range: range
                    });
                }
                
                for (var keyword of sections) {
                    suggestions.push({
                        label: keyword,
                        kind: monaco.languages.CompletionItemKind.Keyword,
                        insertText: keyword.substring(1) + ' ',
                        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                        range: range
                    });
                }
            } else if (previous === "sys") {
                for (var syscall of syscalls) {
                    suggestions.push({
                        label: syscall,
                        kind: monaco.languages.CompletionItemKind.Keyword,
                        insertText: syscall,
                        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                        range: range
                    });
                }
            } else if (instructions.includes(previous)) {
                for (var register of registers) {
                    suggestions.push({
                        label: register,
                        kind: monaco.languages.CompletionItemKind.Keyword,
                        insertText: register + " ",
                        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                        range: range
                    });
                }
            } 

            return { suggestions: suggestions };
        }
    });
})();