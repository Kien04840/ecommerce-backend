package com.ecommerce.notification.repository;

import com.ecommerce.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository quản lý dữ liệu thực thể {@link Notification}.
 * <p>
 * Cung cấp các thao tác truy vấn thông báo người dùng theo phân trang,
 * đếm số lượng thông báo chưa đọc và cập nhật trạng thái đọc hàng loạt.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lấy danh sách tất cả thông báo của người dùng có phân trang, sắp xếp theo thời gian mới nhất.
     *
     * @param userId   định danh người dùng
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách thông báo
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Lấy danh sách thông báo của người dùng theo trạng thái đã đọc/chưa đọc có phân trang.
     *
     * @param userId   định danh người dùng
     * @param isRead   trạng thái đã đọc (true/false)
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách thông báo phù hợp
     */
    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead, Pageable pageable);

    /**
     * Đếm tổng số thông báo chưa đọc của một người dùng.
     * <p>
     * Thường dùng để hiển thị biểu tượng badge số lượng tin nhắn mới trên thanh thông báo.
     *
     * @param userId định danh người dùng
     * @return số lượng thông báo chưa đọc
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Đánh dấu tất cả thông báo chưa đọc của người dùng thành đã đọc.
     * <p>
     * Sử dụng câu lệnh UPDATE JPQL trực tiếp để cập nhật đồng loạt với hiệu năng cao.
     *
     * @param userId định danh người dùng
     * @return số lượng bản ghi được cập nhật
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * Đánh dấu một thông báo cụ thể thành đã đọc, có kiểm tra quyền sở hữu người dùng.
     *
     * @param id     định danh thông báo
     * @param userId định danh người dùng sở hữu
     * @return số lượng bản ghi được cập nhật (1 nếu thành công, 0 nếu không tìm thấy hoặc sai chủ sở hữu)
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user.id = :userId")
    int markAsReadByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Kiểm tra thông báo có thuộc về người dùng chỉ định hay không.
     *
     * @param id     định danh thông báo
     * @param userId định danh người dùng
     * @return {@code true} nếu thông báo tồn tại và thuộc về người dùng
     */
    boolean existsByIdAndUserId(Long id, Long userId);
}

