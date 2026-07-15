package com.chessarena.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEvents {

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        log.info("Nouvelle connexion WebSocket : session {}", event.getMessage().getHeaders().get("simpSessionId"));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.info("Déconnexion WebSocket : session {}", event.getSessionId());
    }
}
