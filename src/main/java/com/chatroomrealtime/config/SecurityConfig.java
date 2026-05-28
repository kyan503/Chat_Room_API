package com.chatroomrealtime.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.chatroomrealtime.repository.UserRepository;
import com.chatroomrealtime.security.JwtAuthFilter;
import com.chatroomrealtime.security.JwtService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthFilter jwtAuthFilter;
	private final UserRepository userRepository;

	public SecurityConfig(@Lazy JwtAuthFilter jwtAuthFilter, UserRepository userRepository) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.userRepository = userRepository;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(request -> {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOrigins(List.of("http://localhost:5173")); // Khớp cổng chạy Front-end
			config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
			config.setAllowedHeaders(List.of("*"));
			config.setAllowCredentials(true); // Cho phép đính kèm credentials/headers bảo mật
			return config;
		}))
				// Tắt CSRF vì dùng JWT (stateless), không cần CSRF protection
				.csrf(AbstractHttpConfigurer::disable)

				.authorizeHttpRequests(auth -> auth
						// Các endpoint này không cần token
						.requestMatchers("/api/auth/**").permitAll()
						// WebSocket endpoint cũng phải permit (JWT validate ở interceptor riêng)
						.requestMatchers("/ws/**").permitAll()
						// Tất cả endpoint còn lại phải có JWT hợp lệ
						.anyRequest().authenticated())

				// STATELESS: mỗi request phải tự mang token, server không lưu session
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authenticationProvider(authenticationProvider())

				// Thêm JwtAuthFilter TRƯỚC UsernamePasswordAuthenticationFilter
				// Để JWT được validate trước khi Security framework kiểm tra
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// Dạy Spring Security cách load user từ DB
	@Bean
	public UserDetailsService userDetailsService() {
		return username -> userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
	}

	// Provider kết hợp UserDetailsService + PasswordEncoder để xác thực
	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	// BCrypt: thuật toán hash password an toàn, mỗi lần hash ra kết quả khác nhau
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// AuthenticationManager dùng trong AuthController để gọi authenticate()
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

}
