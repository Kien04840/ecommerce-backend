package com.ecommerce.user.repository;

import com.ecommerce.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý dữ liệu vai trò phân quyền {@link Role}.
 *
 * <p>Cung cấp các thao tác tra cứu vai trò phục vụ gán quyền hạn khi đăng ký
 * hoặc phân quyền quản trị trong hệ thống.</p>
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Tra cứu vai trò theo tên định danh quyền (ví dụ: ROLE_CUSTOMER, ROLE_ADMIN).
     *
     * @param name tên vai trò phân quyền
     * @return Optional chứa Role nếu tìm thấy
     */
    Optional<Role> findByName(String name);

    /**
     * Kiểm tra vai trò đã tồn tại trong cơ sở dữ liệu hay chưa.
     *
     * @param name tên vai trò cần kiểm tra
     * @return true nếu đã tồn tại, ngược lại false
     */
    boolean existsByName(String name);
}

