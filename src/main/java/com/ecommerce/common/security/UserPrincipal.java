package com.ecommerce.common.security;

import com.ecommerce.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Lớp đại diện cho danh tính người dùng đã được xác thực trong Spring Security (Authentication Principal).
 * <p>
 * Triển khai interface {@link UserDetails} cung cấp các thông tin định danh:
 * <ul>
 *     <li>{@code id}: Mã định danh người dùng trong cơ sở dữ liệu.</li>
 *     <li>{@code email}: Địa chỉ email liên hệ.</li>
 *     <li>{@code role}: Vai trò phân quyền (ví dụ: {@code ROLE_CUSTOMER}, {@code ROLE_ADMIN}).</li>
 *     <li>{@code enabled}: Trạng thái tài khoản (chặn người dùng bị khóa/vô hiệu hóa).</li>
 * </ul>
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
        Long id,
        String username,
        String email,
        String password,
        boolean enabled,
        Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
    }

    /**
     * Khởi tạo đối tượng {@link UserPrincipal} từ thông tin trích xuất trong JWT Access Token.
     * <p>
     * Dùng cho cơ chế xác thực Stateless trong {@link JwtAuthenticationFilter} mà không cần truy vấn lại database.
     *
     * @param id    mã định danh người dùng từ claim {@code sub}
     * @param email địa chỉ email từ claim {@code email}
     * @param role  tên vai trò từ claim {@code role}
     * @return đối tượng {@link UserPrincipal} hợp lệ
     */
    public static UserPrincipal create(Long id, String email, String role) {
        List<GrantedAuthority> authorities = (role != null && !role.isBlank())
            ? List.of(new SimpleGrantedAuthority(role))
            : Collections.emptyList();

        return new UserPrincipal(
            id,
            email, // Gán email làm username định danh trong security context
            email,
            "",    // Không lưu trữ mật khẩu trong principal từ JWT
            true,  // Token hợp lệ được xem là active cho đến khi refresh hoặc blacklist
            authorities
        );
    }

    /**
     * Khởi tạo đối tượng {@link UserPrincipal} từ thực thể JPA {@link User}.
     * <p>
     * Dùng cho quy trình xác thực mật khẩu qua {@link CustomUserDetailsService}.
     *
     * @param user thực thể người dùng tải từ database
     * @return đối tượng {@link UserPrincipal} kèm đầy đủ mật khẩu băm và trạng thái tài khoản
     */
    public static UserPrincipal fromUser(User user) {
        List<GrantedAuthority> authorities = (user.getRole() != null)
            ? List.of(new SimpleGrantedAuthority(user.getRole().getName()))
            : Collections.emptyList();

        boolean isUserEnabled = Boolean.TRUE.equals(user.getEnabled());

        return new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            isUserEnabled,
            authorities
        );
    }

    /**
     * Lấy tên vai trò phân quyền chính của người dùng.
     *
     * @return chuỗi tên vai trò (ví dụ: "ROLE_CUSTOMER") hoặc null nếu không có
     */
    public String getRole() {
        return authorities.stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse(null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
