package com.chatroomrealtime.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
 
import java.util.Map;
 
@Component
@RequiredArgsConstructor
@Slf4j
/*
 * NGÀY 7: JwtHandshakeInterceptor — chạy TRƯỚC khi WebSocket connection được thiết lập.
 *
 * Flow kết nối WebSocket có JWT:
 * 1. Client gửi HTTP Upgrade request tới ws://localhost:8080/ws?token=eyJ...
 * 2. Interceptor này chạy: đọc token từ query param, validate, lưu vào attributes
 * 3. Nếu token hợp lệ → connection được thiết lập
 * 4. Nếu token không hợp lệ → từ chối kết nối (return false)
 *
 * Tại sao không dùng JwtAuthFilter (cái đã có)?
 * JwtAuthFilter là một Servlet Filter — chạy với HTTP request thông thường.
 * WebSocket handshake là một loại HTTP request đặc biệt, nhưng sau khi upgrade
 * thành WebSocket, nó không còn là HTTP nữa. Nên cần interceptor riêng.
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
 
    private final JwtService jwtService;
 
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
 
        log.info("WebSocket handshake interceptor đang chạy...");
 
        if (request instanceof ServletServerHttpRequest servletRequest) {
            /*
             * Client có thể gửi token theo 2 cách:
             * Cách 1: Query param → ws://localhost:8080/ws?token=eyJ...
             * Cách 2: Header → Authorization: Bearer eyJ...
             *
             * Postman WS dùng query param tiện hơn.
             * Frontend app thực tế thường dùng header.
             * Ở đây hỗ trợ cả hai.
             */
            String token = servletRequest.getServletRequest().getParameter("token");
 
            if (token == null) {
                // Thử đọc từ Authorization header
                String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
 
            if (token != null) {
                try {
                    String username = jwtService.extractUsername(token);
 
                    /*
                     * Lưu username vào attributes của WebSocket session.
                     * attributes là Map được truyền qua toàn bộ vòng đời của WS connection.
                     * Sau này ChannelInterceptor và ChatController có thể đọc từ đây.
                     */
                    attributes.put("username", username);
                    log.info("WebSocket handshake thành công cho user: {}", username);
                    return true;  // Cho phép kết nối
 
                } catch (Exception e) {
                    log.warn("Token không hợp lệ, từ chối WebSocket connection: {}", e.getMessage());
                    return false;  // Từ chối kết nối
                }
            }
        }
 
        // Không có token — từ chối kết nối
        log.warn("WebSocket kết nối bị từ chối: không có token");
        return false;
    }
 
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // Không cần làm gì sau handshake
    }
}