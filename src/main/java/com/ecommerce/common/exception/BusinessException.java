package com.ecommerce.common.exception;

import lombok.Getter;

/**
 * Ngoại lệ nghiệp vụ cơ sở dùng chung trong toàn bộ hệ thống (Base Business Exception).
 * <p>
 * Được ném ra từ tầng Service khi phát hiện các vi phạm quy tắc nghiệp vụ hoặc điều kiện không hợp lệ.
 * Lớp này mang theo {@link ErrorCode} để {@link GlobalExceptionHandler} có thể tự động ánh xạ
 * sang mã HTTP status và mã lỗi API tương ứng.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * Khởi tạo ngoại lệ với mã lỗi nghiệp vụ và thông điệp mặc định của mã lỗi.
     *
     * @param errorCode mã lỗi nghiệp vụ
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * Khởi tạo ngoại lệ với mã lỗi nghiệp vụ và thông điệp tùy chỉnh.
     *
     * @param errorCode mã lỗi nghiệp vụ
     * @param message   thông điệp lỗi chi tiết bằng tiếng Việt
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Khởi tạo ngoại lệ với thông điệp tùy chỉnh, mã lỗi mặc định là {@link ErrorCode#BAD_REQUEST}.
     *
     * @param message thông điệp lỗi chi tiết bằng tiếng Việt
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = ErrorCode.BAD_REQUEST;
    }

    /**
     * Khởi tạo ngoại lệ với mã lỗi nghiệp vụ, thông điệp và nguyên nhân gốc.
     *
     * @param errorCode mã lỗi nghiệp vụ
     * @param message   thông điệp lỗi chi tiết bằng tiếng Việt
     * @param cause     nguyên nhân gây ra ngoại lệ
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

