package com.ecommerce.common.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Lớp tiện ích cung cấp các phương thức truy xuất thông tin định danh của người dùng hiện tại
 * từ {@link SecurityContextHolder}.
 * <p>
 * Đóng vai trò là lớp trừu tượng chuẩn hóa (Identity Abstraction) theo yêu cầu tại mục 13 (Security Context Identity):
 * <ul>
 *     <li>Ngăn chặn các Controller hoặc Service phải parse JWT thủ công.</li>
 *     <li>Không để Controller đọc raw claims bằng các thao tác xử lý chuỗi.</li>
 *     <li>Tuyệt đối không tin tưởng hay phụ thuộc vào {@code userId} do client gửi lên trong query/body.</li>
 * </ul>
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Lớp tiện ích tĩnh, ngăn chặn khởi tạo đối tượng
    }

    /**
     * Lấy đối tượng {@link Authentication} hiện tại từ SecurityContext.
     *
     * @return {@link Optional} chứa đối tượng Authentication nếu có
     */
    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Kiểm tra xem request hiện tại đã được xác thực danh tính hay chưa (loại trừ Anonymous).
     *
     * @return {@code true} nếu người dùng đã đăng nhập hợp lệ; ngược lại {@code false}
     */
    public static boolean isAuthenticated() {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
            .isPresent();
    }

    /**
     * Lấy đối tượng {@link UserPrincipal} của người dùng hiện tại.
     *
     * @return {@link Optional} chứa {@link UserPrincipal} nếu đã xác thực
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
            .map(Authentication::getPrincipal)
            .filter(principal -> principal instanceof UserPrincipal)
            .map(principal -> (UserPrincipal) principal);
    }

    /**
     * Lấy mã định danh ID (Long) của người dùng hiện tại từ SecurityContext.
     *
     * @return {@link Optional} chứa ID người dùng nếu đã đăng nhập
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getId);
    }

    /**
     * Lấy địa chỉ email của người dùng hiện tại từ SecurityContext.
     *
     * @return {@link Optional} chứa email người dùng nếu đã đăng nhập
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUserPrincipal().map(UserPrincipal::getEmail);
    }

    /**
     * Lấy tên vai trò phân quyền (Role) của người dùng hiện tại từ SecurityContext.
     *
     * @return {@link Optional} chứa tên vai trò (ví dụ: "ROLE_CUSTOMER") nếu có
     */
    public static Optional<String> getCurrentUserRole() {
        return getCurrentUserPrincipal().map(UserPrincipal::getRole);
    }

    /**
     * Kiểm tra xem người dùng hiện tại có sở hữu vai trò được chỉ định hay không.
     *
     * @param roleName tên vai trò cần kiểm tra (ví dụ: "ROLE_ADMIN" hoặc "ADMIN")
     * @return {@code true} nếu người dùng sở hữu vai trò tương ứng; ngược lại {@code false}
     */
    public static boolean hasRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        String normalizedRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
            .map(auth -> auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(normalizedRole)))
            .orElse(false);
    }
}
