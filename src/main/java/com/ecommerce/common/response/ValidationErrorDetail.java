package com.ecommerce.common.response;

/**
 * Chi tiết một trường dữ liệu vi phạm kiểm tra hợp lệ (Validation Error Detail).
 *
 * @param field         Tên trường dữ liệu bị vi phạm (ví dụ: "price", "name")
 * @param rejectedValue Giá trị không hợp lệ mà client đã gửi lên
 * @param message       Thông báo lỗi kiểm tra hợp lệ bằng tiếng Việt
 */
public record ValidationErrorDetail(
    String field,
    Object rejectedValue,
    String message
) {
}

