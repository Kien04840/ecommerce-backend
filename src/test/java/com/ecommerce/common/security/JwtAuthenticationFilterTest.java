package com.ecommerce.common.security;

import com.ecommerce.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Thiếu header Authorization: cho phép request đi tiếp, không thiết lập Authentication")
    void doFilter_MissingAuthorizationHeader_ShouldProceedWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Header Authorization để rỗng: cho phép request đi tiếp, không thiết lập Authentication")
    void doFilter_BlankAuthorizationHeader_ShouldProceedWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Header Authorization không bắt đầu bằng Bearer: không thiết lập Authentication")
    void doFilter_NonBearerHeader_ShouldProceedWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Header Authorization có tiền tố Bearer nhưng chuỗi token rỗng: không thiết lập Authentication")
    void doFilter_BearerEmptyToken_ShouldProceedWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Token JWT hợp lệ: thiết lập Authentication vào SecurityContextHolder thành công")
    void doFilter_ValidToken_ShouldSetAuthenticationInSecurityContext() throws ServletException, IOException {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(42L);
        when(jwtService.extractEmail(token)).thenReturn("user42@example.com");
        when(jwtService.extractRole(token)).thenReturn("ROLE_CUSTOMER");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication phải được lưu vào SecurityContextHolder");
        assertTrue(auth.isAuthenticated());

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(42L, principal.getId());
        assertEquals("user42@example.com", principal.getEmail());
        assertEquals("ROLE_CUSTOMER", principal.getRole());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token JWT không hợp lệ hoặc đã hết hạn: không thiết lập Authentication trong SecurityContextHolder")
    void doFilter_InvalidToken_ShouldNotSetAuthentication() throws ServletException, IOException {
        String token = "invalid.or.expired.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateToken(token)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Ngoại lệ phát sinh khi validate token: xóa sạch SecurityContext và tiếp tục chuỗi lọc an toàn")
    void doFilter_ExceptionDuringValidation_ShouldClearSecurityContext() throws ServletException, IOException {
        String token = "problematic.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateToken(token)).thenThrow(new RuntimeException("Unexpected JWT parsing issue"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
