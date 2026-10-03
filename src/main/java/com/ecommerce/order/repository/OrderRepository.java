package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý dữ liệu thực thể {@link Order}.
 * <p>
 * Kế thừa {@link JpaRepository} cung cấp các phương thức thao tác dữ liệu đơn hàng.
 * <p>
 * Tối ưu hóa hiệu năng và phòng tránh N+1 Query:
 * <ul>
 *     <li>Truy vấn phân trang sử dụng {@code JOIN FETCH o.user} trên quan hệ ManyToOne (an toàn cho phân trang)
 *         và định nghĩa rõ {@code countQuery} không có FETCH JOIN.</li>
 *     <li>Truy vấn chi tiết đơn hàng {@link #findByIdWithDetails(Long)} sử dụng {@code JOIN FETCH} đồng thời
 *         cho {@code o.user}, {@code o.orderItems} và {@code oi.product} để nạp toàn bộ cấu trúc đơn trong 1 query duy nhất.</li>
 * </ul>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Tìm danh sách đơn hàng của một người dùng có phân trang.
     *
     * @param userId   định danh người dùng
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách đơn hàng
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * Tìm danh sách đơn hàng theo trạng thái xử lý có phân trang.
     *
     * @param status   trạng thái đơn hàng
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách đơn hàng theo trạng thái
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Tìm danh sách đơn hàng của một người dùng theo trạng thái có phân trang.
     *
     * @param userId   định danh người dùng
     * @param status   trạng thái đơn hàng
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách đơn hàng thỏa mãn
     */
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /**
     * Tìm danh sách đơn hàng của người dùng, nạp sẵn thông tin User và hỗ trợ phân trang.
     * <p>
     * Sử dụng {@code JOIN FETCH o.user} để triệt tiêu lỗi N+1 Query khi hiển thị thông tin người đặt hàng.
     *
     * @param userId   định danh người dùng
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách đơn hàng kèm thông tin User
     */
    @Query(
        value = "SELECT o FROM Order o JOIN FETCH o.user WHERE o.user.id = :userId",
        countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId"
    )
    Page<Order> findByUserIdWithUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * Tìm danh sách đơn hàng của người dùng theo trạng thái, nạp sẵn thông tin User và hỗ trợ phân trang.
     *
     * @param userId   định danh người dùng
     * @param status   trạng thái đơn hàng
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách đơn hàng kèm User
     */
    @Query(
        value = "SELECT o FROM Order o JOIN FETCH o.user WHERE o.user.id = :userId AND o.status = :status",
        countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status = :status"
    )
    Page<Order> findByUserIdAndStatusWithUser(
        @Param("userId") Long userId,
        @Param("status") OrderStatus status,
        Pageable pageable
    );

    /**
     * Lấy danh sách tất cả đơn hàng theo trạng thái (dành cho quản trị viên), nạp sẵn thông tin User.
     * <p>
     * Nếu tham số status là null thì lấy tất cả đơn hàng.
     *
     * @param status   trạng thái đơn hàng cần lọc (có thể null)
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách đơn hàng kèm User
     */
    @Query(
        value = "SELECT o FROM Order o JOIN FETCH o.user WHERE (:status IS NULL OR o.status = :status)",
        countQuery = "SELECT COUNT(o) FROM Order o WHERE (:status IS NULL OR o.status = :status)"
    )
    Page<Order> findAllWithUserByStatus(@Param("status") OrderStatus status, Pageable pageable);

    /**
     * Lấy thông tin chi tiết đầy đủ của đơn hàng gồm User, OrderItems và Product của từng item.
     * <p>
     * Triệt tiêu hoàn toàn bài toán N+1 Query khi mở trang chi tiết hóa đơn/đơn đặt hàng.
     * Sử dụng từ khóa {@code DISTINCT} để ngăn trùng lặp đối tượng cha do phép JOIN với danh sách con.
     *
     * @param orderId định danh đơn hàng
     * @return {@link Optional} chứa đối tượng {@link Order} đầy đủ thông tin chi tiết
     */
    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.user
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.product
        WHERE o.id = :orderId
        """)
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);

    /**
     * Kiểm tra đơn hàng có thuộc sở hữu của người dùng hay không.
     * <p>
     * Thường dùng để xác thực quyền truy cập dữ liệu (Security / Data Ownership Authorization)
     * trước khi cho phép người dùng thao tác hủy đơn hoặc xem chi tiết.
     *
     * @param id     định danh đơn hàng
     * @param userId định danh người dùng
     * @return {@code true} nếu đơn hàng tồn tại và thuộc sở hữu của người dùng
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Đếm tổng số đơn hàng theo trạng thái xử lý.
     *
     * @param status trạng thái đơn hàng
     * @return số lượng đơn hàng có trạng thái tương ứng
     */
    long countByStatus(OrderStatus status);

    /**
     * Đếm tổng số đơn hàng của một người dùng theo trạng thái xử lý.
     *
     * @param userId định danh người dùng
     * @param status trạng thái đơn hàng
     * @return số lượng đơn hàng phù hợp
     */
    long countByUserIdAndStatus(Long userId, OrderStatus status);
}

