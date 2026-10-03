package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO yêu cầu cập nhật thông tin sản phẩm (dành cho Admin).
 *
 * @param categoryId  Mã định danh danh mục mới
 * @param name        Tên sản phẩm
 * @param description Mô tả sản phẩm
 * @param price       Giá bán mới (bắt buộc lớn hơn 0)
 * @param status      Trạng thái kinh doanh sản phẩm
 * @param imageUrl    Đường dẫn ảnh đại diện
 * @param tagNames    Tập hợp tên các thẻ gắn cho sản phẩm
 */
public record UpdateProductRequest(
    @NotNull(message = "Danh mục sản phẩm không được để trống")
    Long categoryId,

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    String name,

    String description,

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá sản phẩm phải lớn hơn 0")
    BigDecimal price,

    @NotNull(message = "Trạng thái sản phẩm không được để trống")
    ProductStatus status,

    String imageUrl,

    Set<String> tagNames
) {
}

