package com.ecommerce.user.repository;

import com.ecommerce.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý truy xuất dữ liệu cho thực thể {@link User}.
 *
 * <p>Cung cấp các phương thức truy vấn phục vụ quy trình xác thực (Authentication),
 * phân quyền (Authorization) và quản trị tài khoản người dùng trong hệ thống.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tìm kiếm người dùng theo tên đăng nhập (username).
     *
     * @param username tên đăng nhập cần tìm
     * @return Optional chứa thông tin người dùng nếu tồn tại
     */
    Optional<User> findByUsername(String username);

    /**
     * Tìm kiếm người dùng theo địa chỉ email.
     *
     * @param email địa chỉ email cần tìm
     * @return Optional chứa thông tin người dùng nếu tồn tại
     */
    Optional<User> findByEmail(String email);

    /**
     * Kiểm tra sự tồn tại của tên đăng nhập trong hệ thống.
     *
     * @param username tên đăng nhập cần kiểm tra
     * @return true nếu đã tồn tại, ngược lại false
     */
    boolean existsByUsername(String username);

    /**
     * Kiểm tra sự tồn tại của email trong hệ thống.
     *
     * @param email địa chỉ email cần kiểm tra
     * @return true nếu đã tồn tại, ngược lại false
     */
    boolean existsByEmail(String email);

    /**
     * Tìm người dùng theo username và nạp sẵn (fetch) thông tin Role đi kèm.
     *
     * <p>Sử dụng JPQL với {@code JOIN FETCH} để nạp đồng thời thông tin vai trò,
     * ngăn ngừa triệt để lỗi N+1 Query khi Spring Security thực hiện xác thực và phân quyền.</p>
     *
     * @param username tên đăng nhập
     * @return Optional chứa User kèm Role
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    /**
     * Tìm người dùng theo email và nạp sẵn (fetch) thông tin Role đi kèm.
     *
     * <p>Sử dụng {@code @EntityGraph} trên thuộc tính role để tối ưu hiệu năng
     * truy vấn đăng nhập bằng email trong một câu lệnh SQL duy nhất.</p>
     *
     * @param email địa chỉ email
     * @return Optional chứa User kèm Role
     */
    @EntityGraph(attributePaths = {"role"})
    Optional<User> findWithRoleByEmail(String email);

    /**
     * Phân trang danh sách người dùng theo trạng thái kích hoạt tài khoản.
     *
     * @param enabled  trạng thái kích hoạt (true: đang hoạt động, false: bị khóa)
     * @param pageable thông tin phân trang và sắp xếp
     * @return Trang danh sách người dùng tương ứng
     */
    Page<User> findByEnabled(Boolean enabled, Pageable pageable);
}

