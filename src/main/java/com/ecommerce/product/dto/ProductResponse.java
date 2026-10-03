package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * DTO phản hồi thông tin chi tiết đầy đủ của sản phẩm.
 *
 * @param id            Mã định danh sản phẩm
 * @param name          Tên sản phẩm
 * @param description   Mô tả chi tiết sản phẩm
 * @param price         Giá bán hiện tại
 * @param stockQuantity Số lượng tồn kho hiện tại
 * @param status        Trạng thái kinh doanh
 * @param imageUrl      Đường dẫn ảnh đại diện
 * @param category      Thông tin danh mục sản phẩm
 * @param images        Danh sách hình ảnh chi tiết của sản phẩm
 * @param tags          Tập hợp các thẻ phân loại gắn cho sản phẩm
 * @param createdAt     Thời điểm tạo sản phẩm
 * @param updatedAt     Thời điểm cập nhật sản phẩm gần nhất
 */
public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    ProductStatus status,
    String imageUrl,
    CategoryResponse category,
    List<ProductImageResponse> images,
    Set<TagResponse> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

