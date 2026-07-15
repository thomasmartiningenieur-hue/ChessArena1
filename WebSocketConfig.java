package com.chessarena.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Préfixe pour les messages envoyés PAR le serveur vers les clients abonnés
        registry.enableSimpleBroker("/topic", "/queue");
        // Préfixe pour les messages envoyés PAR les clients vers le serveur (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");
        // Préfixe pour les messages privés (ex: notifications ciblées à un joueur)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:4200")
                .withSockJS();
    }
}
