package com.ecommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu đăng ký tài khoản khách hàng mới.
 *
 * @param username Tên đăng nhập người dùng (3 - 50 ký tự)
 * @param email    Địa chỉ email hợp lệ
 * @param password Mật khẩu tài khoản (tối thiểu 8 ký tự)
 */
public record RegisterRequest(
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải có độ dài từ 3 đến 50 ký tự")
    String username,

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    String email,

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 100, message = "Mật khẩu phải có độ dài từ 8 đến 100 ký tự")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$",
        message = "Mật khẩu phải chứa ít nhất một chữ hoa, một chữ thường, một chữ số và một ký tự đặc biệt"
    )
    String password
) {
}

