package com.chatroomrealtime.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	 /*
     * JWT gồm 3 phần: header.payload.signature
     * - Header: loại token + thuật toán (HS256)
     * - Payload: dữ liệu (username, thời gian hết hạn...)
     * - Signature: mã hóa bằng secret key → đảm bảo không ai giả mạo token
     */
     
	 @Value("${app.jwt.secret}")
     private String secretKey;
     
	 @Value("${app.jwt.expiration}")
     private long jwtExpiration;
	 
	// Lấy username từ token (username được lưu trong claim "subject")
	    public String extractUsername(String token) {
	        return extractClaim(token, Claims::getSubject);
	    }
	    
	    // Generic method: lấy bất kỳ claim nào từ token
	    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
	        final Claims claims = extractAllClaims(token);
	        return claimsResolver.apply(claims);
	    }
	    // Tạo token từ UserDetails (chỉ cần username là đủ cho project này)
	    public String generateToken(UserDetails userDetails) {
	        return generateToken(new HashMap<>(), userDetails);
	    }
	 
	    // Tạo token với extra claims tùy chỉnh (nếu sau này muốn thêm role, userId...)
	    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
	        return Jwts.builder()
	                .claims(extraClaims)
	                .subject(userDetails.getUsername())
	                .issuedAt(new Date())
	                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
	                .signWith(getSigningKey())
	                .compact();
	    }
	 
	    // Kiểm tra token có hợp lệ không: đúng user + chưa hết hạn
	    public boolean isTokenValid(String token, UserDetails userDetails) {
	        final String username = extractUsername(token);
	        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	    }
	 
	    private boolean isTokenExpired(String token) {
	        return extractExpiration(token).before(new Date());
	    }
	 
	    private Date extractExpiration(String token) {
	        return extractClaim(token, Claims::getExpiration);
	    }
	 
	    private Claims extractAllClaims(String token) {
	        return Jwts.parser()
	                .verifyWith(getSigningKey())
	                .build()
	                .parseSignedClaims(token)
	                .getPayload();
	    }
	 
	    private SecretKey getSigningKey() {
	        // Chuyển secret string thành key object để ký/verify token
	    	byte[] keyBytes = this.secretKey.getBytes(StandardCharsets.UTF_8);
	        return Keys.hmacShaKeyFor(keyBytes);
	    }
}
