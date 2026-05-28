package com.chatroomrealtime.controller;

import com.chatroomrealtime.dto.ChatMessageDto;
import com.chatroomrealtime.entity.Message;
import com.chatroomrealtime.entity.Room;
import com.chatroomrealtime.entity.User;
import com.chatroomrealtime.repository.MessageRepository;
import com.chatroomrealtime.repository.RoomMemberRepository;
import com.chatroomrealtime.repository.RoomRepository;
import com.chatroomrealtime.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.security.Principal;
import java.time.LocalDateTime;
 
@Controller
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")

/*
 * NGÀY 7: ChatController đầy đủ — có JWT, lưu message vào DB, broadcast.
 * Thay thế ChatController ngày 6.
 *
 * Toàn bộ flow khi user gửi tin nhắn:
 * 1. Client gửi STOMP SEND /app/chat/1 với body {"content": "Hello"}
 * 2. Spring tìm @MessageMapping("/chat/1") → vào method handleMessage()
 * 3. Principal principal đã có username (nhờ AuthChannelInterceptor ngày 7)
 * 4. Validate: phòng tồn tại? user là member không?
 * 5. Lưu message vào DB
 * 6. Broadcast tới /topic/room/1 → tất cả subscriber nhận được
 */
public class ChatController {
 
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
 
    @MessageMapping("/chat/{roomId}")
    public void handleMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageDto.IncomingMessage incomingMessage,
            Principal principal) {
 
        // ===== BƯỚC 1: Lấy thông tin user từ Principal =====
        /*
         * principal.getName() trả về username.
         * Principal được set bởi AuthChannelInterceptor khi nhận STOMP CONNECT.
         * Chính xác hơn là UsernamePasswordAuthenticationToken.getName() = username của UserDetails.
         */
        String username = principal.getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));
 
        log.info("[{}] gửi message vào phòng [{}]: {}", username, roomId, incomingMessage.getContent());
 
        // ===== BƯỚC 2: Validate phòng =====
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.warn("Phòng không tồn tại: {}", roomId);
                    return new RuntimeException("Phòng không tồn tại");
                });
 
        // ===== BƯỚC 3: Validate user có phải member không =====
        /*
         * Tại sao cần kiểm tra ở đây dù đã check ở REST API?
         * WebSocket connection tồn tại lâu dài — user có thể bị kick khỏi phòng
         * trong khi vẫn còn giữ WS connection. Phải re-validate mỗi message.
         */
        if (!roomMemberRepository.existsByRoomAndUser(room, sender)) {
            log.warn("User [{}] không phải member của phòng [{}]", username, roomId);
 
            // Gửi error chỉ cho người gửi (không broadcast)
            messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                "Bạn không phải thành viên của phòng này"
            );
            return;
        }
 
        // ===== BƯỚC 4: Lưu message vào DB =====
        /*
         * Lưu VÀO DB trước khi broadcast.
         * Nếu DB lỗi, message không được lưu → throw exception → không broadcast.
         * Đảm bảo chỉ broadcast message đã được persist thành công.
         */
        Message savedMessage = messageRepository.save(
            Message.builder()
                .content(incomingMessage.getContent())
                .sender(sender)
                .room(room)
                .build()
        );
 
        log.info("Message [id={}] đã lưu vào DB", savedMessage.getId());
 
        // ===== BƯỚC 5: Tạo OutgoingMessage và broadcast =====
        ChatMessageDto.OutgoingMessage outgoing = ChatMessageDto.OutgoingMessage.builder()
                .messageId(savedMessage.getId())
                .content(savedMessage.getContent())
                .roomId(roomId)
                .senderUsername(sender.getUsername())
                .senderDisplayName(sender.getDisplayName())
                .sentAt(savedMessage.getCreatedAt())
                .type("CHAT")
                .build();
 
        /*
         * convertAndSend("/topic/room/" + roomId, outgoing):
         * Broker tìm tất cả client đang SUBSCRIBE /topic/room/{roomId}
         * và forward outgoing JSON tới họ.
         *
         * Đây là lúc "real-time" xảy ra:
         * - User A ở Hà Nội gửi message
         * - User B ở TP.HCM, User C ở Đà Nẵng nhận ngay trong milliseconds
         * - Không ai cần refresh browser hay polling
         */
        messagingTemplate.convertAndSend("/topic/room/" + roomId, outgoing);
        log.info("Đã broadcast message tới {} subscriber của /topic/room/{}", roomId, roomId);
    }
 
    /*
     * Thông báo khi user join phòng — broadcast cho cả phòng biết.
     * Client gọi: SEND /app/room/{roomId}/join
     */
    @MessageMapping("/room/{roomId}/join")
    public void notifyJoin(@DestinationVariable Long roomId, Principal principal) {
        if (principal == null) return;
 
        String username = principal.getName();
        ChatMessageDto.OutgoingMessage joinMsg = ChatMessageDto.OutgoingMessage.builder()
                .content(username + " đã tham gia phòng")
                .roomId(roomId)
                .senderUsername("system")
                .senderDisplayName("Hệ thống")
                .sentAt(LocalDateTime.now())
                .type("JOIN")
                .build();
 
        messagingTemplate.convertAndSend("/topic/room/" + roomId, joinMsg);
    }
}