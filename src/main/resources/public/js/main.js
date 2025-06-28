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
    element.src = "/display?" + Date.now()
})

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

function initJoystick(){

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
        
        document.querySelector(key).addEventListener("mousedown", ()=> {
            buttons |= value;
            fetch("/io/" + buttons.toString(16), {method: 'POST'});
        });
        
        document.querySelector(key).addEventListener("mouseup", ()=> {
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
        
        if (!key_mappings[e.key]) return;
        if (buttons & key_mappings[e.key]) return; // Eliminate key repeat
        
        buttons |= key_mappings[e.key];
        fetch("/io/" + buttons.toString(16), {method: 'POST'});
    });
    
    display.addEventListener("keyup", (e) => {
        e.preventDefault();
        
        if (!key_mappings[e.key]) return;
        
        buttons &= ~key_mappings[e.key];
        fetch("/io/" + buttons.toString(16), {method: 'POST'});
    });
}

function initConsole() {
    const history = document.getElementById('console-history');
    const input = document.getElementById('console-input');
    const consolePanel = document.getElementById('console-panel');
    let historyIndex = -1;
    let historyStack = [];

    consolePanel.addEventListener('click', (e) => {
        if (!history.contains(e.target)) {
            input.focus();
        }
    })

    sseSource.addEventListener("console-output", (e) => {
        e.data.split("\n").forEach((txt) => {
            const line = document.createElement('DIV');
            line.textContent = txt;
            history.appendChild(line);
        })
        input.scrollIntoView({behavior: "instant"})
    })

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            historyIndex = -1;
            e.preventDefault();
            const line = document.createElement('DIV');
            let cmd = input.value;
            historyStack.unshift(cmd);
            line.textContent = `mtmc$ ${cmd}`;
            history.appendChild(line);
            fetch("/cmd", {method: 'POST', body: JSON.stringify({'cmd': cmd})})
            input.value = '';
            input.focus();
            input.scrollIntoView({behavior: "instant"})
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