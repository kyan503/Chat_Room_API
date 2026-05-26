package com.chatroomrealtime.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RoomDto {
	// ===== REQUEST DTOs (Client → Server) =====
	 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Tên phòng không được để trống")
        @Size(min = 2, max = 100, message = "Tên phòng phải từ 2-100 ký tự")
        private String name;
 
        @Size(max = 300, message = "Mô tả tối đa 300 ký tự")
        private String description;
    }
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "Tên phòng không được để trống")
        @Size(min = 2, max = 100)
        private String name;
 
        @Size(max = 300)
        private String description;
    }
 
    // ===== RESPONSE DTOs (Server → Client) =====
 
    /*
     * Tại sao cần DTO riêng thay vì trả về Entity Room trực tiếp?
     * 1. Tránh lộ thông tin nhạy cảm (password của owner chẳng hạn)
     * 2. Tránh vòng lặp JSON vô tận (Room → members → user → rooms → ...)
     * 3. Kiểm soát chính xác data nào được expose ra ngoài
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomResponse {
        private Long id;
        private String name;
        private String description;
        private String ownerUsername;   // Chỉ lấy username, không lấy cả object User
        private long memberCount;
        private LocalDateTime createdAt;
    }
 
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberResponse {
        private Long userId;
        private String username;
        private String displayName;
        private LocalDateTime joinedAt;
    }

}
