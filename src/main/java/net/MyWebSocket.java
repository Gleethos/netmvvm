package net;
//import javax.websocket.Session;

import app.AbstractViewModel;
import binding.SkinContext;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import org.json.JSONObject;

@WebSocket
public class MyWebSocket {
    private Session session;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        System.out.println("Received: " + message);

        var json = new JSONObject(message);

        if ( !json.has("type") ) return;

        String type = json.getString("type");

        if ( type.equals("getVM") ) {
            String vmId = json.getString("vmId");
            JSONObject vmJson = new JSONObject();
            AbstractViewModel vm = SkinContext.instance().get(vmId);
            vmJson.put("type", "viewModel");
            vmJson.put("viewModel", vm.toJson());
            vmJson.put("vmId", vmId);
            vm.bind( delegate -> {
                try {
                    JSONObject update = new JSONObject();
                    update.put("type", "show");
                    update.put("vmId", vmId);
                    update.put("propName", delegate.current().id());
                    update.put("value", delegate.current().get());
                    String returnJson = update.toString();
                    System.out.println("Sending property: " + returnJson);
                    session.getRemote().sendString(returnJson);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            // Send a message to the client that sent the message
            System.out.println("Sending VM: " + vmJson.toString());
            session.getRemote().sendStringByFuture(vmJson.toString());
        }
        else if ( type.equals("act") ) {
            String vmId = json.getString("vmId");
            String propName = json.getString("propName");
            String value = json.getString("value");
            AbstractViewModel vm = SkinContext.instance().get(vmId);
            vm.getPropById(propName).act(value);
        }

    }

}
