package com.ecommerce.notification.dto;

/**
 * DTO phản hồi số lượng thông báo chưa đọc của người dùng.
 *
 * @param unreadCount Số lượng thông báo chưa đọc
 */
public record UnreadCountResponse(
    long unreadCount
) {
}

