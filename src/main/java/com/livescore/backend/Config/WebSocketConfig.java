package com.livescore.backend.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Client yahin connect karega
        registry.addEndpoint("/live-score")   // URL jahan client connect hoga
                .setAllowedOriginPatterns("*");
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Ye topic prefix hai jahan server messages broadcast karega
        registry.enableSimpleBroker("/topic");

        // Ye prefix hai jahan se client message bhejega, map hota @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
    }
}
