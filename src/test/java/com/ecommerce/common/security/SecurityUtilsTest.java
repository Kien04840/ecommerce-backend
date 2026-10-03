package com.ecommerce.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử SecurityUtils")
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Khi đã đăng nhập: lấy đúng Principal, ID, Email và Role")
    void whenAuthenticated_ShouldReturnCorrectIdentity() {
        UserPrincipal principal = UserPrincipal.create(99L, "customer@example.com", "ROLE_CUSTOMER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityUtils.isAuthenticated());
        assertEquals(Optional.of(principal), SecurityUtils.getCurrentUserPrincipal());
        assertEquals(Optional.of(99L), SecurityUtils.getCurrentUserId());
        assertEquals(Optional.of("customer@example.com"), SecurityUtils.getCurrentUserEmail());
        assertEquals(Optional.of("ROLE_CUSTOMER"), SecurityUtils.getCurrentUserRole());
    }

    @Test
    @DisplayName("Khi chưa đăng nhập: isAuthenticated là false và các method trả về Optional.empty")
    void whenNotAuthenticated_ShouldReturnEmpty() {
        SecurityContextHolder.clearContext();

        assertFalse(SecurityUtils.isAuthenticated());
        assertEquals(Optional.empty(), SecurityUtils.getCurrentUserPrincipal());
        assertEquals(Optional.empty(), SecurityUtils.getCurrentUserId());
        assertEquals(Optional.empty(), SecurityUtils.getCurrentUserEmail());
        assertEquals(Optional.empty(), SecurityUtils.getCurrentUserRole());
        assertFalse(SecurityUtils.hasRole("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Khi Authentication là AnonymousAuthenticationToken: xem như chưa xác thực")
    void whenAnonymous_ShouldReturnFalse() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertFalse(SecurityUtils.isAuthenticated());
        assertEquals(Optional.empty(), SecurityUtils.getCurrentUserPrincipal());
        assertEquals(Optional.empty(), SecurityUtils.getCurrentUserId());
    }

    @Test
    @DisplayName("hasRole: hỗ trợ kiểm tra cả có tiền tố ROLE_ lẫn không có tiền tố")
    void hasRole_ShouldSupportWithAndWithoutPrefix() {
        UserPrincipal principal = UserPrincipal.create(1L, "admin@example.com", "ROLE_ADMIN");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityUtils.hasRole("ROLE_ADMIN"));
        assertTrue(SecurityUtils.hasRole("ADMIN"));
        assertFalse(SecurityUtils.hasRole("CUSTOMER"));
        assertFalse(SecurityUtils.hasRole("ROLE_STAFF"));
        assertFalse(SecurityUtils.hasRole(null));
        assertFalse(SecurityUtils.hasRole(""));
    }
}
