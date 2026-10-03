package com.ecommerce.user.dto;

/**
 * DTO phản hồi thông tin vai trò phân quyền người dùng.
 *
 * @param id   Mã định danh vai trò
 * @param name Tên vai trò (ví dụ: "ROLE_CUSTOMER", "ROLE_ADMIN")
 */
public record RoleResponse(
    Long id,
    String name
) {
}

