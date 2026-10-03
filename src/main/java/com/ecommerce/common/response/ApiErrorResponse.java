package com.ecommerce.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Đối tượng bao bọc phản hồi lỗi API chuẩn (Standard API Error Response).
 * <p>
 * Áp dụng cho toàn bộ các ngoại lệ và lỗi HTTP phát sinh trong hệ thống.
 * Cấu trúc JSON tuân thủ tuyệt đối quy định tại docs/api.md.
 *
 * @param success Trạng thái thành công của yêu cầu (luôn là {@code false} đối với phản hồi lỗi)
 * @param message Thông báo mô tả lỗi bằng tiếng Việt thân thiện với người dùng
 * @param code    Mã định danh lỗi kỹ thuật phục vụ xử lý phía frontend (ví dụ: "VALIDATION_FAILED", "RESOURCE_NOT_FOUND")
 * @param errors  Danh sách chi tiết các trường bị lỗi validation (chỉ hiển thị khi có lỗi kiểm tra dữ liệu đầu vào)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    boolean success,
    String message,
    String code,
    List<ValidationErrorDetail> errors
) {

    /**
     * Tạo phản hồi lỗi không có danh sách chi tiết trường vi phạm.
     *
     * @param code    mã định danh lỗi
     * @param message thông báo lỗi tiếng Việt
     * @return đối tượng {@link ApiErrorResponse}
     */
    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(false, message, code, null);
    }

    /**
     * Tạo phản hồi lỗi kèm danh sách chi tiết các trường vi phạm validation.
     *
     * @param code    mã định danh lỗi
     * @param message thông báo lỗi tiếng Việt
     * @param errors  danh sách lỗi theo từng trường dữ liệu
     * @return đối tượng {@link ApiErrorResponse}
     */
    public static ApiErrorResponse of(String code, String message, List<ValidationErrorDetail> errors) {
        return new ApiErrorResponse(false, message, code, errors);
    }
}

