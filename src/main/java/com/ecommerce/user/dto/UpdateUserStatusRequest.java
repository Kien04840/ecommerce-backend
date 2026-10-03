package com.ecommerce.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO yêu cầu cập nhật trạng thái kích hoạt hoặc khóa tài khoản người dùng (dành cho Admin).
 *
 * @param enabled Trạng thái kích hoạt mới (true: kích hoạt, false: khóa)
 */
public record UpdateUserStatusRequest(
    @NotNull(message = "Trạng thái kích hoạt không được để trống")
    Boolean enabled
) {
}

