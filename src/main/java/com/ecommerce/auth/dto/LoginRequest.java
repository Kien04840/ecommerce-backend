package com.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu đăng nhập tài khoản.
 *
 * @param username Tên đăng nhập hoặc địa chỉ email của tài khoản
 * @param password Mật khẩu tài khoản
 */
public record LoginRequest(
    @NotBlank(message = "Tên đăng nhập không được để trống")
    String username,

    @NotBlank(message = "Mật khẩu không được để trống")
    String password
) {
}

