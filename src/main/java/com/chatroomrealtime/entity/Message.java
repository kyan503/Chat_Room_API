package com.chatroomrealtime.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
   name = "messages",
   indexes = @Index(columnList = "room_id, created_at DESC")
   // Index giúp query lịch sử tin nhắn theo phòng + sắp xếp thời gian nhanh hơn nhiều
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	 
	    @Column(nullable = false, columnDefinition = "TEXT")
	    private String content;
	 
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "sender_id", nullable = false)
	    private User sender;
	 
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "room_id", nullable = false)
	    private Room room;
	 
	    @Column(name = "created_at", updatable = false)
	    private LocalDateTime createdAt;
	 
	    @Column(name = "is_deleted")
	    @Builder.Default
	    private boolean deleted = false;
	    // Soft delete: không xóa thật, chỉ đánh dấu deleted = true
	    // Giữ lại lịch sử, tránh mất data khi user nhấn "Xóa tin nhắn"
	 
	    @PrePersist
	    protected void onCreate() {
	        createdAt = LocalDateTime.now();
	    }

}
