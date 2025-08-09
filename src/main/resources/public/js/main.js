// connect to sse endpoint
const sseSource = new EventSource("/sse", {withCredentials: true});
let buttons = 0x00;
let gamepad = null;

sseSource.addEventListener("update:execution", (e) => {
    let element = document.getElementById("controls");
    element.outerHTML = e.data;
});

sseSource.addEventListener("update:filesystem", (e) => {
    let element = document.getElementById("visual-shell");
    element.outerHTML = e.data;
});

sseSource.addEventListener("update:display", (e) => {
    let element = document.getElementById("display-img");
    element.src = e.data;
});

sseSource.addEventListener("update:registers", (e) => {
    let element = document.getElementById("register-panel");
    element.outerHTML = e.data
})

sseSource.addEventListener("update:memory", (e) => {
    let element = document.getElementById("memory-table");
    element.outerHTML = e.data
})

sseSource.addEventListener("update:memory-panel", (e) => {
    let element = document.getElementById("memory-panel");
    element.outerHTML = e.data
})

sseSource.addEventListener("update:step-execution", (e) => {
    let step = JSON.parse(e.data);
    if (window.stepExecution) {
        window.stepExecution(step);
    }
});

sseSource.onerror = (err) => {
    console.error("EventSource failed:", err);
};

document.addEventListener("mouseover", (evt) => {
    if (evt.target.matches && evt.target.matches(".reg-value")) {
        let text = evt.target.innerText;
        try {
            let elementId = "mem_" + text;
            let elt = document.getElementById(elementId);
            if (elt) {
                elt.classList.add("mem-highlight")
            }
        } catch {
            // ignore
        }
    }
})

document.addEventListener("mouseout", (evt) => {
    if (evt.target.matches && evt.target.matches(".reg-value")) {
        let text = evt.target.innerText;
        try {
            let elt = document.getElementById("mem_" + text);
            if (elt) {
                elt.classList.remove("mem-highlight")
            }
        } catch {
            // ignore
        }
    }
})

document.addEventListener("click", (evt) => {
    if (evt.target.matches && evt.target.matches(".reg-value")) {
        evt.preventDefault();
        let text = evt.target.innerText;
        try {
            let elt = document.getElementById("mem_" + text);
            if (elt) {
                elt.scrollIntoView({behavior:"smooth", block:"start"});
            }
        } catch {
            // ignore
        }
    }
})

document.addEventListener("DOMContentLoaded", () => {
    initJoystick();
    initConsole();
})

function initJoystick() {
    
    // disable default events for control area to prevent browser from reacting
    document.getElementById("display-control-wrapper").addEventListener("mousedown", (e)=> {
        e.preventDefault();
        e.stopPropagation();
        
        return false;
    });
    
    document.getElementById("display-control-wrapper").addEventListener("touchstart", (e)=> {
        e.preventDefault();
        e.stopPropagation();
        
        return false;
    });

    // key mappings
    virtual_button_mappings = {
        ".cross-button.up": 0x80,
        ".cross-button.down": 0x40,
        ".cross-button.left": 0x20,
        ".cross-button.right": 0x10,
        ".small-button.start": 0x08,
        ".small-button.select": 0x04,
        ".big-button.b": 0x02,
        ".big-button.a": 0x01
    };
    
    // virtual buttons
    Object.keys(virtual_button_mappings).forEach(key => {
        const value = virtual_button_mappings[key];
        
        document.querySelector(key).addEventListener("mousedown", (e)=> {
            e.preventDefault();
            e.stopPropagation();
            buttons |= value;
            fetch("/io/" + buttons.toString(16), {method: 'POST'});
        });
        
        document.querySelector(key).addEventListener("mouseup", (e)=> {
            e.preventDefault();
            e.stopPropagation();
            buttons &= ~value;
            fetch("/io/" + buttons.toString(16), {method: 'POST'});
        });
        
        document.querySelector(key).addEventListener("touchstart", (e)=> {
            e.preventDefault();
            e.stopPropagation();
            buttons |= value;
            fetch("/io/" + buttons.toString(16), {method: 'POST'});
        });
        
        document.querySelector(key).addEventListener("touchend", (e)=> {
            e.preventDefault();
            e.stopPropagation();
            buttons &= ~value;
            fetch("/io/" + buttons.toString(16), {method: 'POST'});
        });
    });
    
    // key mappings
    key_mappings = {
        "ArrowUp": 0x80,
        "ArrowDown": 0x40,
        "ArrowLeft": 0x20,
        "ArrowRight": 0x10,
        "l": 0x08,
        " ": 0x04,
        "a": 0x02,
        "s": 0x01
    };

    // keys
    let display = document.getElementById('display');
    display.addEventListener("keydown", (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        if (!key_mappings[e.key]) return;
        if (buttons & key_mappings[e.key]) return; // Eliminate key repeat
        
        buttons |= key_mappings[e.key];
        fetch("/io/" + buttons.toString(16), {method: 'POST'});
    });
    
    display.addEventListener("keyup", (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        if (!key_mappings[e.key]) return;
        
        buttons &= ~key_mappings[e.key];
        fetch("/io/" + buttons.toString(16), {method: 'POST'});
    });
    
    
    // key mappings
    gamepad_mappings = {
        12: 0x80,
        13: 0x40,
        14: 0x20,
        15: 0x10,
        9: 0x08,
        8: 0x04,
        0: 0x02,
        1: 0x01
    };
    
    // gamepad
    window.addEventListener("gamepadconnected", (evt) => {
        gamepad = evt.gamepad;
        requestAnimationFrame(checkGamepadInput);
        console.log("Gamepad connected", gamepad);
    });
    
    window.addEventListener("gamepaddisconnected", (evt) => {
        if (gamepad === evt.gamepad) {
            gamepad = null;
            console.log("Gamepad disconnected", gamepad);
        }
    });
    
    function checkGamepadInput() {
        var last = buttons;

        if(!gamepad) return; // Not polling anymore

        navigator.getGamepads(); // Poll gamepads

        Object.keys(gamepad_mappings).forEach(button => {
            if (gamepad.buttons[button].pressed) buttons |= gamepad_mappings[button];
            else buttons &= ~gamepad_mappings[button];
        });
        
        if (buttons !== last) {
            fetch("/io/" + buttons.toString(16), {method: 'POST'});
        }
        
        requestAnimationFrame(checkGamepadInput);
    }
}

function initConsole() {
    const history = document.getElementById('console-history');
    const input = document.getElementById('console-input');
    const prompt = document.getElementById('console-prompt');
    const consolePanel = document.getElementById('console-panel');
    const consolePartial = document.getElementById('console-partial');
    
    let historyIndex = -1;
    let historyStack = [];
    let readChar = false;
    let readString = false;
    let readInt = false;
    
    let startClick = 0;


    consolePanel.addEventListener('mousedown', (e) => {
        startClick = Date.now();
    });

    consolePanel.addEventListener('mouseup', (e) => {
        if ((Date.now() - startClick) < 200) {
            input.focus();
        }
    });

    sseSource.addEventListener("console-output", (e) => {
        consolePartial.textContent = "";
        e.data.split("\n").forEach((txt) => {
            const line = document.createElement('DIV');
            line.textContent = txt;
            history.appendChild(line);
            while (history.length > 1000) {
                history.removeChild(history.firstElementChild);
            }
        });
        input.scrollIntoView({behavior: "instant"});
    });
    
    sseSource.addEventListener("console-partial", (e) => {
        consolePartial.textContent = e.data;
        input.scrollIntoView({behavior: "instant"});
    });
    
    sseSource.addEventListener("console-ready", (e) => {
        var text = e.data.trim() || "mtmc$";
        prompt.textContent = text;
        readString = false;
        readChar = false;
        readInt = false;
    });
    
    sseSource.addEventListener("console-readstr", (e) => {
        var text = e.data.trim() || ">";
        prompt.textContent = text;
        readString = true;
    });
    
    sseSource.addEventListener("console-readchar", (e) => {
        var text = e.data.trim() || ">";
        prompt.textContent = text;
        readChar = true;
    });
    
    sseSource.addEventListener("console-readint", (e) => {
        var text = e.data.trim() || "#";
        prompt.textContent = text;
        readString = true;
        readInt = true;
    });

    input.addEventListener('keydown', (e) => {
        
        if (readChar) {
            e.preventDefault();
            e.stopPropagation();
            
            if(e.key.length === 1) {
                fetch("/readchar", {method: 'POST', body: JSON.stringify({'c': e.key})});
            }

            return false;
        } 
        
        if (e.key === 'Enter') {
            historyIndex = -1;
            e.preventDefault();
            const line = document.createElement('DIV');
            let cmd = input.value;
            
            if (!(readString || readChar)) {
                historyStack.unshift(cmd);
            }
            
            line.textContent = `${prompt.textContent} ${cmd}`;
            history.appendChild(line);
            
            if (readInt) {
                fetch("/readint", {method: 'POST', body: JSON.stringify({'str': cmd})});
            } else if (readString) {
                fetch("/readstr", {method: 'POST', body: JSON.stringify({'str': cmd})});
            } else {
                fetch("/cmd", {method: 'POST', body: JSON.stringify({'cmd': cmd})});
            }
            
            input.value = '';
            input.focus();
            input.scrollIntoView({behavior: "instant"});
        }
        if (readInt) {
            let c = e.key;
            
            if (c === "Backspace" || c === "Delete" || (input.value.length < 1 && c === '-')) {
                return;
            }
            if (c.length !== 1 || c < '0' || c > '9') {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
        }
        if (e.key === 'ArrowUp') {
            if (!historyStack.length) return;
            
            historyIndex++;
            e.preventDefault();
            if (historyIndex >= historyStack.length) {
                historyIndex = historyStack.length - 1;
            }
            input.value = historyStack[historyIndex];
        }
        if (e.key === 'ArrowDown') {
            historyIndex--;
            e.preventDefault();
            if (historyIndex < 0) {
                historyIndex = -1;
                input.value = "";
            } else {
                input.value = historyStack[historyIndex];
            }
        }
    });

    input.focus();
}

// Monaco editor support tools
async function startMonaco() {
    var editor_div = document.getElementById('editor');
    var editor_save = document.getElementById('editor-save');
    var editor_close = document.getElementById('editor-close');
    
    var viewer_div = document.getElementById('assembly');
    var viewer = null;
    
    var filename = editor_div.dataset.filename;
    var response = await fetch("/fs/read" + filename);
    var text = await response.text();
    var stepHighlight = [];
    var viewerHighlight = [];
    var breakpoints = [];
    var ignoreChange = false;
    
    var theme = "vs";
    var language = "plaintext";
    
    switch(editor_div.dataset.mime) {
        case "text/x-asm":
            language = "mtmc16-asm";
            theme = "mtmc16-asm";
            break;
        case "text/x-csrc":
            language = "c";
            break;
        case "application/json":
        case "text/mtmc16-bin":
            language = "json";
            break;
    }
    
    for (var line of JSON.parse(editor_div.dataset.breakpoints)) {
        breakpoints[line] = true;
    }
    
    function lineRenderer(line) {
        if(breakpoints[line]) return "<span style=\"font-size: 60%;\">&#128308;</span>";
        
        return line;
    }
    
    var editor = monaco.editor.create(editor_div, {
        value: text,
        language: language,
        theme: theme,
        automaticLayout: true,
	lineNumbers: lineRenderer
    });
    
    function save() {
        if (editor_save.hasAttribute("disabled")) {
            return;
        }
        fetch("/fs/write" + editor_div.dataset.filename, {method: 'POST', body: editor.getValue()});
        editor_save.setAttribute("disabled", "disabled");
    };
    
    editor_save.onclick = save;
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KEY_S, save); // Not working for some reason
    editor_div.addEventListener("keydown", function(e) {  // fallback
        if ((e.ctrlKey || e.metaKey) && e.key === 's') {
            e.preventDefault();
            e.stopPropagation();
            save();
        }
    });
    
    editor.getModel().onDidChangeContent(function(event) {
        if (ignoreChange) {
            ignoreChange = false;
            return;
        }
        
        editor_save.removeAttribute("disabled");
    });
    
    editor_close.addEventListener("fx:config", (evt) => {
        var message = "You have unsaved work. Are you sure you want to close?";
        if (!editor_save.hasAttribute("disabled")) {
            evt.detail.cfg.confirm = () => confirm(message);
        }
    });
    
    editor.onMouseDown(async function(event) {
        var info = event.target;
        var line = info.position.lineNumber;
        var model = editor.getModel();
        var content = model.getLineContent(line);

        if (!content) return;
        if (!("glyphMarginLeft" in info.detail)) return;
        
        breakpoints[line] = !breakpoints[line];
        ignoreChange = true;

        var response = await fetch("/breakpoint/" + line + "/" + breakpoints[line], {method: 'POST'});
        var result = await response.text();
        
        breakpoints[line] = (result === "true");
        
        editor.executeEdits(
            "force-re-render-" + line,
            [{
                identifier: {major: 0, minor: 0},
                range: new monaco.Range(line, 1, line, model.getLineMaxColumn(line)),
                text: content
            }]
        );
    });
    
    function renderHighlight(editor, highlight, line) {
        var model = editor.getModel();
        
        if (line < 1) {
            return model.deltaDecorations(highlight, []);
        }
        
        var range = new monaco.Range(line, 1, line, model.getLineMaxColumn(line));
        var options = { isWholeLine: true, inlineClassName: 'step-highlight' };
        var decoration = {range: range, options: options};
        
        highlight = model.deltaDecorations(highlight, [decoration]);

        editor.revealLineInCenter(line);
        
        return highlight;
    }
    
    window.stepExecution = async function(step) {
        var type = filename.toLowerCase().endsWith(".asm") ? "asm" : "src";

        if (step.program !== filename) {
            return;
        }
        
        if (!viewer && type === "src") {
            var response = await fetch("/asm");
            var code = await response.text();
            
            viewer_div.classList.remove("hidden");
            
            viewer = monaco.editor.create(viewer_div, {
                value: code,
                language: "mtmc16-asm",
                theme: "mtmc16-asm",
                automaticLayout: true,
                readOnly: true
            });
        }
        
        stepHighlight = renderHighlight(editor, stepHighlight, step[type]);
        
        if (viewer) {
            viewerHighlight = renderHighlight(viewer, viewerHighlight, step.asm);
        }
    };
}

function fullscreen(id, event) {
    document.getElementById(id).classList.add("fullscreen");
    
    if (event) {
        event.preventDefault();
        event.stopPropagation();
        event.stopImmediatePropagation();
    }
}

function restore(id) {
    document.getElementById(id).classList.remove("fullscreen");
    
    if (event) {
        event.preventDefault();
        event.stopPropagation();
        event.stopImmediatePropagation();
    }
}

document.addEventListener("fx:swapped", (evt) => {
    var action = evt.detail.cfg.action;

    if (action.startsWith("/fs/open/") || action.startsWith("/fs/create")) {
        startMonaco();
    }
});

document.addEventListener("DOMContentLoaded", function() {
    if (document.getElementById("editor")) {
        startMonaco();
    }
});