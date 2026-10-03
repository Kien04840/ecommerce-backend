package com.ecommerce.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO yêu cầu điều chỉnh số lượng tồn kho sản phẩm (dành cho Admin / Staff).
 *
 * @param stockQuantity Số lượng tồn kho mới cần cập nhật (không âm)
 */
public record UpdateStockRequest(
    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    Integer stockQuantity
) {
}

