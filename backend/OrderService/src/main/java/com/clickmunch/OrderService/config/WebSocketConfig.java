package com.clickmunch.OrderService.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket setup for pushing kitchen events to chef clients.
 *
 * Endpoint: /ws/kitchen
 *   - Reached directly at ws://orderservice:8085/ws/kitchen (service-to-service)
 *   - Reached from browsers at ws://localhost:8080/ws/order/ (proxied by the
 *     API Gateway with JWT handshake).
 *
 * Topics:
 *   /topic/kitchen/{restaurantId}  - ORDER_CREATED, ORDER_STATUS_CHANGED events
 *     scoped to a single restaurant so chefs only see their kitchen.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for /topic/** fan-out (sufficient for a single
        // OrderService replica; switch to RabbitMQ/ActiveMQ relay if we scale out).
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/kitchen")
                .setAllowedOriginPatterns("*");
        // SockJS fallback is intentionally omitted: the gateway proxies a raw
        // WebSocket upgrade; clients use @stomp/stompjs with a ws:// URL.
    }
}
