package com.ecommerce.shipping.dto;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin vận chuyển và mã theo dõi đơn hàng.
 *
 * @param id             Mã định danh bản ghi vận đơn
 * @param orderId        Mã định danh đơn hàng liên kết
 * @param trackingNumber Mã theo dõi vận đơn (Tracking Number)
 * @param carrier        Đơn vị / Hãng vận chuyển
 * @param status         Trạng thái giao vận hiện tại
 * @param lastSyncTime   Thời điểm đồng bộ trạng thái gần nhất
 * @param createdAt      Thời điểm tạo bản ghi vận chuyển
 */
public record ShipmentResponse(
    Long id,
    Long orderId,
    String trackingNumber,
    String carrier,
    String status,
    LocalDateTime lastSyncTime,
    LocalDateTime createdAt
) {
}

