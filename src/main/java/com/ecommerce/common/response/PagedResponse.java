package com.ecommerce.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Đối tượng bao bọc dữ liệu phân trang chuẩn (Standard Pagination Response).
 * <p>
 * Chuẩn hóa định dạng danh sách và thông tin phân trang theo đặc tả tại docs/api.md,
 * giúp tầng Controller không phải tự trích xuất metadata pagination thủ công.
 *
 * @param <T>        Kiểu dữ liệu của phần tử trong danh sách
 * @param items      Danh sách các phần tử của trang hiện tại
 * @param pagination Metadata thông tin phân trang
 */
public record PagedResponse<T>(
    List<T> items,
    PaginationMetadata pagination
) {

    /**
     * Thông tin chi tiết phân trang của tập kết quả.
     *
     * @param page          Chỉ số trang hiện tại (bắt đầu từ 0)
     * @param size          Số lượng phần tử tối đa trên mỗi trang
     * @param totalElements Tổng số phần tử thỏa mãn điều kiện lọc
     * @param totalPages    Tổng số trang tương ứng
     * @param isFirst       {@code true} nếu là trang đầu tiên
     * @param isLast        {@code true} nếu là trang cuối cùng
     * @param hasNext       {@code true} nếu còn trang kế tiếp
     * @param hasPrevious   {@code true} nếu có trang trước đó
     */
    public record PaginationMetadata(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast,
        boolean hasNext,
        boolean hasPrevious
    ) {
        /**
         * Chuyển đổi metadata từ đối tượng {@link Page} của Spring Data.
         *
         * @param page đối tượng phân trang nguồn
         * @return đối tượng {@link PaginationMetadata}
         */
        public static PaginationMetadata from(Page<?> page) {
            return new PaginationMetadata(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
            );
        }
    }

    /**
     * Tạo {@link PagedResponse} trực tiếp từ đối tượng {@link Page} có cùng kiểu dữ liệu.
     *
     * @param page đối tượng phân trang Spring Data
     * @param <T>  kiểu phần tử
     * @return đối tượng {@link PagedResponse} hoàn chỉnh
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
            page.getContent(),
            PaginationMetadata.from(page)
        );
    }

    /**
     * Tạo {@link PagedResponse} với danh sách phần tử đã chuyển đổi (DTO) và metadata từ {@link Page}.
     * <p>
     * Thường dùng khi Service truy vấn {@code Page<Entity>} sau đó ánh xạ sang {@code List<DTO>}.
     *
     * @param items danh sách phần tử DTO đã được ánh xạ
     * @param page  đối tượng phân trang nguồn chứa thông tin trang
     * @param <T>   kiểu phần tử DTO
     * @return đối tượng {@link PagedResponse} hoàn chỉnh
     */
    public static <T> PagedResponse<T> of(List<T> items, Page<?> page) {
        return new PagedResponse<>(
            items,
            PaginationMetadata.from(page)
        );
    }
}

