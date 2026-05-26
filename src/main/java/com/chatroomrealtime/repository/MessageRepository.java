package com.chatroomrealtime.repository;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chatroomrealtime.entity.Message;
import com.chatroomrealtime.entity.Room;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
	 /*
     * Lấy lịch sử message của phòng, chỉ lấy những message chưa bị xóa (deleted = false).
     * Pageable cho phép phân trang: Page<Message> chứa data + metadata (tổng số trang, tổng số record...).
     * Tại sao cần phân trang? Nếu phòng có 100.000 tin nhắn, load hết 1 lần sẽ làm app chậm.
     */
	
    Page<Message> findByRoomAndDeletedFalseOrderByCreatedAtDesc(Room room, Pageable pageable);
 
    // Đếm số message chưa đọc (dùng cho tính năng notification sau này)
    long countByRoomAndDeletedFalse(Room room);
}
