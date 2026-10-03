package com.ecommerce.common.security;

import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý lỗi xác thực không thành công (HTTP 401 Unauthorized) trong Spring Security Filter Chain.
 * <p>
 * Kích hoạt khi:
 * <ul>
 *     <li>Yêu cầu truy cập endpoint được bảo vệ nhưng thiếu Bearer Token trong header {@code Authorization}.</li>
 *     <li>Mã xác thực JWT không hợp lệ, sai chữ ký số (invalid signature), sai định dạng (malformed) hoặc đã hết hạn (expired).</li>
 * </ul>
 * Đảm bảo phản hồi luôn ở định dạng chuẩn JSON {@link ApiErrorResponse}, tuyệt đối không trả về
 * Whitelabel Error Page hoặc mã HTML mặc định của Spring Security.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        log.warn("Truy cập bị từ chối do chưa xác thực (401 Unauthorized) tại URI '{}': {}",
            request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
            ErrorCode.UNAUTHORIZED.name(),
            ErrorCode.UNAUTHORIZED.getDefaultMessage()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
