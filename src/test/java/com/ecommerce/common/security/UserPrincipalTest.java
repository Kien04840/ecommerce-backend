package com.ecommerce.common.security;

import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử UserPrincipal")
class UserPrincipalTest {

    @Test
    @DisplayName("Khởi tạo từ claims JWT: gán đúng ID, Email, Role và authorities")
    void create_FromJwtClaims_ShouldPopulateFieldsCorrectly() {
        UserPrincipal principal = UserPrincipal.create(10L, "buyer@example.com", "ROLE_CUSTOMER");

        assertEquals(10L, principal.getId());
        assertEquals("buyer@example.com", principal.getEmail());
        assertEquals("buyer@example.com", principal.getUsername());
        assertEquals("ROLE_CUSTOMER", principal.getRole());
        assertTrue(principal.isEnabled());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isCredentialsNonExpired());

        assertEquals(1, principal.getAuthorities().size());
        GrantedAuthority authority = principal.getAuthorities().iterator().next();
        assertEquals("ROLE_CUSTOMER", authority.getAuthority());
    }

    @Test
    @DisplayName("Khởi tạo từ thực thể User có vai trò và trạng thái enabled = true")
    void fromUser_ActiveUser_ShouldPopulateFieldsAndAuthorities() {
        Role role = Role.builder().name("ROLE_ADMIN").build();
        User user = User.builder()
            .username("admin123")
            .email("admin@example.com")
            .password("EncodedHashSecret")
            .enabled(true)
            .role(role)
            .build();

        UserPrincipal principal = UserPrincipal.fromUser(user);

        assertEquals("admin123", principal.getUsername());
        assertEquals("admin@example.com", principal.getEmail());
        assertEquals("EncodedHashSecret", principal.getPassword());
        assertEquals("ROLE_ADMIN", principal.getRole());
        assertTrue(principal.isEnabled());
        assertTrue(principal.isAccountNonLocked());
    }

    @Test
    @DisplayName("Khởi tạo từ thực thể User bị vô hiệu hóa: isEnabled và isAccountNonLocked phải trả về false")
    void fromUser_DisabledUser_IsEnabledShouldBeFalse() {
        Role role = Role.builder().name("ROLE_CUSTOMER").build();
        User user = User.builder()
            .username("banned_user")
            .email("banned@example.com")
            .password("EncodedHashSecret")
            .enabled(false)
            .role(role)
            .build();

        UserPrincipal principal = UserPrincipal.fromUser(user);

        assertFalse(principal.isEnabled(), "Tài khoản bị vô hiệu hóa thì isEnabled phải false");
        assertTrue(principal.isAccountNonLocked());
    }

    @Test
    @DisplayName("Khởi tạo từ User không có vai trò: authorities phải rỗng và getRole trả về null")
    void fromUser_UserWithoutRole_AuthoritiesShouldBeEmpty() {
        User user = User.builder()
            .username("norole_user")
            .email("norole@example.com")
            .password("EncodedHashSecret")
            .enabled(true)
            .role(null)
            .build();

        UserPrincipal principal = UserPrincipal.fromUser(user);

        assertTrue(principal.getAuthorities().isEmpty());
        assertNull(principal.getRole());
    }

    @Test
    @DisplayName("So sánh bằng nhau (equals và hashCode) dựa trên ID người dùng")
    void equalsAndHashCode_ShouldBeBasedOnId() {
        UserPrincipal p1 = UserPrincipal.create(10L, "u1@example.com", "ROLE_CUSTOMER");
        UserPrincipal p2 = UserPrincipal.create(10L, "u2@example.com", "ROLE_ADMIN");
        UserPrincipal p3 = UserPrincipal.create(20L, "u3@example.com", "ROLE_CUSTOMER");

        assertEquals(p1, p2, "Cùng ID thì phải equals");
        assertEquals(p1.hashCode(), p2.hashCode(), "Cùng ID thì hashCode phải bằng nhau");
        assertNotEquals(p1, p3, "Khác ID thì không được equals");
    }
}
