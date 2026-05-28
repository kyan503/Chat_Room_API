package com.chatroomrealtime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatroomrealtime.dto.AuthDto;
import com.chatroomrealtime.service.AuthService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    /*
     * @RestController = @Controller + @ResponseBody
     * Tự động serialize object trả về thành JSON
     *
     * @RequestMapping("/api/auth") → tất cả endpoint trong class này bắt đầu bằng /api/auth
     */
 
    private final AuthService authService;
    public AuthController(AuthService authService ) {
		this.authService = authService;
	}
 
    // POST /api/auth/register
    // Body: { "username": "john", "password": "123456", "email": "john@mail.com" }
    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(
            @Valid @RequestBody
            AuthDto.RegisterRequest request
            // @Valid: kích hoạt validation của các annotation như @NotBlank, @Size
            // @RequestBody: parse JSON body thành object Java
    ) {
        return ResponseEntity.ok(authService.register(request));
    }
 
    // POST /api/auth/login
    // Body: { "username": "john", "password": "123456" }
    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(
            @Valid @RequestBody AuthDto.LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
    
}