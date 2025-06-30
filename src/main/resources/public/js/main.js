// connect to sse endpoint
const sseSource = new EventSource("/sse", {withCredentials: true});

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

    // buttons
    let up = document.querySelector(".cross-button.up");
    up.addEventListener("mousedown", ()=>{
        fetch("/io/up/pressed", {method: 'POST'})
    })
    up.addEventListener("mouseup", ()=>{
        fetch("/io/up/released", {method: 'POST'})
    })
    
    let down = document.querySelector(".cross-button.down");
    down.addEventListener("mousedown", ()=>{
        fetch("/io/down/pressed", {method: 'POST'})
    })
    down.addEventListener("mouseup", ()=>{
        fetch("/io/down/released", {method: 'POST'})
    })
    
    let left = document.querySelector(".cross-button.left");
    left.addEventListener("mousedown", ()=>{
        fetch("/io/left/pressed", {method: 'POST'})
    })
    left.addEventListener("mouseup", ()=>{
        fetch("/io/left/released", {method: 'POST'})
    })
    
    let right = document.querySelector(".cross-button.right");
    right.addEventListener("mousedown", ()=>{
        fetch("/io/right/pressed", {method: 'POST'})
    })
    right.addEventListener("mouseup", ()=>{
        fetch("/io/right/released", {method: 'POST'})
    })
    
    let select = document.querySelector(".small-button.select");
    select.addEventListener("mousedown", ()=>{
        fetch("/io/select/pressed", {method: 'POST'})
    })
    select.addEventListener("mouseup", ()=>{
        fetch("/io/select/released", {method: 'POST'})
    })
    
    let start = document.querySelector(".small-button.start");
    start.addEventListener("mousedown", ()=>{
        fetch("/io/start/pressed", {method: 'POST'})
    })
    start.addEventListener("mouseup", ()=>{
        fetch("/io/start/released", {method: 'POST'})
    })
    
    let b = document.querySelector(".big-button.b");
    b.addEventListener("mousedown", ()=>{
        fetch("/io/b/pressed", {method: 'POST'})
    })
    b.addEventListener("mouseup", ()=>{
        fetch("/io/b/released", {method: 'POST'})
    })

    let a = document.querySelector(".big-button.a");
    a.addEventListener("mousedown", ()=>{
        fetch("/io/a/pressed", {method: 'POST'})
    })
    a.addEventListener("mouseup", ()=>{
        fetch("/io/a/released", {method: 'POST'})
    })

    // keys
    let display = document.getElementById('display');
    display.addEventListener("keydown", (e) => {
        e.preventDefault()
        if (e.key === "ArrowUp") {
            fetch("/io/up/pressed", {method: 'POST'})
        }
        if (e.key === "ArrowLeft") {
            fetch("/io/left/pressed", {method: 'POST'})
        }
        if (e.key === "ArrowRight") {
            fetch("/io/right/pressed", {method: 'POST'})
        }
        if (e.key === "ArrowDown") {
            fetch("/io/down/pressed", {method: 'POST'})
        }
        if (e.key === "l") {
            fetch("/io/select/pressed", {method: 'POST'})
        }
        if (e.key === " ") {
            fetch("/io/start/pressed", {method: 'POST'})
        }
        if (e.key === "a") {
            fetch("/io/b/pressed", {method: 'POST'})
        }
        if (e.key === "s") {
            fetch("/io/a/pressed", {method: 'POST'})
        }
    })
    display.addEventListener("keyup", (e) => {
        e.preventDefault()
        if (e.key === "ArrowUp") {
            fetch("/io/up/released", {method: 'POST'})
        }
        if (e.key === "ArrowLeft") {
            fetch("/io/left/released", {method: 'POST'})
        }
        if (e.key === "ArrowRight") {
            fetch("/io/right/released", {method: 'POST'})
        }
        if (e.key === "ArrowDown") {
            fetch("/io/down/released", {method: 'POST'})
        }
        if (e.key === "l") {
            fetch("/io/select/released", {method: 'POST'})
        }
        if (e.key === " ") {
            fetch("/io/start/released", {method: 'POST'})
        }
        if (e.key === "a") {
            fetch("/io/b/released", {method: 'POST'})
        }
        if (e.key === "s") {
            fetch("/io/a/released", {method: 'POST'})
        }
    })
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