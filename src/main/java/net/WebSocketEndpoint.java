package net;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebSocketEndpoint extends WebSocketServlet {
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.register(MyWebSocket.class);
    }
}
