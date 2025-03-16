// connect to sse endpoint
const eventSource = new EventSource("/sse", {withCredentials:true});

eventSource.addEventListener("console", (e) => {
    console.log(e);
});

eventSource.onerror = (err) => {
    console.error("EventSource failed:", err);
};