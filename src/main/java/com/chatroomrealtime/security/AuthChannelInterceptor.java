package com.chatroomrealtime.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
 
import java.util.Map;
 
@Component
@RequiredArgsConstructor
@Slf4j
/*
 * NGÀY 7: AuthChannelInterceptor — chạy với mỗi STOMP frame (message) qua channel.
 *
 * Tại sao cần thêm interceptor này sau khi đã có JwtHandshakeInterceptor?
 *
 * JwtHandshakeInterceptor chỉ chạy 1 lần khi kết nối.
 * Nó lưu username vào session attributes nhưng chưa set Authentication
 * vào STOMP message headers — nên Principal trong ChatController vẫn null.
 *
 * AuthChannelInterceptor bắt STOMP CONNECT frame,
 * đọc username từ session attributes,
 * tạo Authentication object và set vào STOMP session.
 * Từ đó mọi message tiếp theo trong session đó đều có Principal.
 *
 * Tóm tắt flow:
 * HTTP Handshake → JwtHandshakeInterceptor (validate token, lưu username)
 *      ↓
 * STOMP CONNECT → AuthChannelInterceptor (set Principal từ username)
 *      ↓
 * STOMP SEND /app/chat/1 → ChatController (Principal đã có, dùng được)
 */
public class AuthChannelInterceptor implements ChannelInterceptor {
 
    private final UserDetailsService userDetailsService;
 
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);
 
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            /*
             * Bắt STOMP CONNECT frame — frame đầu tiên client gửi sau khi WebSocket kết nối.
             * Đây là thời điểm đúng để set Principal cho toàn bộ session.
             */
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
 
            if (sessionAttributes != null && sessionAttributes.containsKey("username")) {
                String username = (String) sessionAttributes.get("username");
                log.info("STOMP CONNECT — set Principal cho user: {}", username);
 
                // Load đầy đủ thông tin user từ DB
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
 
                /*
                 * Tạo Authentication object và set vào STOMP session.
                 * accessor.setUser(principal): Spring lưu principal này vào session.
                 * Mọi message tiếp theo từ session này đều có principal tương ứng.
                 * ChatController nhận qua parameter Principal principal — luôn có giá trị.
                 */
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
 
                accessor.setUser(auth);
                log.info("Principal đã được set cho session của user: {}", username);
            } else {
                log.warn("STOMP CONNECT không có username trong session attributes");
            }
        }
 
        return message;
    }
}
