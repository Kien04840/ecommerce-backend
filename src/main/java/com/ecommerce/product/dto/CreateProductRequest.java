package com.ecommerce.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO yêu cầu tạo mới sản phẩm (dành cho Admin).
 *
 * @param categoryId    Mã định danh danh mục của sản phẩm
 * @param name          Tên sản phẩm
 * @param description   Mô tả chi tiết sản phẩm
 * @param price         Giá bán sản phẩm (bắt buộc lớn hơn 0)
 * @param stockQuantity Số lượng tồn kho ban đầu (không âm)
 * @param imageUrl      Đường dẫn ảnh đại diện
 * @param tagNames      Tập hợp tên các thẻ gắn cho sản phẩm
 */
public record CreateProductRequest(
    @NotNull(message = "Danh mục sản phẩm không được để trống")
    Long categoryId,

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    String name,

    String description,

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá sản phẩm phải lớn hơn 0")
    BigDecimal price,

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    Integer stockQuantity,

    String imageUrl,

    Set<String> tagNames
) {
}

