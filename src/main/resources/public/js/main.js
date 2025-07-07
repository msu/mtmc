// connect to sse endpoint
const sseSource = new EventSource("/sse", {withCredentials: true});
let buttons = 0x00;

sseSource.addEventListener("update:execution", (e) => {
    let element = document.getElementById("controls");
    element.outerHTML = e.data;
});

sseSource.addEventListener("update:filesystem", (e) => {
    let element = document.getElementById("fs");
    element.outerHTML = e.data;
});

sseSource.addEventListener("update:display", (e) => {
    let element = document.getElementById("display-img");
//    element.src = "/display?" + Date.now()
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

sseSource.onerror = (err) => {
    console.error("EventSource failed:", err);
};

document.addEventListener("mouseover", (evt) => {
    if (evt.target.matches && evt.target.matches(".reg-value")) {
        let text = evt.target.innerText;
        console.log(evt.target);
        console.log(text);
        try {
            let elementId = "mem_" + text;
            console.log()
            let elt = document.getElementById(elementId);
            console.log(elt)
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
}

function initConsole() {
    const history = document.getElementById('console-history');
    const input = document.getElementById('console-input');
    const prompt = document.getElementById('console-prompt');
    const consolePanel = document.getElementById('console-panel');
    
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