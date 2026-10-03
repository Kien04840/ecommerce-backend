package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu thực thể {@link Tag}.
 * <p>
 * Cung cấp các thao tác truy vấn thẻ sản phẩm theo tên, danh sách tên và theo mã sản phẩm liên kết.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Tìm thẻ phân loại theo tên chính xác.
     *
     * @param name tên thẻ cần tìm
     * @return {@link Optional} chứa đối tượng thẻ nếu tìm thấy
     */
    Optional<Tag> findByName(String name);

    /**
     * Kiểm tra sự tồn tại của thẻ theo tên.
     *
     * @param name tên thẻ cần kiểm tra
     * @return {@code true} nếu thẻ đã tồn tại, ngược lại {@code false}
     */
    boolean existsByName(String name);

    /**
     * Tìm danh sách các thẻ có tên nằm trong tập hợp tên cung cấp.
     * <p>
     * Thường dùng khi gán đồng thời nhiều thẻ cho sản phẩm hoặc tạo mới hàng loạt.
     *
     * @param names tập hợp tên thẻ
     * @return danh sách các thẻ tồn tại
     */
    List<Tag> findByNameIn(Collection<String> names);

    /**
     * Tìm kiếm các thẻ có tên chứa từ khóa (không phân biệt chữ hoa thường).
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách thẻ phù hợp
     */
    List<Tag> findByNameContainingIgnoreCase(String keyword);

    /**
     * Lấy danh sách tất cả các thẻ được gắn cho một sản phẩm cụ thể.
     *
     * @param productId định danh sản phẩm
     * @return danh sách các thẻ của sản phẩm
     */
    @Query("SELECT t FROM Tag t JOIN t.products p WHERE p.id = :productId")
    List<Tag> findByProductId(@Param("productId") Long productId);
}

