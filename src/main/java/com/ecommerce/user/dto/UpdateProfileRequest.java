package com.ecommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu cập nhật thông tin hồ sơ cá nhân.
 *
 * @param email    Địa chỉ email mới (nếu muốn thay đổi)
 * @param password Mật khẩu mới (tối thiểu 8 ký tự, nếu muốn đổi mật khẩu)
 */
public record UpdateProfileRequest(
    @Email(message = "Định dạng email không hợp lệ")
    String email,

    @Size(min = 8, max = 100, message = "Mật khẩu mới phải có độ dài từ 8 đến 100 ký tự")
    String password
) {
}

