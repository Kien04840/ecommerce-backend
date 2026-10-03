package com.ecommerce.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Đối tượng bao bọc phản hồi API chuẩn (Standard API Response Wrapper).
 * <p>
 * Áp dụng cho toàn bộ các phản hồi HTTP thành công trong hệ thống,
 * bảo đảm cấu trúc JSON đồng nhất giữa các module theo đặc tả tại docs/api.md.
 *
 * @param <T> Kiểu dữ liệu của payload trả về
 * @param success Trạng thái thành công của yêu cầu (luôn là {@code true} cho phản hồi thành công)
 * @param message Thông báo kết quả thao tác bằng tiếng Việt
 * @param data Dữ liệu kết quả nghiệp vụ (có thể null đối với thao tác không trả dữ liệu)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data
) {

    /**
     * Tạo phản hồi thành công kèm dữ liệu với thông báo mặc định.
     *
     * @param data dữ liệu kết quả trả về
     * @param <T>  kiểu dữ liệu payload
     * @return đối tượng {@link ApiResponse} chứa dữ liệu
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Thao tác thành công", data);
    }

    /**
     * Tạo phản hồi thành công kèm thông báo tùy biến và dữ liệu.
     *
     * @param message thông báo phản hồi tiếng Việt
     * @param data    dữ liệu kết quả trả về
     * @param <T>     kiểu dữ liệu payload
     * @return đối tượng {@link ApiResponse} chứa thông báo và dữ liệu
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Tạo phản hồi thành công chỉ kèm thông báo (không có body data).
     *
     * @param message thông báo phản hồi tiếng Việt
     * @param <T>     kiểu dữ liệu payload
     * @return đối tượng {@link ApiResponse} chỉ chứa thông báo
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }
}

