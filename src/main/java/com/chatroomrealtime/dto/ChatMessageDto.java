package com.chatroomrealtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
 
public class ChatMessageDto {
 
    /*
     * IncomingMessage: DTO nhận từ client qua WebSocket.
     * Client gửi JSON này khi nhắn tin:
     * {
     *   "content": "Hello everyone!"
     * }
     * Không cần gửi roomId (lấy từ URL) hay senderId (lấy từ JWT token).
     * Chỉ cần content — đơn giản nhất có thể.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncomingMessage {
        private String content;
    }
 
    /*
     * OutgoingMessage: DTO server broadcast xuống cho tất cả subscriber.
     * Đầy đủ thông tin để client hiển thị luôn, không cần gọi thêm API.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutgoingMessage {
        private Long messageId;
        private String content;
        private Long roomId;
        private String senderUsername;
        private String senderDisplayName;
        private LocalDateTime sentAt;
        private String type;  // "CHAT" | "JOIN" | "LEAVE" — phân loại message
    }
}
