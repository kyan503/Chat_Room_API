package com.chatroomrealtime.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
public class JwtAuthFilter extends OncePerRequestFilter{
	 /*
     * OncePerRequestFilter đảm bảo filter này chỉ chạy 1 lần mỗi request.
     *
     * Luồng xử lý mỗi request:
     * 1. Đọc header Authorization
     * 2. Lấy token ra khỏi "Bearer <token>"
     * 3. Parse username từ token
     * 4. Load UserDetails từ DB
     * 5. Validate token
     * 6. Nếu hợp lệ → đặt Authentication vào SecurityContext
     * → Các request tiếp theo trong cùng thread sẽ biết user này đã xác thực
     */
	
	    private final JwtService jwtService;
	    private final UserDetailsService userDetailsService;

	    public JwtAuthFilter(JwtService jwtService,UserDetailsService userDetailsService) {
			this.jwtService = jwtService;
			this.userDetailsService = userDetailsService;
		}
	 
	    @Override
	    protected void doFilterInternal(
	            @NonNull HttpServletRequest request,
	            @NonNull HttpServletResponse response,
	            @NonNull FilterChain filterChain
	    ) throws ServletException, IOException {
	 
	        final String authHeader = request.getHeader("Authorization");
	 
	        // Nếu không có header hoặc không phải Bearer token → bỏ qua, đi tiếp
	        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
	            filterChain.doFilter(request, response);
	            return;
	        }
	 
	        // "Bearer eyJhbGc..." → lấy phần sau "Bearer "
	        final String jwt = authHeader.substring(7);
	        final String username = jwtService.extractUsername(jwt);
	 
	        // Chỉ xác thực nếu chưa có auth trong context (tránh xử lý 2 lần)
	        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
	            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
	 
	            if (jwtService.isTokenValid(jwt, userDetails)) {
	                // Tạo Authentication object và đặt vào SecurityContext
	                UsernamePasswordAuthenticationToken authToken =
	                        new UsernamePasswordAuthenticationToken(
	                                userDetails,
	                                null,                          // credentials = null vì đã xác thực bằng JWT
	                                userDetails.getAuthorities()
	                        );
	                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
	                SecurityContextHolder.getContext().setAuthentication(authToken);
	            }
	        }
	 
	        filterChain.doFilter(request, response);
	    }
}
