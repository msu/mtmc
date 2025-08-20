package mtmc.web.teavm;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSExport;

public class WebWorker {
    
    private static int pingCounter = 0;
    
    public static void main(String[] args) {
        setupMessageListener();
        // Example: Send an update:execution event after initialization
        sendEvent("update:execution", "<div>Worker initialized</div>");
    }
    
    @JSExport
    public static void handleMessage(String type, String payload) {
        if ("ping".equals(type)) {
            pingCounter++;
            sendMessage("pong", "Pong #" + pingCounter + " - " + payload);
        } else {
            System.out.println("WebWorker got unhandled message: " + type + ", " + payload);
        }
    }
    
    /**
     * Send SSE to the frontend
     * Usage: WebWorker.sendEvent("update:execution", htmlContent);
     */
    public static void sendEvent(String eventType, String data) {
        sendMessage("sse:" + eventType, data);
    }
    
    public static void sendMessage(String type, String payload) {
        postMessage(createMessage(type, payload));
    }
    
    @JSBody(params = { "message" }, script = "postMessage(message);")
    private static native void postMessage(JSObject message);
    
    @JSBody(params = { "type", "payload" }, script = "return { type: type, payload: payload };")
    private static native JSObject createMessage(String type, String payload);
    
    @JSBody(script = "self.addEventListener('message', function(e) { handleMessage(e.data.type || '', e.data.payload || ''); });")
    private static native void setupMessageListener();
}

