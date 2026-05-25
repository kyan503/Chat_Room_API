package com.chatroomrealtime.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "room_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
// Đảm bảo một user chỉ join một phòng một lần (không bị duplicate record)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMember {
    	  @Id
    	    @GeneratedValue(strategy = GenerationType.IDENTITY)
    	    private Long id;
    	 
    	    @ManyToOne(fetch = FetchType.LAZY)
    	    @JoinColumn(name = "room_id", nullable = false)
    	    private Room room;
    	 
    	    @ManyToOne(fetch = FetchType.LAZY)
    	    @JoinColumn(name = "user_id", nullable = false)
    	    private User user;
    	 
    	    @Column(name = "joined_at", updatable = false)
    	    private LocalDateTime joinedAt;
    	 
    	    @PrePersist
    	    protected void onCreate() {
    	        joinedAt = LocalDateTime.now();
    	    }

}
