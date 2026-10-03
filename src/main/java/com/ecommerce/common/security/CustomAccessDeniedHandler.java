package com.ecommerce.common.security;

import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý lỗi không đủ quyền hạn truy cập (HTTP 403 Forbidden) trong Spring Security Filter Chain.
 * <p>
 * Kích hoạt khi:
 * <ul>
 *     <li>Người dùng đã đăng nhập hợp lệ nhưng cố tình truy cập tài nguyên vượt quá thẩm quyền của vai trò (RBAC).</li>
 *     <li>Ví dụ: Khách hàng ({@code ROLE_CUSTOMER}) cố tình gọi các endpoint quản trị ({@code /api/v1/admin/**}).</li>
 * </ul>
 * Đảm bảo phản hồi luôn ở định dạng chuẩn JSON {@link ApiErrorResponse}, phân biệt rạch ròi giữa lỗi 401 (chưa xác thực)
 * và lỗi 403 (đã xác thực nhưng không đủ quyền).
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("Truy cập bị từ chối do không đủ quyền hạn (403 Forbidden) tại URI '{}': {}",
            request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
            ErrorCode.FORBIDDEN.name(),
            ErrorCode.FORBIDDEN.getDefaultMessage()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
