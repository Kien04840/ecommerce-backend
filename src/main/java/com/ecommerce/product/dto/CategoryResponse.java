package com.ecommerce.product.dto;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin chi tiết danh mục sản phẩm.
 *
 * @param id          Mã định danh danh mục
 * @param name        Tên danh mục
 * @param description Mô tả danh mục
 * @param createdAt   Thời điểm tạo danh mục
 */
public record CategoryResponse(
    Long id,
    String name,
    String description,
    LocalDateTime createdAt
) {
}

