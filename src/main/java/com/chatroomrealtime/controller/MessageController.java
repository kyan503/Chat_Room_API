package com.chatroomrealtime.controller;

import com.chatroomrealtime.dto.MessageDto;
import com.chatroomrealtime.entity.User;
import com.chatroomrealtime.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class MessageController {
 
    private final MessageService messageService;
 
    /*
     * GET /api/rooms/{roomId}/messages?page=0&size=20
     *
     * Query params:
     * - page: số trang (mặc định 0 = trang đầu tiên)
     * - size: số tin nhắn mỗi trang (mặc định 20)
     *
     * Ví dụ gọi API:
     * GET /api/rooms/1/messages          → 20 tin nhắn mới nhất
     * GET /api/rooms/1/messages?page=1   → 20 tin nhắn tiếp theo (cũ hơn)
     * GET /api/rooms/1/messages?size=50  → 50 tin nhắn mới nhất
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<MessageDto.PageResponse<MessageDto.MessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
 
        return ResponseEntity.ok(
                messageService.getMessages(roomId, page, size, currentUser));
    }
 
    /*
     * DELETE /api/messages/{messageId}
     * Soft delete — chỉ đánh dấu deleted=true, không xóa khỏi DB.
     * Chỉ người gửi mới được xóa tin nhắn của mình.
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal User currentUser) {
 
        messageService.deleteMessage(messageId, currentUser);
        return ResponseEntity.noContent().build();  // 204 No Content
    }

}
