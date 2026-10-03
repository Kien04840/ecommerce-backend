package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO phản hồi thông tin chi tiết đầy đủ của đơn hàng.
 *
 * @param id            Mã định danh đơn hàng
 * @param userId        Mã định danh người dùng đặt hàng
 * @param totalAmount   Tổng giá trị thanh toán của đơn hàng
 * @param status        Trạng thái xử lý của đơn hàng
 * @param paymentStatus Trạng thái thanh toán của đơn hàng
 * @param orderDate     Thời điểm tạo đơn hàng
 * @param updatedAt     Thời điểm cập nhật đơn hàng gần nhất
 * @param orderItems    Danh sách các mặt hàng snapshot thuộc đơn hàng
 */
public record OrderResponse(
    Long id,
    Long userId,
    BigDecimal totalAmount,
    OrderStatus status,
    PaymentStatus paymentStatus,
    LocalDateTime orderDate,
    LocalDateTime updatedAt,
    List<OrderItemResponse> orderItems
) {
}

