package com.ecommerce.shipping.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO nhận dữ liệu thông báo trạng thái giao vận từ Webhook của đối tác vận chuyển.
 *
 * @param trackingNumber Mã vận đơn của đối tác
 * @param carrier        Tên đơn vị vận chuyển gửi thông báo
 * @param status         Trạng thái vận chuyển mới (ví dụ: "DELIVERED", "IN_TRANSIT")
 */
public record CarrierWebhookPayload(
    @NotBlank(message = "Mã vận đơn không được để trống")
    String trackingNumber,

    @NotBlank(message = "Hãng vận chuyển không được để trống")
    String carrier,

    @NotBlank(message = "Trạng thái vận chuyển không được để trống")
    String status
) {
}

