package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductStatus;

import java.math.BigDecimal;

/**
 * DTO chứa các tiêu chí tìm kiếm và lọc dữ liệu sản phẩm đa điều kiện.
 *
 * @param keyword    Từ khóa tìm kiếm theo tên hoặc mô tả sản phẩm
 * @param categoryId Mã định danh danh mục cần lọc
 * @param status     Trạng thái kinh doanh sản phẩm
 * @param minPrice   Khoảng giá tối thiểu
 * @param maxPrice   Khoảng giá tối đa
 */
public record ProductSearchCriteria(
    String keyword,
    Long categoryId,
    ProductStatus status,
    BigDecimal minPrice,
    BigDecimal maxPrice
) {
}

