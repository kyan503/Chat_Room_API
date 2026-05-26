package com.chatroomrealtime.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatroomrealtime.entity.Room;
import com.chatroomrealtime.entity.RoomMember;
import com.chatroomrealtime.entity.User;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long>{
	
	// Kiểm tra user đã là member của phòng này chưa
    boolean existsByRoomAndUser(Room room, User user);

    // Tìm record RoomMember cụ thể (dùng khi rời phòng)
    Optional<RoomMember> findByRoomAndUser(Room room, User user);
    
    // Đếm số thành viên trong phòng
    long countByRoom(Room room);
}
