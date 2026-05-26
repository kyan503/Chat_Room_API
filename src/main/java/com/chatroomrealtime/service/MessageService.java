package com.chatroomrealtime.service;

import com.chatroomrealtime.dto.MessageDto;
import com.chatroomrealtime.entity.Message;
import com.chatroomrealtime.entity.Room;
import com.chatroomrealtime.entity.User;
import com.chatroomrealtime.repository.MessageRepository;
import com.chatroomrealtime.repository.RoomMemberRepository;
import com.chatroomrealtime.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
@Service
@RequiredArgsConstructor
public class MessageService {
 
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
 
    // ===== XEM LỊCH SỬ MESSAGE (PHÂN TRANG) =====
    public MessageDto.PageResponse<MessageDto.MessageResponse> getMessages(
            Long roomId, int page, int size, User currentUser) {
 
        Room room = findRoomOrThrow(roomId);
 
        // Chỉ thành viên mới xem được lịch sử chat
        checkMembership(room, currentUser);
 
        /*
         * PageRequest.of(page, size):
         * - page: số trang, bắt đầu từ 0 (page=0 là trang đầu tiên)
         * - size: số item mỗi trang
         * Ví dụ: page=0, size=20 → lấy 20 tin nhắn mới nhất
         *        page=1, size=20 → lấy 20 tin nhắn tiếp theo (cũ hơn)
         */
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository
                .findByRoomAndDeletedFalseOrderByCreatedAtDesc(room, pageable);
 
        // Map từng Message entity sang MessageResponse DTO
        return MessageDto.PageResponse.<MessageDto.MessageResponse>builder()
                .content(messagePage.getContent().stream()
                        .map(this::toMessageResponse)
                        .toList())
                .currentPage(messagePage.getNumber())
                .totalPages(messagePage.getTotalPages())
                .totalElements(messagePage.getTotalElements())
                .hasNext(messagePage.hasNext())
                .hasPrevious(messagePage.hasPrevious())
                .build();
    }
 
    // ===== XÓA TIN NHẮN (SOFT DELETE) =====
    @Transactional
    public void deleteMessage(Long messageId, User currentUser) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin nhắn"));
 
        // Chỉ người gửi mới được xóa tin nhắn của mình
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn chỉ có thể xóa tin nhắn của chính mình");
        }
 
        /*
         * Soft delete: không DELETE khỏi DB mà chỉ set deleted = true.
         * Lý do:
         * 1. Giữ lại lịch sử cho audit log
         * 2. Trong WebSocket, các client khác cần nhận thông báo "tin nhắn đã bị xóa"
         *    thay vì tin nhắn biến mất đột ngột
         */
        message.setDeleted(true);
        messageRepository.save(message);
    }
 
    // ===== HELPER =====
 
    private Room findRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với id: " + roomId));
    }
 
    private void checkMembership(Room room, User currentUser) {
        if (!roomMemberRepository.existsByRoomAndUser(room, currentUser)) {
            throw new RuntimeException("Bạn không phải thành viên của phòng này");
        }
    }
 
    private MessageDto.MessageResponse toMessageResponse(Message message) {
        return MessageDto.MessageResponse.builder()
                .id(message.getId())
                .content(message.isDeleted() ? "[Tin nhắn đã bị xóa]" : message.getContent())
                .senderUsername(message.getSender().getUsername())
                .senderDisplayName(message.getSender().getDisplayName())
                .roomId(message.getRoom().getId())
                .createdAt(message.getCreatedAt())
                .deleted(message.isDeleted())
                .build();
    }
}
