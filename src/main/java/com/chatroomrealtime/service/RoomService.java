package com.chatroomrealtime.service;

import com.chatroomrealtime.dto.RoomDto;
import com.chatroomrealtime.entity.Room;
import com.chatroomrealtime.entity.RoomMember;
import com.chatroomrealtime.entity.User;
import com.chatroomrealtime.repository.RoomMemberRepository;
import com.chatroomrealtime.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.List;
import java.util.stream.Collectors;
 
@Service
@RequiredArgsConstructor
public class RoomService {
 
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
 
    // ===== TẠO PHÒNG =====
    @Transactional
    // @Transactional: nếu có lỗi xảy ra giữa chừng, toàn bộ thao tác DB sẽ bị rollback.
    // Ví dụ: save room thành công nhưng add member thất bại → room cũng bị xóa, không bị data dở dang.
    public RoomDto.RoomResponse createRoom(RoomDto.CreateRequest request, User currentUser) {
        // Kiểm tra tên phòng trùng
        if (roomRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên phòng đã tồn tại: " + request.getName());
        }
 
        // Tạo Room entity
        Room room = Room.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(currentUser)
                .build();
 
        roomRepository.save(room);
 
        // Tự động add owner vào phòng với tư cách thành viên luôn
        RoomMember ownerAsMember = RoomMember.builder()
                .room(room)
                .user(currentUser)
                .build();
        roomMemberRepository.save(ownerAsMember);
 
        return toRoomResponse(room, 1L);
    }
 
    // ===== XEM DANH SÁCH PHÒNG =====
    public List<RoomDto.RoomResponse> getAllRooms() {
        // Lấy tất cả phòng, map sang DTO
        return roomRepository.findAll().stream()
                .map(room -> toRoomResponse(room,
                        roomMemberRepository.countByRoom(room)))
                .collect(Collectors.toList());
    }
 
    // Lấy phòng mà user đang là thành viên
    public List<RoomDto.RoomResponse> getMyRooms(User currentUser) {
        return roomRepository.findRoomsByMember(currentUser).stream()
                .map(room -> toRoomResponse(room,
                        roomMemberRepository.countByRoom(room)))
                .collect(Collectors.toList());
    }
 
    // Xem chi tiết 1 phòng
    public RoomDto.RoomResponse getRoomById(Long roomId) {
        Room room = findRoomOrThrow(roomId);
        long memberCount = roomMemberRepository.countByRoom(room);
        return toRoomResponse(room, memberCount);
    }
 
    // ===== ĐỔI TÊN PHÒNG =====
    @Transactional
    public RoomDto.RoomResponse updateRoom(Long roomId, RoomDto.UpdateRequest request, User currentUser) {
        Room room = findRoomOrThrow(roomId);
 
        // Chỉ owner mới được sửa
        checkOwnership(room, currentUser);
 
        // Kiểm tra tên mới có bị trùng với phòng khác không
        if (!room.getName().equals(request.getName())
                && roomRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên phòng đã tồn tại");
        }
 
        room.setName(request.getName());
        room.setDescription(request.getDescription());
        roomRepository.save(room);
 
        return toRoomResponse(room, roomMemberRepository.countByRoom(room));
    }
 
    // ===== XÓA PHÒNG =====
    @Transactional
    public void deleteRoom(Long roomId, User currentUser) {
        Room room = findRoomOrThrow(roomId);
        checkOwnership(room, currentUser);  // Chỉ owner mới xóa được
 
        /*
         * CascadeType.ALL trong Room entity sẽ tự xóa tất cả RoomMember liên quan.
         * Message sẽ cần xóa thủ công hoặc dùng cascade — ở đây dùng deleteById để JPA xử lý.
         */
        roomRepository.delete(room);
    }
 
    // ===== THÀNH VIÊN =====
 
    // Tham gia phòng
    @Transactional
    public String joinRoom(Long roomId, User currentUser) {
        Room room = findRoomOrThrow(roomId);
 
        // Kiểm tra đã join rồi chưa
        if (roomMemberRepository.existsByRoomAndUser(room, currentUser)) {
            throw new RuntimeException("Bạn đã là thành viên của phòng này rồi");
        }
 
        RoomMember member = RoomMember.builder()
                .room(room)
                .user(currentUser)
                .build();
        roomMemberRepository.save(member);
 
        return "Tham gia phòng '" + room.getName() + "' thành công";
    }
 
    // Rời phòng
    @Transactional
    public String leaveRoom(Long roomId, User currentUser) {
        Room room = findRoomOrThrow(roomId);
 
        // Owner không thể rời phòng — phải xóa phòng đi
        if (room.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Owner không thể rời phòng. Hãy xóa phòng nếu muốn.");
        }
 
        RoomMember member = roomMemberRepository.findByRoomAndUser(room, currentUser)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của phòng này"));
 
        roomMemberRepository.delete(member);
        return "Rời phòng '" + room.getName() + "' thành công";
    }
 
    // Xem danh sách thành viên
    public List<RoomDto.MemberResponse> getMembers(Long roomId) {
        Room room = findRoomOrThrow(roomId);
 
        return room.getMembers().stream()
                .map(member -> RoomDto.MemberResponse.builder()
                        .userId(member.getUser().getId())
                        .username(member.getUser().getUsername())
                        .displayName(member.getUser().getDisplayName())
                        .joinedAt(member.getJoinedAt())
                        .build())
                .collect(Collectors.toList());
    }
 
    // ===== HELPER METHODS =====
 
    // Tái sử dụng logic tìm Room — tránh lặp code ở mỗi method
    private Room findRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với id: " + roomId));
    }
 
    // Kiểm tra quyền owner — ném exception nếu không phải owner
    private void checkOwnership(Room room, User currentUser) {
        if (!room.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này");
        }
    }
 
    // Chuyển Room entity → RoomResponse DTO
    private RoomDto.RoomResponse toRoomResponse(Room room, long memberCount) {
        return RoomDto.RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .ownerUsername(room.getOwner().getUsername())
                .memberCount(memberCount)
                .createdAt(room.getCreatedAt())
                .build();
    }

}
