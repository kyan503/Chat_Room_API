package com.chatroomrealtime.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chatroomrealtime.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	/*
     * JpaRepository<User, Long>:
     * - User: entity type
     * - Long: kiểu của primary key (id)
     * Spring Data tự generate SQL, không cần viết tay.
     * Chỉ cần khai báo method signature → Spring tự hiểu.
     */
 
    Optional<User> findByUsername(String username);
 
    Optional<User> findByEmail(String email);
 
    boolean existsByUsername(String username);
 
    boolean existsByEmail(String email);
}
