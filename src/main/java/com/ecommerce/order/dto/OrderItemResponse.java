package com.ecommerce.order.dto;

import java.math.BigDecimal;

/**
 * DTO phản hồi thông tin mặt hàng đã lưu trong đơn hàng (Snapshot item).
 *
 * @param id          Mã định danh bản ghi mục hàng
 * @param productId   Mã sản phẩm tại thời điểm mua
 * @param productName Tên sản phẩm tại thời điểm mua
 * @param quantity    Số lượng đặt mua
 * @param unitPrice   Đơn giá sản phẩm được đóng băng snapshot tại thời điểm đặt hàng
 * @param subtotal    Thành tiền của mục hàng (unitPrice * quantity)
 */
public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {
}

