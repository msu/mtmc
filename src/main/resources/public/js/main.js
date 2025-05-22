// connect to sse endpoint
const sseSource = new EventSource("/sse", {withCredentials: true});

sseSource.addEventListener("update:display", (e) => {
    console.log("here")
    let element = document.getElementById("display-img");
    console.log("here", element)
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
