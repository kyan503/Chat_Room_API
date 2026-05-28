package com.chatroomrealtime.controller;
import com.chatroomrealtime.dto.RoomDto;
import com.chatroomrealtime.entity.User;
import com.chatroomrealtime.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {
 
    private final RoomService roomService;
 
    /*
     * @AuthenticationPrincipal User currentUser:
     * Spring Security tự inject User object từ SecurityContext vào parameter này.
     * Đây là user đã được xác thực bởi JwtAuthFilter — không cần parse token thủ công.
     */
 
    // POST /api/rooms — Tạo phòng mới
    @PostMapping
    public ResponseEntity<RoomDto.RoomResponse> createRoom(
            @Valid @RequestBody RoomDto.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201 Created thay vì 200 OK khi tạo resource mới
                .body(roomService.createRoom(request, currentUser));
    }
 
    // GET /api/rooms — Xem tất cả phòng
    @GetMapping
    public ResponseEntity<List<RoomDto.RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }
 
    // GET /api/rooms/my — Xem phòng mình đang tham gia
    @GetMapping("/my")
    public ResponseEntity<List<RoomDto.RoomResponse>> getMyRooms(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(roomService.getMyRooms(currentUser));
    }
 
    // GET /api/rooms/{id} — Xem chi tiết 1 phòng
    @GetMapping("/{id}")
    public ResponseEntity<RoomDto.RoomResponse> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }
 
    // PUT /api/rooms/{id} — Đổi tên/mô tả phòng (chỉ owner)
    @PutMapping("/{id}")
    public ResponseEntity<RoomDto.RoomResponse> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomDto.UpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(roomService.updateRoom(id, request, currentUser));
    }
 
    // DELETE /api/rooms/{id} — Xóa phòng (chỉ owner)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        roomService.deleteRoom(id, currentUser);
        return ResponseEntity.noContent().build();  // 204 No Content — xóa thành công, không có body
    }
 
    // POST /api/rooms/{id}/join — Tham gia phòng
    @PostMapping("/{id}/join")
    public ResponseEntity<String> joinRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(roomService.joinRoom(id, currentUser));
    }
 
    // DELETE /api/rooms/{id}/leave — Rời phòng
    @DeleteMapping("/{id}/leave")
    public ResponseEntity<String> leaveRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(roomService.leaveRoom(id, currentUser));
    }
 
    // GET /api/rooms/{id}/members — Xem danh sách thành viên
    @GetMapping("/{id}/members")
    public ResponseEntity<List<RoomDto.MemberResponse>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getMembers(id));
    }
}