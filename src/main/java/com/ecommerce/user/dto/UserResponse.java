package com.ecommerce.user.dto;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin chi tiết người dùng.
 *
 * @param id        Mã định danh người dùng
 * @param username  Tên đăng nhập
 * @param email     Địa chỉ email
 * @param enabled   Trạng thái kích hoạt của tài khoản
 * @param role      Thông tin vai trò phân quyền
 * @param createdAt Thời điểm tạo tài khoản
 */
public record UserResponse(
    Long id,
    String username,
    String email,
    Boolean enabled,
    RoleResponse role,
    LocalDateTime createdAt
) {
}

