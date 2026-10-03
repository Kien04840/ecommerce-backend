package com.ecommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu cập nhật thông tin hồ sơ cá nhân.
 * <p>
 * Lưu ý: Việc thay đổi mật khẩu (credentials) thuộc trách nhiệm độc lập của Auth Module
 * nhằm bảo đảm an toàn dữ liệu và tuân thủ ranh giới phân tầng.
 *
 * @param email Địa chỉ email mới (nếu muốn thay đổi)
 */
public record UpdateProfileRequest(
    @Email(message = "Định dạng email không hợp lệ")
    String email
) {
}

