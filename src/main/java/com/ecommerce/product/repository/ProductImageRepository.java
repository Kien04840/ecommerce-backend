package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu thực thể {@link ProductImage}.
 * <p>
 * Cung cấp các thao tác truy vấn và xóa ảnh sản phẩm theo mã sản phẩm.
 */
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * Tìm toàn bộ danh sách hình ảnh của một sản phẩm.
     *
     * @param productId định danh sản phẩm
     * @return danh sách hình ảnh thuộc sản phẩm
     */
    List<ProductImage> findByProductId(Long productId);

    /**
     * Tìm danh sách hình ảnh của một sản phẩm, sắp xếp ưu tiên ảnh đại diện (isPrimary = true) lên đầu.
     *
     * @param productId định danh sản phẩm
     * @return danh sách hình ảnh có thứ tự ưu tiên
     */
    List<ProductImage> findByProductIdOrderByIsPrimaryDesc(Long productId);

    /**
     * Tìm hình ảnh đại diện (ảnh chính) của sản phẩm.
     *
     * @param productId định danh sản phẩm
     * @return {@link Optional} chứa hình ảnh chính nếu có
     */
    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);

    /**
     * Xóa toàn bộ hình ảnh thuộc về một sản phẩm cụ thể.
     * <p>
     * Thao tác trực tiếp bằng câu lệnh DELETE JPQL để tối ưu tốc độ,
     * tránh việc phải tải từng thực thể ảnh vào persistence context rồi mới xóa.
     *
     * @param productId định danh sản phẩm
     */
    @Modifying
    @Query("DELETE FROM ProductImage pi WHERE pi.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}

