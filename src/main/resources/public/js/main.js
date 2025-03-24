// connect to sse endpoint
const sseSource = new EventSource("/sse", {withCredentials:true});

sseSource.addEventListener("update", (e) => {
    let updateInfo = JSON.parse(e.data);
    for(let id in updateInfo) {
        let element = document.querySelector("#" + id);
        element.outerHTML = updateInfo[id]
    }
})

sseSource.onerror = (err) => {
    console.error("EventSource failed:", err);
};