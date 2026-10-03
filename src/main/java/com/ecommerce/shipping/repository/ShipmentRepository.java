package com.ecommerce.shipping.repository;

import com.ecommerce.shipping.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu thực thể {@link Shipment}.
 * <p>
 * Cung cấp các thao tác truy vấn thông tin giao vận liên kết với đơn hàng,
 * mã vận đơn (tracking number) và trạng thái giao hàng.
 * <p>
 * Phòng tránh N+1 Query bằng cách chủ động nạp thông tin {@code Order} và {@code User}
 * thông qua câu lệnh {@code JOIN FETCH}.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    /**
     * Tìm thông tin vận chuyển theo định danh đơn hàng.
     *
     * @param orderId định danh đơn hàng
     * @return {@link Optional} chứa đối tượng vận chuyển
     */
    Optional<Shipment> findByOrderId(Long orderId);

    /**
     * Tìm thông tin vận chuyển theo mã vận đơn của hãng vận chuyển.
     *
     * @param trackingNumber mã vận đơn
     * @return {@link Optional} chứa đối tượng vận chuyển
     */
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    /**
     * Tìm danh sách các đơn vận chuyển theo trạng thái giao nhận.
     *
     * @param status trạng thái giao nhận (ví dụ: DELIVERED, IN_TRANSIT, PENDING)
     * @return danh sách các đơn vận chuyển tương ứng
     */
    List<Shipment> findByStatus(String status);

    /**
     * Kiểm tra xem đơn hàng đã được tạo bản ghi vận chuyển hay chưa.
     *
     * @param orderId định danh đơn hàng
     * @return {@code true} nếu đơn hàng đã có bản ghi vận chuyển
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Kiểm tra mã vận đơn đã tồn tại trong hệ thống hay chưa.
     *
     * @param trackingNumber mã vận đơn
     * @return {@code true} nếu mã vận đơn đã được sử dụng
     */
    boolean existsByTrackingNumber(String trackingNumber);

    /**
     * Tìm thông tin vận chuyển theo ID kèm đầy đủ thông tin {@code Order} và {@code User}.
     * <p>
     * Sử dụng {@code JOIN FETCH} kép để nạp quan hệ phân cấp {@code Shipment -> Order -> User}
     * trong một truy vấn duy nhất, triệt tiêu hoàn toàn lỗi N+1 Query.
     *
     * @param id định danh bản ghi vận chuyển
     * @return {@link Optional} chứa thông tin vận chuyển kèm đơn hàng và khách hàng
     */
    @Query("SELECT s FROM Shipment s JOIN FETCH s.order o JOIN FETCH o.user WHERE s.id = :id")
    Optional<Shipment> findByIdWithOrderAndUser(@Param("id") Long id);

    /**
     * Tìm thông tin vận chuyển theo Order ID kèm đầy đủ thông tin {@code Order} và {@code User}.
     *
     * @param orderId định danh đơn hàng
     * @return {@link Optional} chứa thông tin vận chuyển kèm đơn hàng và khách hàng
     */
    @Query("SELECT s FROM Shipment s JOIN FETCH s.order o JOIN FETCH o.user WHERE o.id = :orderId")
    Optional<Shipment> findByOrderIdWithOrderAndUser(@Param("orderId") Long orderId);
}

