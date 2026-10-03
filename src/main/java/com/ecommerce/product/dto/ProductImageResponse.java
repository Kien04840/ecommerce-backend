package com.ecommerce.product.dto;

/**
 * DTO phản hồi thông tin hình ảnh sản phẩm.
 *
 * @param id        Mã định danh bản ghi hình ảnh
 * @param url       Đường dẫn URL hình ảnh
 * @param isPrimary Đánh dấu ảnh chính đại diện cho sản phẩm
 */
public record ProductImageResponse(
    Long id,
    String url,
    Boolean isPrimary
) {
}

