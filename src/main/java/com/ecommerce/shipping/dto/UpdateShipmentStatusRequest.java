package com.ecommerce.shipping.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu cập nhật trạng thái vận đơn (dành cho Admin / Staff).
 *
 * @param status Trạng thái vận đơn mới cần cập nhật
 */
public record UpdateShipmentStatusRequest(
    @NotBlank(message = "Trạng thái vận đơn không được để trống")
    String status
) {
}

