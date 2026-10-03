package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu danh mục sản phẩm {@link Category}.
 *
 * <p>Cung cấp các phương thức tìm kiếm và kiểm tra danh mục phục vụ
 * phân loại sản phẩm trên sàn thương mại điện tử.</p>
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Tra cứu danh mục theo tên chính xác.
     *
     * @param name tên danh mục
     * @return Optional chứa Category nếu tìm thấy
     */
    Optional<Category> findByName(String name);

    /**
     * Kiểm tra sự tồn tại của danh mục theo tên.
     *
     * @param name tên danh mục cần kiểm tra
     * @return true nếu đã tồn tại, ngược lại false
     */
    boolean existsByName(String name);

    /**
     * Tìm kiếm danh mục theo từ khóa chứa trong tên (không phân biệt chữ hoa thường).
     *
     * @param keyword từ khóa tìm kiếm
     * @return Danh sách các danh mục phù hợp
     */
    List<Category> findByNameContainingIgnoreCase(String keyword);
}

