package com.chatroomrealtime.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MessageDto {
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MessageResponse {
		private Long id;
		private String content;
		private String senderUsername;
		private String senderDisplayName;
		private Long roomId;
		private LocalDateTime createdAt;
		private boolean deleted;
	}

	/*
	 * PageResponse: wrapper cho phân trang. Thay vì client nhận 1 mảng thô, họ nhận
	 * thêm metadata: - totalElements: tổng số message - totalPages: tổng số trang -
	 * currentPage: trang hiện tại → Client biết còn bao nhiêu trang để load tiếp
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PageResponse<T> {
		private java.util.List<T> content;
		private int currentPage;
		private int totalPages;
		private long totalElements;
		private boolean hasNext;
		private boolean hasPrevious;
	}
}
