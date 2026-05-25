package com.chatroomrealtime.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatroomrealtime.dto.AuthDto;
import com.chatroomrealtime.entity.User;
import com.chatroomrealtime.repository.UserRepository;
import com.chatroomrealtime.security.JwtService;



@Service
public class AuthService {

	private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    
	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			AuthenticationManager authenticationManager) {
		super();
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
	}

    
 
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        // Kiểm tra username hoặc email đã tồn tại chưa
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }
 
        // Tạo user mới, hash password trước khi lưu
        // KHÔNG BAO GIỜ lưu plain text password vào DB
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .displayName(request.getDisplayName() != null
                        ? request.getDisplayName()
                        : request.getUsername())  // Dùng username làm displayName nếu không điền
                .build();
 
        userRepository.save(user);
 
        // Tạo JWT ngay sau khi đăng ký → user có thể dùng ngay, không cần login lại
        String token = jwtService.generateToken(user);
        return new AuthDto.AuthResponse(token, user.getUsername(), user.getDisplayName());
    }
 
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        /*
         * authenticationManager.authenticate() sẽ:
         * 1. Gọi userDetailsService.loadUserByUsername()
         * 2. So sánh password với BCrypt
         * 3. Throw exception nếu sai → Spring Security tự xử lý trả 401
         */
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
 
        // Nếu đến đây = xác thực thành công
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
 
        String token = jwtService.generateToken(user);
        return new AuthDto.AuthResponse(token, user.getUsername(), user.getDisplayName());
    }

}
