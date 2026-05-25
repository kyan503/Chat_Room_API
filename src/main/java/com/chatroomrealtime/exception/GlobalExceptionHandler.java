package com.chatroomrealtime.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
//@RestControllerAdvice: bắt exception từ tất cả @RestController, xử lý tập trung
public class GlobalExceptionHandler {

 // Xử lý lỗi validation (@Valid thất bại)
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<Map<String, Object>> handleValidationErrors(
         MethodArgumentNotValidException ex) {

     Map<String, String> fieldErrors = new HashMap<>();
     for (FieldError error : ex.getBindingResult().getFieldErrors()) {
         fieldErrors.put(error.getField(), error.getDefaultMessage());
     }

     Map<String, Object> body = new HashMap<>();
     body.put("timestamp", LocalDateTime.now().toString());
     body.put("status", 400);
     body.put("errors", fieldErrors);

     return ResponseEntity.badRequest().body(body);
 }

 // Xử lý sai username/password
 @ExceptionHandler(BadCredentialsException.class)
 public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
     return buildError(HttpStatus.UNAUTHORIZED, "Sai username hoặc password");
 }

 // Xử lý RuntimeException chung (username đã tồn tại, room không tìm thấy...)
 @ExceptionHandler(RuntimeException.class)
 public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
     return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
 }

 private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
     Map<String, Object> body = new HashMap<>();
     body.put("timestamp", LocalDateTime.now().toString());
     body.put("status", status.value());
     body.put("message", message);
     return ResponseEntity.status(status).body(body);
 }
}