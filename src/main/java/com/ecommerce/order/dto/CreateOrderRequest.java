package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO yêu cầu khởi tạo đơn hàng mới (Checkout).
 *
 * @param items Danh sách các mặt hàng trong giỏ hàng (bắt buộc phải có ít nhất 1 mặt hàng)
 */
public record CreateOrderRequest(
    @NotEmpty(message = "Đơn hàng phải có ít nhất một mặt hàng")
    @Valid
    List<OrderItemRequest> items
) {
}

