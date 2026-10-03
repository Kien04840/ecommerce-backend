package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu thực thể {@link Product}.
 * <p>
 * Kế thừa {@link JpaRepository} cung cấp các thao tác CRUD cơ bản
 * và {@link JpaSpecificationExecutor} hỗ trợ xây dựng truy vấn động đa tiêu chí.
 * <p>
 * Được tối ưu hóa bằng các kỹ thuật:
 * <ul>
 *     <li>Sử dụng {@code JOIN FETCH} kết hợp {@code countQuery} riêng biệt cho truy vấn phân trang chống lỗi N+1 Query.</li>
 *     <li>Sử dụng {@link EntityGraph} nạp đồng thời Category, Images và Tags khi lấy thông tin chi tiết một sản phẩm.</li>
 * </ul>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Tìm danh sách sản phẩm theo danh mục có phân trang.
     *
     * @param categoryId định danh danh mục sản phẩm
     * @param pageable   thông tin phân trang và sắp xếp
     * @return trang danh sách sản phẩm thuộc danh mục
     */
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * Tìm danh sách sản phẩm theo trạng thái kinh doanh có phân trang.
     *
     * @param status   trạng thái sản phẩm (ACTIVE, INACTIVE, OUT_OF_STOCK)
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách sản phẩm theo trạng thái
     */
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    /**
     * Tìm kiếm sản phẩm theo tên (không phân biệt chữ hoa thường) có phân trang.
     *
     * @param name     từ khóa tên sản phẩm
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách sản phẩm khớp tên
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Kiểm tra sự tồn tại của sản phẩm theo tên.
     *
     * @param name tên sản phẩm cần kiểm tra
     * @return {@code true} nếu tên sản phẩm đã tồn tại, ngược lại {@code false}
     */
    boolean existsByName(String name);

    /**
     * Kiểm tra sự tồn tại của sản phẩm theo ID và trạng thái kinh doanh.
     *
     * @param id     định danh sản phẩm
     * @param status trạng thái sản phẩm
     * @return {@code true} nếu sản phẩm tồn tại với đúng trạng thái
     */
    boolean existsByIdAndStatus(Long id, ProductStatus status);

    /**
     * Lấy thông tin chi tiết đầy đủ của sản phẩm bao gồm Category, danh sách Images và danh sách Tags.
     * <p>
     * Sử dụng {@link EntityGraph} để nạp eager các quan hệ LAZY trong một truy vấn duy nhất,
     * loại bỏ hoàn toàn vấn đề N+1 Query khi hiển thị trang chi tiết sản phẩm.
     *
     * @param id định danh sản phẩm
     * @return {@link Optional} chứa đối tượng {@link Product} với đầy đủ thông tin liên kết
     */
    @EntityGraph(attributePaths = {"category", "images", "tags"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    /**
     * Tìm kiếm và lọc sản phẩm đa tiêu chí kết hợp phân trang, nạp sẵn thông tin Category.
     * <p>
     * Sử dụng {@code JOIN FETCH p.category} trên quan hệ ManyToOne (an toàn tuyệt đối cho phân trang)
     * nhằm tránh N+1 Query khi client duyệt danh sách sản phẩm.
     * Khai báo {@code countQuery} riêng biệt không chứa FETCH JOIN theo đúng chuẩn Spring Data JPA.
     *
     * @param keyword    từ khóa tìm kiếm trong tên hoặc mô tả sản phẩm (có thể null)
     * @param categoryId định danh danh mục cần lọc (có thể null)
     * @param status     trạng thái sản phẩm cần lọc (có thể null)
     * @param minPrice   mức giá tối thiểu (có thể null)
     * @param maxPrice   mức giá tối đa (có thể null)
     * @param pageable   thông tin phân trang và sắp xếp
     * @return trang dữ liệu sản phẩm thỏa mãn tất cả tiêu chí lọc
     */
    @Query(
        value = """
            SELECT p FROM Product p
            JOIN FETCH p.category c
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """,
        countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """
    )
    Page<Product> searchAndFilterProducts(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("status") ProductStatus status,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    /**
     * Truy vấn số lượng tồn kho của một sản phẩm cụ thể.
     * <p>
     * Chỉ chọn cột stockQuantity để tối ưu băng thông và hiệu năng truy vấn,
     * không cần nạp toàn bộ thực thể Product khi chỉ muốn kiểm tra số lượng tồn kho.
     *
     * @param id định danh sản phẩm
     * @return {@link Optional} chứa số lượng tồn kho
     */
    @Query("SELECT p.stockQuantity FROM Product p WHERE p.id = :id")
    Optional<Integer> findStockQuantityById(@Param("id") Long id);
}

