package com.ecommerce.common.security;

import com.ecommerce.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bộ lọc xác thực JWT (JSON Web Token Authentication Filter) trong chuỗi lọc của Spring Security.
 * <p>
 * Thực thi chính xác một lần duy nhất trên mỗi HTTP request (kế thừa {@link OncePerRequestFilter}):
 * <ol>
 *     <li>Trích xuất header {@code Authorization} và kiểm tra tiền tố {@code Bearer }.</li>
 *     <li>Kiểm tra tính hợp lệ về mặt toán học và thời hạn của JWT Access Token qua {@link JwtService}.</li>
 *     <li>Trích xuất các claims chuẩn ({@code sub}, {@code email}, {@code role}).</li>
 *     <li>Khởi tạo đối tượng {@link UserPrincipal} và thiết lập danh tính vào {@link SecurityContextHolder}.</li>
 * </ol>
 * Cơ chế hoạt động hoàn toàn phi trạng thái (Stateless), không lưu token vào session máy chủ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 1. Kiểm tra header Authorization có tồn tại và đúng định dạng 'Bearer <token>'
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith(BEARER_PREFIX)) {
            // Cho phép request tiếp tục đi qua chuỗi lọc; nếu endpoint yêu cầu xác thực, EntryPoint sẽ chặn sau đó
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Tách chuỗi JWT token
        String jwtToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (jwtToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Xác thực chữ ký số và thời hạn hiệu lực của JWT Access Token
        try {
            if (jwtService.validateToken(jwtToken)) {
                Long userId = jwtService.extractUserId(jwtToken);
                String email = jwtService.extractEmail(jwtToken);
                String role = jwtService.extractRole(jwtToken);

                // Khởi tạo Principal đại diện cho người dùng đã xác thực
                UserPrincipal userPrincipal = UserPrincipal.create(userId, email, role);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userPrincipal,
                    null,
                    userPrincipal.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Lưu trữ danh tính vào SecurityContextHolder cho toàn bộ vòng đời của thread xử lý request
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Xác thực JWT thành công cho User ID: {}, Role: {}, URI: {}",
                    userId, role, request.getRequestURI());
            } else {
                log.warn("Mã xác thực JWT không hợp lệ hoặc đã hết hạn tại URI: {}", request.getRequestURI());
            }
        } catch (Exception ex) {
            log.warn("Lỗi khi xử lý giải mã JWT tại URI '{}': {}", request.getRequestURI(), ex.getMessage());
            // Đảm bảo không để lộ ngoại lệ kỹ thuật, xóa sạch SecurityContext nếu xảy ra lỗi
            SecurityContextHolder.clearContext();
        }

        // 4. Chuyển tiếp request cho các Filter tiếp theo trong chuỗi
        filterChain.doFilter(request, response);
    }
}
