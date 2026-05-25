package com.chatroomrealtime.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
    
	 @Column(unique = true, nullable = false, length = 50)
	    private String username;
	 
	    @Column(nullable = false)
	    private String password;   // Sẽ được hash bằng BCrypt trước khi lưu
	 
	    @Column(unique = true, nullable = false)
	    private String email;
	 
	    @Column(name = "display_name", length = 100)
	    private String displayName;
	 
	    @Column(name = "created_at", nullable = false, updatable = false)
	    private LocalDateTime createdAt;
	 
	    @PrePersist
	    protected void onCreate() {
	        createdAt = LocalDateTime.now();
	    }
	    
	    @Override
	    public Collection<? extends GrantedAuthority> getAuthorities() {
	        return List.of();   // Chưa có role system, trả về rỗng
	    }
	    
	    @Override
		public String getPassword() {
			return this.password;
		}

		// Chỉ định rõ ràng hàm getUsername() để đáp ứng interface UserDetails
		@Override
		public String getUsername() {
			return this.username;
		}
	    @Override
	    public boolean isAccountNonExpired()  { return true; }
	 
	    @Override
	    public boolean isAccountNonLocked()   { return true; }
	 
	    @Override
	    public boolean isCredentialsNonExpired() { return true; }
	 
	    @Override
	    public boolean isEnabled()            { return true; }
	
}
