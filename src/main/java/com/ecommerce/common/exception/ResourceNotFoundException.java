package com.ecommerce.common.exception;

/**
 * Ngoại lệ ném ra khi không tìm thấy tài nguyên yêu cầu trong hệ thống (Resource Not Found Exception).
 * <p>
 * Luôn gắn liền với {@link ErrorCode#RESOURCE_NOT_FOUND} (HTTP 404 Not Found).
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi cụ thể.
     *
     * @param message thông báo lỗi bằng tiếng Việt
     */
    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /**
     * Khởi tạo ngoại lệ định dạng chuẩn theo tên tài nguyên, tên trường và giá trị tìm kiếm.
     * <p>
     * Ví dụ: {@code new ResourceNotFoundException("Sản phẩm", "id", 10)}
     * sẽ sinh thông điệp: "Không tìm thấy Sản phẩm với id: '10'".
     *
     * @param resourceName tên loại tài nguyên (ví dụ: "Sản phẩm", "Người dùng")
     * @param fieldName    tên trường dùng để tra cứu (ví dụ: "id", "email")
     * @param fieldValue   giá trị tìm kiếm không tồn tại
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
            String.format("Không tìm thấy %s với %s: '%s'", resourceName, fieldName, fieldValue));
    }
}

