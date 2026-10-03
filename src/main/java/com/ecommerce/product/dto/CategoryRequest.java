package com.ecommerce.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu tạo mới hoặc cập nhật danh mục sản phẩm.
 *
 * @param name        Tên danh mục sản phẩm
 * @param description Mô tả chi tiết về danh mục
 */
public record CategoryRequest(
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    String name,

    String description
) {
}

