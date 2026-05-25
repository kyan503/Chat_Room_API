package com.chatroomrealtime.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AuthDto {
	@Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Username không được để trống")
        @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
        private String username;
 
        @NotBlank(message = "Password không được để trống")
        @Size(min = 6, message = "Password phải ít nhất 6 ký tự")
        private String password;
 
        @NotBlank
        @Email(message = "Email không hợp lệ")
        private String email;
 
        private String displayName;
    }
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank
        private String username;
 
        @NotBlank
        private String password;
    }
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String username;
        private String displayName;
    }

}
