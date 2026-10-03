package com.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu làm mới Access Token bằng Refresh Token.
 *
 * @param refreshToken Mã Refresh Token đã được cấp phát
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token không được để trống")
    String refreshToken
) {
}

