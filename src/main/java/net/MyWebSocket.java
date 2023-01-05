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

        if ( !json.has("type") ) {
            return;
        }

        String type = json.getString("type");

        if ( type.equals("getVM") ) {
            String vmId = json.getString("vmId");
            JSONObject vmJson = new JSONObject();
            AbstractViewModel vm = SkinContext.instance().get(vmId);
            vmJson.put("type", "viewModel");
            vmJson.put("viewModel", vm.toJson());
            vm.bind( delegate -> {
                try {
                    session.getRemote().sendString(
                        "{" +
                                "\"type\":\"show\"," +
                                "\"vmId\":\""+ vmId +"\"," +
                                "\"propName\":\"" + delegate.current().id() + "\"," +
                                "\"value\":\""+ delegate.current().get() +"\"" +
                            "}"
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            // Send a message to the client that sent the message
            session.getRemote().sendStringByFuture(vmJson.toString());
        }
        else if ( type.equals("act") ) {
            System.out.println("act");
        }

    }

}
