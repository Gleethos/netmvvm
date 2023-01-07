package net;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebSocketEndpoint extends WebSocketServlet {
    @Override
    public void configure(WebSocketServletFactory factory) {
        // set a 10 second idle timeout
        factory.getPolicy().setIdleTimeout(10000);
        // register my socket
        factory.register(BindingWebSocket.class);
    }
}
