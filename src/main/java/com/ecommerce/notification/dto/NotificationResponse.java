package com.ecommerce.notification.dto;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin một thông báo người dùng.
 *
 * @param id        Mã định danh thông báo
 * @param title     Tiêu đề thông báo
 * @param message   Nội dung thông báo chi tiết
 * @param isRead    Trạng thái đã đọc (true: đã đọc, false: chưa đọc)
 * @param createdAt Thời điểm phát sinh thông báo
 */
public record NotificationResponse(
    Long id,
    String title,
    String message,
    Boolean isRead,
    LocalDateTime createdAt
) {
}

