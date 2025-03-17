// connect to sse endpoint
const eventSource = new EventSource("/sse", {withCredentials:true});

eventSource.addEventListener("update", (e) => {
    let updateInfo = JSON.parse(e.data);
    for(let id in updateInfo) {
        let element = document.querySelector("#" + id);
        element.outerHTML = updateInfo[id]
    }
})

eventSource.onerror = (err) => {
    console.error("EventSource failed:", err);
};