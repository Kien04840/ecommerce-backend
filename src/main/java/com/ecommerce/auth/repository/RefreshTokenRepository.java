package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý mã làm mới phiên đăng nhập {@link RefreshToken}.
 *
 * <p>Cung cấp các phương thức truy vấn và cập nhật phục vụ cơ chế xác thực JWT,
 * duy trì phiên và xoay vòng Refresh Token (Refresh Token Rotation - RTR).</p>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Tra cứu bản ghi Refresh Token theo chuỗi giá trị token.
     *
     * @param token chuỗi mã token
     * @return Optional chứa RefreshToken nếu tìm thấy
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Tìm kiếm token hợp lệ và nạp sẵn (fetch) thông tin User liên kết.
     *
     * <p>Sử dụng JPQL với {@code JOIN FETCH r.user} để lấy đồng thời thông tin người dùng,
     * tránh N+1 Query khi hệ thống cần đọc thông tin user để cấp phát Access Token mới.
     * Điều kiện kiểm tra: token chưa bị thu hồi (revoked = false) và chưa quá hạn.</p>
     *
     * @param token chuỗi token cần kiểm tra
     * @param now   thời điểm hiện tại để so sánh hạn sử dụng
     * @return Optional chứa RefreshToken hợp lệ kèm User
     */
    @Query("""
        SELECT r FROM RefreshToken r
        JOIN FETCH r.user
        WHERE r.token = :token
          AND r.revoked = false
          AND r.expiryDate > :now
    """)
    Optional<RefreshToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Lấy danh sách toàn bộ các token đã phát hành cho một người dùng.
     *
     * @param userId mã định danh người dùng
     * @return Danh sách RefreshToken của người dùng
     */
    List<RefreshToken> findByUserId(Long userId);

    /**
     * Thu hồi toàn bộ các Refresh Token còn hoạt động của một người dùng.
     *
     * <p>Được kích hoạt khi người dùng đổi mật khẩu, đăng xuất toàn bộ thiết bị
     * hoặc bị khóa tài khoản để bảo đảm an toàn thông tin.</p>
     *
     * @param userId mã định danh người dùng
     * @return số lượng bản ghi token bị vô hiệu hóa
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    int revokeAllUserTokens(@Param("userId") Long userId);

    /**
     * Dọn dẹp các token đã quá hạn sử dụng khỏi cơ sở dữ liệu.
     *
     * @param now mốc thời gian hiện tại
     * @return số lượng bản ghi đã được xóa
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}

