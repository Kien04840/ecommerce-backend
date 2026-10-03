package com.ecommerce.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Danh mục mã lỗi hệ thống chuẩn (System Error Codes).
 * <p>
 * Định nghĩa mã lỗi định danh, trạng thái HTTP tương ứng và thông điệp tiếng Việt mặc định
 * phục vụ chuẩn hóa toàn bộ phản hồi lỗi của hệ thống theo đặc tả tại docs/api.md.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /**
     * Dữ liệu yêu cầu gửi lên không hợp lệ hoặc vi phạm kiểm tra validation.
     */
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),

    /**
     * Tài nguyên yêu cầu không tồn tại trong hệ thống.
     */
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu"),

    /**
     * Xung đột dữ liệu duy nhất hoặc tài nguyên đã tồn tại trước đó.
     */
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Dữ liệu đã tồn tại trong hệ thống"),

    /**
     * Yêu cầu gửi lên sai cấu trúc hoặc không thể xử lý.
     */
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),

    /**
     * Yêu cầu chưa được xác thực hoặc thông tin xác thực không hợp lệ.
     */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc phiên làm việc đã hết hạn"),

    /**
     * Người dùng đã xác thực nhưng không có quyền hạn truy cập tài nguyên.
     */
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này"),

    /**
     * Phương thức HTTP không được hỗ trợ trên endpoint hiện tại.
     */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ"),

    /**
     * Lỗi không xác định hoặc sự cố kỹ thuật nội bộ của máy chủ.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống nội bộ, vui lòng thử lại sau");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}

