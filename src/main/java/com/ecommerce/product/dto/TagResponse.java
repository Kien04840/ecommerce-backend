package com.ecommerce.product.dto;

/**
 * DTO phản hồi thông tin thẻ phân loại sản phẩm (Tag).
 *
 * @param id   Mã định danh thẻ
 * @param name Tên thẻ
 */
public record TagResponse(
    Long id,
    String name
) {
}

