package com.chatroomrealtime.config;

import com.chatroomrealtime.security.AuthChannelInterceptor;
import com.chatroomrealtime.security.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
 
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
/*
 * NGÀY 7: WebSocketConfig mở rộng — thêm JwtHandshakeInterceptor và AuthChannelInterceptor.
 * Thay thế file WebSocketConfig.java cũ (ngày 6) bằng file này.
 */
public class WebSocketSecuredConfig implements WebSocketMessageBrokerConfigurer {
 
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final AuthChannelInterceptor authChannelInterceptor;
 
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
 
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                /*
                 * addInterceptors(jwtHandshakeInterceptor):
                 * Đăng ký interceptor HTTP handshake.
                 * Chạy trước khi WebSocket connection được thiết lập.
                 * Token được validate ở đây.
                 */
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
 
        // Endpoint không SockJS cho Postman
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor);
    }
 
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        /*
         * configureClientInboundChannel: cấu hình channel xử lý message từ client → server.
         * Thêm AuthChannelInterceptor vào đây để nó chặn STOMP CONNECT frame
         * và set Principal cho session.
         */
        registration.interceptors(authChannelInterceptor);
    }
}