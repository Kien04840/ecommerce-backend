package com.ecommerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO đại diện cho một mặt hàng được đặt trong đơn hàng.
 *
 * @param productId Mã định danh sản phẩm cần mua
 * @param quantity  Số lượng mua (tối thiểu là 1)
 */
public record OrderItemRequest(
    @NotNull(message = "Mã sản phẩm không được để trống")
    Long productId,

    @NotNull(message = "Số lượng mua không được để trống")
    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    Integer quantity
) {
}

