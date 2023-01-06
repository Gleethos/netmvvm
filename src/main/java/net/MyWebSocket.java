package net;

import app.AbstractViewModel;
import binding.SkinContext;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class MyWebSocket {

    private final static Logger log = LoggerFactory.getLogger(MyWebSocket.class);

    private Session session;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        System.out.println("Received: " + message);

        var json = new JSONObject(message);

        if ( !json.has(Constants.EVENT_TYPE) ) return;

        String type = json.getString(Constants.EVENT_TYPE);

        if ( type.equals(Constants.GET_VM) ) {
            try {
                sendVMToFrontend(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if ( type.equals(Constants.SET_PROP) ) {
            try {
                applyMutationToVM(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void sendVMToFrontend(JSONObject json) {
        String vmId = json.getString(Constants.VM_ID);
        JSONObject vmJson = new JSONObject();
        AbstractViewModel vm = SkinContext.instance().get(vmId);
        vmJson.put(Constants.EVENT_TYPE, Constants.RETURN_GET_VM);
        vmJson.put(Constants.RETURN_GET_VM, vm.toJson());
        vmJson.put(Constants.VM_ID, vmId);
        vm.bind( delegate -> {
            try {
                JSONObject update = new JSONObject();
                update.put(Constants.EVENT_TYPE, Constants.RETURN_PROP);
                update.put(Constants.VM_ID, vmId);
                update.put(Constants.PROP_NAME, delegate.current().id());
                update.put(Constants.PROP_VALUE, delegate.current().get());
                String returnJson = update.toString();
                System.out.println("Sending property: " + returnJson);
                session.getRemote().sendString(returnJson);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // Send a message to the client that sent the message
        System.out.println("Sending VM: " + vmJson);
        session.getRemote().sendStringByFuture(vmJson.toString());
    }

    private void applyMutationToVM(JSONObject json) {
        String vmId     = json.getString(Constants.VM_ID);
        String propName = json.getString(Constants.PROP_NAME);
        String value    = json.getString(Constants.PROP_VALUE);
        AbstractViewModel vm = SkinContext.instance().get(vmId);
        vm.applyToPropertyById(propName, value);
    }

}
