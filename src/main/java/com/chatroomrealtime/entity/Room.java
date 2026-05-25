package com.chatroomrealtime.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rooms")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, length = 50)
	private String name;
	
	@Column(length = 100)
	private String description;
	
	 /*
     * ManyToOne: nhiều phòng có thể có cùng owner.
     * FetchType.LAZY = chỉ load owner khi thực sự gọi getOwner() → tránh query thừa.
     * JoinColumn = tên cột foreign key trong bảng rooms.
     */
	
	@ManyToOne(fetch = FetchType.LAZY)
	private User owner;
	
	  /*
     * OneToMany: một phòng có nhiều thành viên.
     * mappedBy = tên field "room" bên trong RoomMember (không tạo bảng join riêng).
     * CascadeType.ALL = xóa phòng thì xóa luôn tất cả RoomMember liên quan.
     */
	 @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
	    @Builder.Default
	    private List<RoomMember> members = new ArrayList<>();
	 
	    @Column(name = "created_at", updatable = false)
	    private LocalDateTime createdAt;
	 
	    @PrePersist
	    protected void onCreate() {
	        createdAt = LocalDateTime.now();
	    }

}
