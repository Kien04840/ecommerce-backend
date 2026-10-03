package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO yêu cầu cập nhật trạng thái đơn hàng (dành cho Admin / Staff).
 *
 * @param status Trạng thái mới của đơn hàng
 */
public record UpdateOrderStatusRequest(
    @NotNull(message = "Trạng thái đơn hàng không được để trống")
    OrderStatus status
) {
}

