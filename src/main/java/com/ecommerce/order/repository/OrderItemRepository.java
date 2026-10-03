package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý dữ liệu thực thể {@link OrderItem}.
 * <p>
 * Kế thừa {@link JpaRepository} cung cấp các thao tác truy vấn các dòng chi tiết đơn hàng.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Tìm danh sách các mặt hàng thuộc về một đơn hàng cụ thể.
     *
     * @param orderId định danh đơn hàng
     * @return danh sách các mục chi tiết đơn hàng
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Tìm danh sách các mặt hàng thuộc về một đơn hàng, nạp sẵn thông tin {@code Product}.
     * <p>
     * Sử dụng {@code JOIN FETCH oi.product} để loại bỏ hoàn toàn lỗi N+1 Query
     * khi duyệt qua danh sách sản phẩm đã mua trong đơn hàng.
     *
     * @param orderId định danh đơn hàng
     * @return danh sách các mục chi tiết đơn hàng kèm thông tin sản phẩm
     */
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") Long orderId);

    /**
     * Kiểm tra xem một sản phẩm đã từng phát sinh trong bất kỳ đơn hàng nào hay chưa.
     * <p>
     * Thường dùng để kiểm tra tính toàn vẹn nghiệp vụ trước khi xóa sản phẩm:
     * không được phép xóa sản phẩm đã có trong lịch sử mua hàng.
     *
     * @param productId định danh sản phẩm
     * @return {@code true} nếu sản phẩm đã tồn tại trong ít nhất một đơn hàng
     */
    boolean existsByProductId(Long productId);

    /**
     * Xóa toàn bộ các dòng mặt hàng thuộc về một đơn hàng.
     *
     * @param orderId định danh đơn hàng
     */
    @Modifying
    @Query("DELETE FROM OrderItem oi WHERE oi.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);
}

