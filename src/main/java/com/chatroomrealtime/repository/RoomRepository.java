package com.chatroomrealtime.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.chatroomrealtime.entity.Room;
import com.chatroomrealtime.entity.User;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>{

	 // Lấy tất cả phòng do user này tạo	
	List<Room> findByOwner(User owner);
	
	// Lấy tất cả phòng mà user là thành viên
    // JOIN qua bảng room_members để tìm phòng user đã join
	@Query("SELECT r FROM Room r JOIN r.members m where m.user = :user")
	List<Room> findRoomsByMember(User user);
	
	  // Kiểm tra tên phòng đã tồn tại chưa (tránh trùng)
    boolean existsByName(String name);
	
}
