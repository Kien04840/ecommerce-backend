package com.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu thay đổi mật khẩu người dùng.
 * <p>
 * Bắt buộc cung cấp mật khẩu hiện tại để xác thực và mật khẩu mới tuân thủ
 * quy định an toàn mật khẩu hệ thống (tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt).
 *
 * @param currentPassword Mật khẩu hiện tại của tài khoản
 * @param newPassword     Mật khẩu mới cần cập nhật
 */
public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    String currentPassword,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 100, message = "Mật khẩu mới phải có độ dài từ 8 đến 100 ký tự")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$",
        message = "Mật khẩu mới phải chứa ít nhất một chữ hoa, một chữ thường, một chữ số và một ký tự đặc biệt"
    )
    String newPassword
) {
}

