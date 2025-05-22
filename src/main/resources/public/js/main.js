// connect to sse endpoint
const sseSource = new EventSource("/sse", {withCredentials: true});

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

    const history = document.getElementById('console-history');
    const input = document.getElementById('console-input');
    const consolePanel = document.getElementById('console-panel');
    let historyIndex = -1;
    let historyStack = [];

    consolePanel.addEventListener('click', (e)=> {
        if (!history.contains(e.target)) {
            input.focus();
        }
    })

    sseSource.addEventListener("console-output", (e) => {
        e.data.split("\n").forEach((txt)=>{
            const line = document.createElement('DIV');
            line.textContent = txt;
            history.appendChild(line);
        })
        input.scrollIntoView({behavior:"instant"})
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
            input.scrollIntoView({behavior:"instant"})
        }
        if (e.key === 'ArrowUp') {
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
                historyIndex = 0;
                input.value = ""
            } else {
                input.value = historyStack[historyIndex];
            }
        }
    });

    input.focus();
})